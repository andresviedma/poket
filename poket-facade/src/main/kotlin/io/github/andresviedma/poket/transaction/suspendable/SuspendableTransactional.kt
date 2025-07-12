package io.github.andresviedma.poket.transaction.suspendable

import io.github.andresviedma.poket.transaction.TransactionIsolationLevel
import io.github.andresviedma.poket.transaction.TransactionWrapper

suspend fun <T> transactional(
    isolationLevel: TransactionIsolationLevel? = null, // use default
    rollbackOn: Set<Class<out Throwable>> = emptySet(),
    dontRollbackOn: Set<Class<out Throwable>> = emptySet(),
    block: suspend () -> T
): T = TransactionWrapper.suspendableTransactional(isolationLevel, rollbackOn, dontRollbackOn, block)
