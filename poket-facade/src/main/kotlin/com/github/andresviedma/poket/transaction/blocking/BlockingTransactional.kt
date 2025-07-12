package com.github.andresviedma.poket.transaction.blocking

import com.github.andresviedma.poket.transaction.TransactionIsolationLevel
import com.github.andresviedma.poket.transaction.TransactionWrapper

fun <T> transactional(
    isolationLevel: TransactionIsolationLevel? = null, // use default
    rollbackOn: Set<Class<out Throwable>> = emptySet(),
    dontRollbackOn: Set<Class<out Throwable>> = emptySet(),
    block: () -> T
): T = TransactionWrapper.blockingTransactional(isolationLevel, rollbackOn, dontRollbackOn, block)
