package io.github.andresviedma.poket.transaction

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * Object with functions for fast transaction handling access. The main function is "transactional", wrapping the
 * code inside in a transaction.
 */
object TransactionWrapper {
    @Suppress("MemberVisibilityCanBePrivate")
    internal var injectedTransactionManager: TransactionManager = TransactionManager.withHandlers()

    var overriddenTransactionManager: TransactionManager? = null

    @Suppress("MemberVisibilityCanBePrivate")
    val transactionManager get() = overriddenTransactionManager ?: injectedTransactionManager

    @Suppress("unused")
    fun init(transactionManager: TransactionManager): TransactionWrapper = apply {
        injectedTransactionManager = transactionManager
    }

    /**
     * Wraps the given block of code in a transaction.
     * If it is nested in a different transaction it will join the existing transaction.
     */
    suspend fun <T> suspendableTransactional(
        isolationLevel: TransactionIsolationLevel? = null, // use default
        rollbackOn: Set<KClass<out Throwable>> = emptySet(),
        dontRollbackOn: Set<KClass<out Throwable>> = emptySet(),
        block: suspend () -> T
    ): T =
        transactionManager.transactional(
            TransactionMetadata(isolationLevel = isolationLevel, rollbackOn = rollbackOn, dontRollbackOn = dontRollbackOn),
            block
        )

    /**
     * Wraps the given block of code in a transaction, for non suspendable code.
     */
    fun <T> blockingTransactional(
        isolationLevel: TransactionIsolationLevel? = null, // use default
        rollbackOn: Set<KClass<out Throwable>> = emptySet(),
        dontRollbackOn: Set<KClass<out Throwable>> = emptySet(),
        block: () -> T
    ): T =
        transactionManager.blockingTransactional(
            TransactionMetadata(isolationLevel = isolationLevel, rollbackOn = rollbackOn, dontRollbackOn = dontRollbackOn),
            block
        )

    /**
     * True if current code is inside a transaction scope.
     */
    fun inTransaction(): Boolean =
        transactionManager.inTransaction()

    /**
     * Coroutine context to preserve current coroutine transaction context.
     */
    fun transactionCoroutineContext(): CoroutineContext.Element =
        transactionManager.asContextElement()

    /**
     * Coroutine context to create a new current coroutine transaction context.
     */
    fun newTransactionCoroutineContext(): CoroutineContext.Element =
        transactionManager.asNewContextElement()

    /**
     * Returns current transaction data for a given type.
     */
    inline fun <reified T> currentTransactionData(): T? =
        currentTransactionData(T::class) as T?

    fun currentTransactionData(dataClass: KClass<*>): Any? =
        transactionManager.currentContext()?.getTransactionData(dataClass)
}
