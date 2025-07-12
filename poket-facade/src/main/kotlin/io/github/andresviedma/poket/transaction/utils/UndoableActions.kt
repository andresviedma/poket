package io.github.andresviedma.poket.transaction.utils

import io.github.andresviedma.poket.transaction.TransactionDataHandler
import io.github.andresviedma.poket.transaction.TransactionMetadata
import io.github.andresviedma.poket.transaction.TransactionWrapper.currentTransactionData
import io.github.andresviedma.poket.transaction.TransactionWrapper.inTransaction
import io.github.oshai.kotlinlogging.KotlinLogging


/**
 * Transaction-aware execution of actions that can be undone by running a different (opposite) function.
 * If this is run inside a transaction and it is rolled back, the undo action will be run, but only if the action
 * was completed successfully with no exception.
 * If not in a transaction, the action will just be executed normally.
 *
 * Usage:
 *      undoable(undo = { (undo action) }) {
 *          (action)
 *      }
 */
suspend fun <T> undoable(undo: suspend (T) -> Unit, action: suspend () -> T): T =
    action()
        .also { addUndo(it, undo) }

/**
 * Same as undoable, but in this case the undo function will be executed also if the action throws an exception
 * and is not finished. In that case, the undo function will receive null as parameter.
 *
 * Usage:
 *      undoableEvenIfNotFinished(undo = { (undo action) }) {
 *          (action)
 *      }
 */
suspend fun <T> undoableEvenIfNotFinished(undo: suspend (T?) -> Unit, action: suspend () -> T): T =
    try {
        action()
            .also { addUndo(it, undo) }
    } catch (exception: Throwable) {
        addUndo(null, undo)
        throw exception
    }

private fun <T> addUndo(it: T, undo: suspend (T) -> Unit) {
    currentTransactionData<UndoableActionsRegister>()?.addUndoAction(it, undo)
}

private class UndoableActionsRegister {
    private val transactionUndoes: MutableList<suspend () -> Unit> = mutableListOf()

    fun <T> addUndoAction(data: T, undo: suspend (T) -> Unit) {
        if (inTransaction()) {
            transactionUndoes.add { undo(data) }
        }
    }

    fun clearUndoActions(): List<suspend () -> Unit> =
        transactionUndoes.toList()
            .also { transactionUndoes.clear() }
}

private val logger = KotlinLogging.logger {}

class UndoableActionsTransactionHandler : TransactionDataHandler {
    override suspend fun commitTransaction(transactionData: Any?) {
        if (transactionData is UndoableActionsRegister) {
            transactionData.clearUndoActions()
        }
    }

    override suspend fun rollbackTransaction(transactionData: Any?) {
        if (transactionData is UndoableActionsRegister) {
            transactionData.clearUndoActions().reversed().forEach { undo ->
                try {
                    undo()
                } catch (exception: Throwable) {
                    logger.error(exception) { "Error undoing an action on transaction rollback: ${exception.message}" }
                }
            }
        }
    }

    override suspend fun startTransaction(metadata: TransactionMetadata): Any =
        UndoableActionsRegister()
}
