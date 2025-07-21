package io.github.andresviedma.poket.backends.redisson

import io.github.andresviedma.poket.support.serialization.jackson.DefaultJacksonMappers.DEFAULT_JACKSON_SERIALIZER
import io.netty.channel.EventLoopGroup
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import java.util.concurrent.ExecutorService

class RedissonClientProvider(
    private val redisConfig: RedissonConfig
) {
    private val executor: ExecutorService? = null

    private val eventLoopGroup: EventLoopGroup? = null

    private var redissonSyncClient: RedissonClient? = null
    private val redissonClient: RedissonReactiveClient by lazy { createRedissonClient() }

    fun getClient(): RedissonReactiveClient {
        return redissonClient
    }

    fun close() {
        redissonSyncClient?.shutdown()
    }

    private fun createRedissonClient(): RedissonReactiveClient {
        val config = redisConfig.redissonConfig

        val codec = JsonJacksonKotlinCodec(DEFAULT_JACKSON_SERIALIZER)
        config.codec = codec

        if (executor != null) config.executor = executor
        if (eventLoopGroup != null) config.eventLoopGroup = eventLoopGroup

        redissonSyncClient = Redisson.create(config)
        return redissonSyncClient!!.reactive()
    }
}
