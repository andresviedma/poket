package io.github.andresviedma.poket.mutex.watcher

import io.github.andresviedma.poket.mutex.LockContext
import io.github.andresviedma.poket.mutex.LockSystem
import java.time.Duration

open class LockSystemWithTtlWatcher(
    protected val targetLockSystem: LockSystem
) : LockSystem {
    private val ttlWatcher = LockExpirationWatcher(
        { lock, ctx -> releaseExpiredLock(lock, ctx) },
        { ctx -> releaseExpiredLockConnection(ctx) }
    )

    override fun getId(): String = targetLockSystem.getId()

    override suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext =
        targetLockSystem.waitLock(name, timeout, ttl)
            .also { if (it.hasLock) ttlWatcher.scheduleTtlExpiration(name, ttl, it) }

    override suspend fun getLockIfFree(name: String, ttl: Duration): LockContext =
        targetLockSystem.getLockIfFree(name, ttl)
            .also { if (it.hasLock) ttlWatcher.scheduleTtlExpiration(name, ttl, it) }

    override suspend fun releaseLock(name: String, lockContext: LockContext): Boolean {
        runCatching { ttlWatcher.killTtlExpirationJob(name) }
        return targetLockSystem.releaseLock(name, lockContext)
    }

    protected open suspend fun releaseExpiredLock(name: String, lockContext: LockContext) {
        targetLockSystem.releaseLock(name, lockContext)
    }

    protected open suspend fun releaseExpiredLockConnection(lockContext: LockContext) {
    }
}
