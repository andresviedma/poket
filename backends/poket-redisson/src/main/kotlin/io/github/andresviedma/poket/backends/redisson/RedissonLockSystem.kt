package io.github.andresviedma.poket.backends.redisson

import io.github.andresviedma.poket.mutex.LockContext
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.support.metrics.recordTimer
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration

class RedissonLockSystem(
    private val redisProvider: RedissonClientProvider,
    private val meterRegistry: MeterRegistry
) : LockSystem {

    override fun getId(): String = "redisson"

    override suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext =
        meterRegistry.recordTimer("redis.lock.get") {
            val lock = redisProvider.getClient().getLock(name)
            val ownerId = Random.nextLong()
            val lockAcquired = lock?.tryLock(timeout.inWholeMilliseconds, ttl.inWholeMilliseconds, TimeUnit.MILLISECONDS, ownerId)?.awaitFirstOrNull()
            LockContext(lockAcquired == true, ownerId)
        }

    override suspend fun getLockIfFree(name: String, ttl: Duration): LockContext {
        return waitLock(name, Duration.ZERO, ttl)
    }

    override suspend fun releaseLock(name: String, lockContext: LockContext): Boolean =
        meterRegistry.recordTimer("redis.lock.release") {
            val lockReleased = redisProvider.getClient().getLock(name)?.forceUnlock()?.awaitSingle() == true
            lockContext.hasLock = !lockReleased
            lockReleased
        }
}
