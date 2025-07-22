package io.github.andresviedma.poket.backends.lettuce

import io.github.andresviedma.poket.mutex.LockContext
import io.github.andresviedma.poket.mutex.LockSystem
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.SetArgs
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceLockSystem(
    private val redisConnection: RedisLettuceConnection,
) : LockSystem {
    private val waitLockPollInterval = 10.milliseconds

    override fun getId(): String = "lettuce-redis"

    override suspend fun waitLock(
        name: String,
        timeout: Duration,
        ttl: Duration,
    ): LockContext {
        val t0 = Clock.System.now()
        val key = "mutex::$name"
        val value = UUID.randomUUID().toString()
        do {
            val result = redisConnection.coroutines.set(key, value, SetArgs.Builder.nx().px(ttl.toJavaDuration()))
            if (result == "OK") return LockContext(hasLock = true, lockContext = value)
            if (Clock.System.now() - t0 < timeout) {
                delay(waitLockPollInterval)
            }
        } while (Clock.System.now() - t0 < timeout)
        return LockContext(hasLock = false)
    }

    override suspend fun getLockIfFree(
        name: String,
        ttl: Duration,
    ): LockContext {
        val key = "mutex::$name"
        val value = UUID.randomUUID().toString()
        val result = redisConnection.coroutines.set(key, value, SetArgs.Builder.nx().px(ttl.toJavaDuration()))
        if (result == "OK") return LockContext(hasLock = true, lockContext = value)
        return LockContext(hasLock = false)
    }

    override suspend fun releaseLock(
        name: String,
        lockContext: LockContext,
    ): Boolean {
        val key = "mutex::$name"
        val currentValue = redisConnection.coroutines.get(key)
        if (currentValue != lockContext.lockContext) return false
        redisConnection.coroutines.del(key)
        return true
    }
}
