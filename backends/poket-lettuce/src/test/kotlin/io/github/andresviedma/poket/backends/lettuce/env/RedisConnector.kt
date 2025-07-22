package io.github.andresviedma.poket.backends.lettuce.env

import io.github.andresviedma.poket.backends.lettuce.RedisConfig
import io.github.andresviedma.poket.backends.lettuce.RedisLettuceConnection
import io.github.andresviedma.poket.config.utils.configWith
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName
import java.net.Socket
import java.time.Duration
import java.time.Instant

const val REDIS_DEFAULT_PORT = 6379
const val REDIS_DEFAULT_HOST = "localhost"

private val logger = KotlinLogging.logger {}

class RedisConnector {

    val redisClient: RedisLettuceConnection by lazy { createClient() }
    val address get() = "redis://${getHost()}:${getPort()}"

    private var serverStarted: Boolean = false
    private var container: RedisContainer? = null

    object RedisContainer : GenericContainer<RedisContainer>(DockerImageName.parse("redis:6.2.4-alpine"))

    fun forceStarted() {
        synchronized(this) {
            when {
                serverStarted -> return
                isPortUsed(REDIS_DEFAULT_PORT) -> logger.info { "*** Redis server: using external instance already running" }
                else -> {
                    logger.logTimedOperation("Redis server start") {
                        container = RedisContainer
                            .withExposedPorts(REDIS_DEFAULT_PORT)
                            .withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger(logger.name))).apply { start() }
                    }
                }
            }
            serverStarted = true
        }
    }

    private fun isPortUsed(port: Int): Boolean =
        runCatching { Socket(REDIS_DEFAULT_HOST, port).close() }.isSuccess

    fun reset() {
        if (serverStarted) {
            redisClient.sync.flushall()
        }
    }

    fun stop() {
        container?.let { container ->
            logger.logTimedOperation("Redis server stop") {
                container.stop()
                serverStarted = false
            }
        }
    }

    fun getHost(): String {
        forceStarted()
        return container?.host ?: "localhost"
    }

    fun getPort(): Int {
        forceStarted()
        return container?.getMappedPort(REDIS_DEFAULT_PORT) ?: REDIS_DEFAULT_PORT
    }

    private fun createClient(): RedisLettuceConnection {
        forceStarted()
        return RedisLettuceConnection(configWith(RedisConfig(uri = address)))
    }

    private fun <T> KLogger.logTimedOperation(operation: String, block: () -> T): T {
        val t0 = Instant.now()
        info { "*** $operation: start" }
        try {
            val result = block()
            val t1 = Instant.now()
            info { "*** $operation: done (${Duration.between(t0, t1)}s)" }
            return result
        } catch (exc: Throwable) {
            val t1 = Instant.now()
            error { "*** $operation: ERROR (${Duration.between(t0, t1)}) -- ${exc.message}" }
            throw exc
        }
    }
}
