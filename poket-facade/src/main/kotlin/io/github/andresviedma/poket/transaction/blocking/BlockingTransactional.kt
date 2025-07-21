package io.github.andresviedma.poket.transaction.blocking

import io.github.andresviedma.poket.transaction.TransactionIsolationLevel
import io.github.andresviedma.poket.transaction.TransactionWrapper
import kotlin.reflect.KClass

fun <T> transactional(
    isolationLevel: TransactionIsolationLevel? = null, // use default
    rollbackOn: Set<KClass<out Throwable>> = emptySet(),
    dontRollbackOn: Set<KClass<out Throwable>> = emptySet(),
    block: () -> T
): T = TransactionWrapper.blockingTransactional(isolationLevel, rollbackOn, dontRollbackOn, block)
