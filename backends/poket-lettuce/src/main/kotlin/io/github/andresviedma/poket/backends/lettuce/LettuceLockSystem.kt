package io.github.andresviedma.poket.backends.lettuce

import com.himadieiev.redpulsar.lettuce.abstracts.LettuceUnified
import com.himadieiev.redpulsar.lettuce.locks.LockFactory
import io.github.andresviedma.poket.mutex.LockContext
import io.github.andresviedma.poket.mutex.LockSystem
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

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
        val mutex =
            LockFactory.createMutex(
                listOf(redisConnection.toLettuceUnified()),
                retryDelay = waitLockPollInterval.toJavaDuration(),
                retryCount = ((timeout.inWholeMilliseconds + waitLockPollInterval.inWholeMilliseconds - 1) / waitLockPollInterval.inWholeMilliseconds).toInt(),
            )
        val gotLock = mutex.lock(name, ttl.toJavaDuration())
        return LockContext(gotLock)
    }

    override suspend fun getLockIfFree(
        name: String,
        ttl: Duration,
    ): LockContext {
        val mutex =
            LockFactory.createMutex(
                listOf(redisConnection.toLettuceUnified()),
                retryDelay = 1.milliseconds.toJavaDuration(),
                retryCount = 1,
            )
        val gotLock = mutex.lock(name, ttl.toJavaDuration())
        return LockContext(gotLock)
    }

    override suspend fun releaseLock(
        name: String,
        lockContext: LockContext,
    ): Boolean =
        LockFactory
            .createMutex(
                listOf(redisConnection.toLettuceUnified()),
                retryDelay = 1.milliseconds.toJavaDuration(),
                retryCount = 1,
            ).unlock(name)

    private fun RedisLettuceConnection.toLettuceUnified(): LettuceUnified<String, String> =
        object : LettuceUnified<String, String> {
            override fun <R> sync(consumer: (RedisClusterCommands<String, String>) -> R): R = consumer(this@toLettuceUnified.sync)
        }
}
