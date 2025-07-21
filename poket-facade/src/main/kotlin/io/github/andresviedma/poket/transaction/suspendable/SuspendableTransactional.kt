package io.github.andresviedma.poket.transaction.suspendable

import io.github.andresviedma.poket.transaction.TransactionIsolationLevel
import io.github.andresviedma.poket.transaction.TransactionWrapper
import kotlin.reflect.KClass

suspend fun <T> transactional(
    isolationLevel: TransactionIsolationLevel? = null, // use default
    rollbackOn: Set<KClass<out Throwable>> = emptySet(),
    dontRollbackOn: Set<KClass<out Throwable>> = emptySet(),
    block: suspend () -> T
): T = TransactionWrapper.suspendableTransactional(isolationLevel, rollbackOn, dontRollbackOn, block)
