package com.github.andresviedma.poket.config

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

/**
 * Provides configuration objects by configuration class, which can be read from different sources.
 * The class allows hot-reloading of configuration, where the consumer would need to adapt to configuration
 * changes.
 */
class ConfigProvider(
    private val sources: List<ConfigSource>,
    private val clock: Clock,
) {
    private val cachedConfigs: ConcurrentMap<KClass<*>, ConfigCacheEntry> = ConcurrentHashMap()

    suspend fun warmup() {
        sources.forEach { it.warmup() }
    }

    @Suppress("unchecked_cast")
    suspend fun <T : Any> getConfig(configClass: KClass<T>): T =
        cachedConfigs[configClass]
            ?.takeIf { it.isStillValid(clock) }
            ?.let { it.config as T }
            ?: fetchConfig(configClass)

    inline suspend fun <reified T : Any> get(): T =
        getConfig(T::class)

    private suspend fun <T : Any> fetchConfig(configClass: KClass<T>): T =
        sources.firstNotNullOfOrNull { source ->
            source.getConfig(configClass)?.let { source to it }
        }?.let { (source, config) ->
            val sourceReloadConfig = source.getDefaultReloadConfig()
            cachedConfigs[configClass] = ConfigCacheEntry(
                config = config,
                expiration = sourceReloadConfig?.outdateTimeInSeconds?.let { clock.now() + it.seconds }
            )
            config
        } ?: error("Class ${configClass.qualifiedName} not configured")

    private data class ConfigCacheEntry(
        val config: Any,
        val expiration: Instant?,
    ) {
        fun isStillValid(clock: Clock): Boolean =
            expiration != null && expiration < clock.now()
    }
}
