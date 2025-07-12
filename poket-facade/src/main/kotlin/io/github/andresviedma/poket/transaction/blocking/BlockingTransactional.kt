package io.github.andresviedma.poket.transaction.blocking

import io.github.andresviedma.poket.transaction.TransactionIsolationLevel
import io.github.andresviedma.poket.transaction.TransactionWrapper

fun <T> transactional(
    isolationLevel: TransactionIsolationLevel? = null, // use default
    rollbackOn: Set<Class<out Throwable>> = emptySet(),
    dontRollbackOn: Set<Class<out Throwable>> = emptySet(),
    block: () -> T
): T = TransactionWrapper.blockingTransactional(isolationLevel, rollbackOn, dontRollbackOn, block)
