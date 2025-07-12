package io.github.andresviedma.poket.transaction.utils

import io.github.andresviedma.poket.transaction.TransactionDataHandler
import io.github.andresviedma.poket.transaction.TransactionMetadata
import io.github.andresviedma.poket.transaction.TransactionWrapper.currentTransactionData
import io.github.andresviedma.poket.transaction.TransactionWrapper.inTransaction
import io.github.oshai.kotlinlogging.KotlinLogging


/**
 * Implementation of the saga pattern in the context of a generic transaction.
 *
 * Transaction-aware execution of an operation in a saga, so that it can be undone by running a different (opposite)
 * compensation operation.
 * If this is run inside a transaction and it is rolled back, the undo operation will be run, but only if the action
 * was completed successfully with no exception.
 * If not in a transaction, the original operation will just be executed normally.
 *
 * Usage:
 *      sagaOperation(undo = { (undo action) }) {
 *          (action)
 *      }
 */
suspend fun <T> sagaOperation(undo: suspend (T) -> Unit, action: suspend () -> T): T =
    action()
        .also { addSagaUndo(it, undo) }

/**
 * Same as sagaOperation, but in this case the undo function will be executed also if the operation throws an exception
 * and is not finished. In that case, the undo function will receive null as parameter.
 *
 * Usage:
 *      sagaOperationIfNotFinished(undo = { (undo action) }) {
 *          (action)
 *      }
 */
@Suppress("unused")
suspend fun <T> sagaOperationIfNotFinished(undo: suspend (T?) -> Unit, action: suspend () -> T): T =
    try {
        action()
            .also { addSagaUndo(it, undo) }
    } catch (exception: Throwable) {
        addSagaUndo(null, undo)
        throw exception
    }

private fun <T> addSagaUndo(it: T, undo: suspend (T) -> Unit) {
    currentTransactionData<SagaRegister>()?.addSagaUndo(it, undo)
}

private class SagaRegister {
    private val transactionUndoes: MutableList<suspend () -> Unit> = mutableListOf()

    fun <T> addSagaUndo(data: T, undo: suspend (T) -> Unit) {
        if (inTransaction()) {
            transactionUndoes.add { undo(data) }
        }
    }

    fun clearSagas(): List<suspend () -> Unit> =
        transactionUndoes.toList()
            .also { transactionUndoes.clear() }
}

private val logger = KotlinLogging.logger {}

class SagaTransactionHandler : TransactionDataHandler {
    override suspend fun commitTransaction(transactionData: Any?) {
        if (transactionData is SagaRegister) {
            transactionData.clearSagas()
        }
    }

    override suspend fun rollbackTransaction(transactionData: Any?) {
        if (transactionData is SagaRegister) {
            transactionData.clearSagas().reversed().forEach { undo ->
                try {
                    undo()
                } catch (exception: Throwable) {
                    logger.error(exception) { "Error undoing an action on transaction rollback: ${exception.message}" }
                }
            }
        }
    }

    override suspend fun startTransaction(metadata: TransactionMetadata): Any =
        SagaRegister()
}
