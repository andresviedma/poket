package io.github.andresviedma.poket.mutex.local

import io.github.andresviedma.poket.mutex.LockContext
import io.github.andresviedma.poket.mutex.LockSystem
import kotlin.time.Duration

/**
 * Implementation of a lock system that is disabled and does not really get a lock.
 */
@Suppress("unused")
class DisabledLockSystem : LockSystem {
    override fun getId(): String = "disabled"

    override suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext =
        LockContext(true)

    override suspend fun getLockIfFree(name: String, ttl: Duration): LockContext =
        LockContext(true)

    override suspend fun releaseLock(name: String, lockContext: LockContext): Boolean =
        true
}
