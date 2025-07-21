package io.github.andresviedma.poket.mutex.watcher

import io.github.andresviedma.poket.support.async.PoketAsyncRunnerProvider
import io.github.andresviedma.poket.mutex.LockContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class LockExpirationWatcher(
    private val releaseLockOperation: suspend (String, LockContext) -> Unit,
    private val releaseLockConnection: suspend (LockContext) -> Unit
) {
    private val locks = ConcurrentHashMap<String, Job>()

    suspend fun scheduleTtlExpiration(lock: String, ttl: Duration, context: LockContext) {
        if (ttl.inWholeMilliseconds == 0L) return

        locks[lock] = PoketAsyncRunnerProvider.launcher.launch("lock-expiration") {
            try {
                delay(ttl.inWholeMilliseconds)
                if (isActive) {
                    if (locks.containsKey(lock)) {
                        logger.warn { "DB Lock expired TTL: $lock" }
                        locks.remove(lock)
                        releaseLockOperation(lock, context)
                    } else {
                        logger.warn { "DB Lock expired TTL but lock is not registered yet: $lock" }
                        releaseLockConnection(context)
                    }
                }
            } catch (e: Throwable) {
                logger.error { "Error trying to release lock with TTL expired: $lock - ${e.message}" }
            }
        }
    }

    fun killTtlExpirationJob(name: String) {
        val job = locks[name]
        job?.cancel()
    }
}
