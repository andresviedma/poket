package com.github.andresviedma.poket.transaction

import com.github.andresviedma.poket.transaction.TransactionWrapper.currentTransactionData

suspend fun <T> runAfterTransactionCommit(action: suspend () -> T) {
    currentTransactionData<TransactionHookRegister>()
        ?.addOnTransactionCommitHook { action() }
        ?: action()
}

class TransactionHookRegister {
    private val onCommitActions: MutableList<suspend () -> Unit> = mutableListOf()

    fun addOnTransactionCommitHook(action: suspend () -> Unit) {
        if (TransactionWrapper.inTransaction()) {
            onCommitActions.add(action)
        }
    }

    fun clearTransactionHooks(commit: Boolean): List<suspend () -> Unit> =
        (if (commit) onCommitActions.toList() else emptyList())
            .also { onCommitActions.clear() }
}
