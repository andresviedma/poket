package io.github.andresviedma.poket.mutex.local

import io.github.andresviedma.poket.mutex.LockContext
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.mutex.watcher.LockSystemWithTtlWatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Implementation of a lock system using local mutex. It is mainly useful for tests, it should not be used for
 * distributed locking in services.
 */
class LocalLockSystem : LockSystemWithTtlWatcher(
    targetLockSystem = SimpleLocalLockSystem()
) {
    private val targetLocal: SimpleLocalLockSystem get() = targetLockSystem as SimpleLocalLockSystem

    override suspend fun releaseExpiredLock(name: String, lockContext: LockContext) =
        targetLocal.forceReleaseLock(name)
}

private class SimpleLocalLockSystem : LockSystem {
    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    override fun getId(): String = "local"

    override suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext {
        val mutex = getMutex(name)
        return if (mutex.tryLock()) {
            LockContext(true)
        } else {
            runCatching {
                withTimeout(timeout.inWholeMilliseconds) {
                    mutex.lock()
                    LockContext(true)
                }
            }.getOrDefault(LockContext(false))
        }
    }

    override suspend fun getLockIfFree(name: String, ttl: Duration): LockContext =
        LockContext(getMutex(name).tryLock())

    override suspend fun releaseLock(name: String, lockContext: LockContext): Boolean =
        getMutex(name).let { mutex ->
            mutex.isLocked.also {
                runCatching {
                    if (mutex.isLocked) {
                        mutex.unlock()
                    }
                }
            }
        }

    fun forceReleaseLock(name: String) {
        runCatching {
            val mutex = getMutex(name)
            mutex.unlock()
        }
    }

    private fun getMutex(name: String): Mutex =
        mutexMap.getOrPut(name) { Mutex() }
}
