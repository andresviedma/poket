package io.github.andresviedma.poket.backends.lettuce

import io.github.andresviedma.poket.config.Config
import io.github.andresviedma.poket.config.ConfigProvider
import io.lettuce.core.ClientOptions
import io.lettuce.core.ClientOptions.DisconnectedBehavior
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.SocketOptions
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.protocol.ProtocolVersion
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisLettuceConnection(
    configProvider: ConfigProvider,
) {
    private val config: Config<RedisConfig> = configProvider.getTypedConfig()

    private val internalConnection: StatefulRedisConnection<String, String> by lazy {
        RedisClient
            .create(config.get().finalUri)
            .apply {
                options =
                    ClientOptions
                        .builder()
                        .apply {
                            config.get().let { opts ->
                                opts.autoReconnect?.let { autoReconnect(it) }
                                opts.disconnectedBehavior?.let { disconnectedBehavior(it) }
                                opts.pingBeforeActivateConnection?.let { pingBeforeActivateConnection(it) }
                                opts.protocolVersion?.let { protocolVersion(it) }
                                opts.connectTimeoutMillis?.let {
                                    socketOptions(
                                        SocketOptions.builder().connectTimeout(it.milliseconds.toJavaDuration()).build(),
                                    )
                                }
                            }
                        }.build()
            }.connect()
    }

    val sync: RedisCommands<String, String> by lazy { internalConnection.sync() }
    val async: RedisAsyncCommands<String, String> by lazy { internalConnection.async() }
    val coroutines: RedisCoroutinesCommands<String, String> by lazy { internalConnection.coroutines() }
}

data class RedisConfig(
    val uri: String? = null,
    val host: String? = null, // Alternative to uri for standalone Redis instances
    val port: Int = 6379,
    val protocolVersion: ProtocolVersion? = null,
    val pingBeforeActivateConnection: Boolean? = null,
    val autoReconnect: Boolean? = null,
    val disconnectedBehavior: DisconnectedBehavior? = null,
    val connectTimeoutMillis: Long? = null,
) {
    inline val finalUri: String
        get() = this.uri ?: "redis://${host!!}:$port" // Default URI if not provided
}
