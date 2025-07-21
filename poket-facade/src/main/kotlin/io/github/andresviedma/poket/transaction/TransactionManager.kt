package io.github.andresviedma.poket.transaction

import io.github.andresviedma.poket.transaction.utils.TransactionHookRegister
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Deque
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

/**
 * Generic transaction coordinator.
 *
 * In case of error in any transaction step of handler remaining handlers will do rollback, even
 * when they can have been already committed.
 */
class TransactionManager(
    transactionHandlersSet: Set<TransactionDataHandler>,
) {
    private val transactionContext: ThreadLocal<TransactionContext?> = ThreadLocal.withInitial { null }

    private val transactionHandlers: List<TransactionDataHandler> =
        transactionHandlersSet.sortedBy { if (it.isPrimaryStorage()) 0 else 1 }

    fun inTransaction(): Boolean =
        currentContext()?.inTransaction == true

    suspend fun <T> transactional(metadata: TransactionMetadata = TransactionMetadata(), block: suspend () -> T): T =
        withContext(asContextElement()) {
            transactionalBlock(metadata, block)
        }

    @Suppress("unused")
    suspend fun <T> transactionalNew(metadata: TransactionMetadata = TransactionMetadata(), block: suspend () -> T): T =
        withContext(asNewContextElement()) {
            transactionalBlock(metadata, block)
        }

    private suspend fun <T> transactionalBlock(
        metadata: TransactionMetadata = TransactionMetadata(),
        block: suspend () -> T
    ): T {
        val transactionHandlersData = mutableListOf<Any?>()
        val startedHere = startOrJoinTransaction(metadata, transactionHandlersData)

        var commit = false
        try {
            return block().also {
                if (startedHere) {
                    commit(transactionHandlersData, null)
                    commit = true
                }
            }
        } catch (e: Throwable) {
            if (startedHere) {
                if (metadata.isRollbackException(e)) {
                    forceRollback(transactionHandlersData, e)
                } else {
                    commit(transactionHandlersData, e)
                    commit = true
                }
            }
            throw e
        } finally {
            if (startedHere) {
                val ctx = currentContextForced()
                try {
                    runActionsAfterTransaction(ctx, commit)
                } finally {
                    ctx.clear()
                    transactionContext.set(null)
                }
            }
        }
    }

    fun <T> blockingTransactional(metadata: TransactionMetadata = TransactionMetadata(), block: () -> T): T =
        runBlocking {
            if (!inTransaction()) transactionContext.set(TransactionContext())
            transactionalBlock(metadata) { block() }
        }

    fun asContextElement(): CoroutineContext.Element {
        return if (inTransaction()) {
            transactionContext.asContextElement()
        } else {
            asNewContextElement()
        }
    }

    fun asNewContextElement(): CoroutineContext.Element {
        return transactionContext.asContextElement(TransactionContext())
    }

    private fun currentContextForced(): TransactionContext =
        transactionContext.get()!!

    fun currentContext(): TransactionContext? =
        transactionContext.get()

    private suspend fun startOrJoinTransaction(
        metadata: TransactionMetadata,
        transactionHandlersData: MutableList<Any?>
    ): Boolean =
        if (!inTransaction()) {
            val ctx = currentContextForced()
            ctx.inTransaction = true

            val afterTransactionActions = TransactionHookRegister()
            ctx.registerTransactionData(afterTransactionActions)

            var currentHandler: TransactionDataHandler? = null
            try {
                transactionHandlers.forEach { handler ->
                    currentHandler = handler
                    val transactionData = handler.startTransaction(metadata)
                    transactionHandlersData.add(transactionData)
                    ctx.registerTransactionData(transactionData)
                }
            } catch (exc: Throwable) {
                forceRollbackAfterHandler(transactionHandlersData, exc, currentHandler)
                throw exc
            }
            true
        } else {
            false
        }

    private suspend fun forceRollback(transactionHandlersData: MutableList<Any?>, originalException: Throwable) {
        nonCancellable {
            forceRollbackAfterHandler(transactionHandlersData, originalException, null)
        }
    }

    /** For commit / rollback, make sure they are run even if current job has been cancelled */
    private suspend fun nonCancellable(block: suspend () -> Unit) {
        withContext(NonCancellable) { block() }
    }

    private suspend fun forceRollbackAfterHandler(
        transactionHandlersData: MutableList<Any?>,
        originalException: Throwable,
        lastIgnoredHandler: TransactionDataHandler?
    ) {
        var workingLastIgnoredHandler = lastIgnoredHandler
        transactionHandlers.reversed().forEach { handler ->
            if (workingLastIgnoredHandler === handler) {
                workingLastIgnoredHandler = null
            } else if (workingLastIgnoredHandler == null) {
                try {
                    val transactionData = transactionHandlersData.removeLast()
                    handler.rollbackTransaction(transactionData)
                } catch (exception: Throwable) {
                    originalException.addSuppressed(exception)
                }
            }
        }
        currentContext()?.clear()
    }

    private suspend fun commit(transactionHandlersData: MutableList<Any?>, originalException: Throwable?) {
        nonCancellable {
            val transactionHandlersDataCopy: Deque<Any?> = LinkedList(transactionHandlersData)
            transactionHandlers.reversed().forEach { handler ->
                val transactionData = transactionHandlersDataCopy.removeLast()
                handler.commitTransaction(transactionData)
            }
            if (originalException != null) {
                logger.warn(originalException) {
                    "Transaction commit run although an exception occurred: " +
                            "${originalException.javaClass.simpleName} ${originalException.message}"
                }
            }
        }
    }

    private suspend fun runActionsAfterTransaction(context: TransactionContext, commit: Boolean) {
        (context.getTransactionData(TransactionHookRegister::class) as? TransactionHookRegister)
            ?.clearTransactionHooks(commit = commit)
            ?.forEach { action -> action() }
    }

    companion object {
        fun withHandlers(vararg handlers: TransactionDataHandler) =
            TransactionManager(handlers.toSet())
    }
}

data class TransactionContext(
    var inTransaction: Boolean = false
) {
    private val transactionData = ConcurrentHashMap<KClass<*>, Any>()

    fun registerTransactionData(dataObject: Any?) {
        dataObject?.let { transactionData[it::class] = dataObject }
    }

    fun getTransactionData(dataClass: KClass<*>): Any? =
        transactionData[dataClass]

    fun clear() {
        inTransaction = false
        transactionData.clear()
    }
}
