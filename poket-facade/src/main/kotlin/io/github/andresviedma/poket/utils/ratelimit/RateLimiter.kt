package io.github.andresviedma.poket.utils.ratelimit

import io.github.andresviedma.poket.cache.CacheTypeConfig
import io.github.andresviedma.poket.cache.ObjectCache
import io.github.andresviedma.poket.cache.ObjectCacheFactory
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.mutex.MutexOnErrorAction
import io.github.andresviedma.poket.mutex.MutexTypeConfig
import io.github.andresviedma.poket.support.SystemProvider
import io.github.andresviedma.poket.support.metrics.incrementCounter
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class RateLimiterFactory(
    private val configProvider: ConfigProvider,
    private val mutexFactory: DistributedMutexFactory,
    private val cacheFactory: ObjectCacheFactory,
) {
    fun createRateLimiter(type: String) =
        RateLimiter(type, configProvider, SystemProvider.meterRegistry, mutexFactory, cacheFactory)
}

/**
 * Class that allows creating a rate limit mechanism for any kind of events.
 * It is based on config, describing how many events can be done in how much time.
 */
class RateLimiter(
    private val type: String,
    private val configProvider: ConfigProvider,
    private val meterRegistry: MeterRegistry,
    mutexFactory: DistributedMutexFactory,
    cacheFactory: ObjectCacheFactory
) {
    private val clock: Clock by lazy { SystemProvider.clock }

    private val globalCacheKey: String = "-"
    private val rateLimiterMutex = mutexFactory.createMutex(
        "ratelimit-$type",
        baseTypeConfig = MutexTypeConfig(onLockSystemError = MutexOnErrorAction.GET)
    )
    private val eventCache: ObjectCache<Any, List<Instant>> = cacheFactory.createCache(
        "ratelimit-$type",
        defaultTypeConfig = CacheTypeConfig(
            ttlInSeconds = 5.minutes.inWholeSeconds
        )
    )
    private var forceRateLimit: Boolean = false

    private suspend fun config(): RateLimitTypeConfig =
        configProvider.get<RateLimitConfig>().getTypeConfig(type)

    suspend fun eventInRateLimit(timestamp: Instant = clock.now()): Boolean =
        eventInRateLimit(globalCacheKey, timestamp)

    suspend fun eventInRateLimit(key: Any, timestamp: Instant = clock.now()): Boolean = config().let { config ->
        when {
            !config.active -> true
            forceRateLimit -> false
            else -> {
                rateLimiterMutex.synchronized(key) {
                    eventInRateLimitLogic(key, timestamp, config)
                }
            }
        }
    }

    suspend fun <T> runIfInRateLimit(timestamp: Instant = clock.now(), action: suspend () -> T): T? =
        runIfInRateLimit(globalCacheKey, timestamp, action)

    suspend fun <T> runIfInRateLimit(key: Any, timestamp: Instant = clock.now(), action: suspend () -> T): T? =
        if (eventInRateLimit(key, timestamp)) {
            action()
        } else {
            null
        }

    suspend fun exceedsRateLimit(key: Any, timestamp: Instant = clock.now()): Boolean =
        !eventInRateLimit(key, timestamp)

    suspend fun exceedsRateLimit(timestamp: Instant = clock.now()): Boolean =
        !eventInRateLimit(timestamp)

    fun forceRateLimitInEvents() {
        forceRateLimit = true
    }

    private suspend fun eventInRateLimitLogic(cacheKey: Any, timestamp: Instant, config: RateLimitTypeConfig): Boolean {
        val slotEvents = eventCache.get(cacheKey)
            ?.filter { (timestamp - it).inWholeMilliseconds <= config.slotTimeMillis }
            ?: emptyList()

        val inLimit = if (slotEvents.size + 1 > config.maxEvents) {
            eventCache.put(cacheKey, slotEvents)
            false
        } else {
            eventCache.put(cacheKey, slotEvents + timestamp)
            true
        }

        val metricsResult = if (inLimit) "ok" else "exceeded"
        meterRegistry.incrementCounter("rateLimit", mapOf("type" to type, "result" to metricsResult))
        return inLimit
    }
}
