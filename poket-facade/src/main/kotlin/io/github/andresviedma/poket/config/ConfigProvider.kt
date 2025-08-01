package io.github.andresviedma.poket.config

import io.github.andresviedma.poket.config.utils.ConstantConfigSource
import io.github.andresviedma.poket.support.SystemProvider
import io.github.andresviedma.poket.support.async.PoketAsyncRunnerProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val CONFIG_RELOAD_CHECK_INTERVAL = 30.seconds

/**
 * Provides configuration objects by configuration class, which can be read from different sources.
 * The class allows hot-reloading of configuration, where the consumer would need to adapt to configuration
 * changes.
 */
class ConfigProvider(
    configSources: Set<ConfigSource>,
) {
    private val sources: List<ConfigSource> = configSources.sortedBy { it.getConfigSourcePriority().value }
    private val clock: Clock = SystemProvider.clock
    private val cachedConfigs: ConcurrentMap<KClass<*>, Any> = ConcurrentHashMap()
    private var warmedUp: Boolean = false
    private val sourcesLastUpdated: MutableMap<ConfigSource, Instant> = mutableMapOf()
    private var reloaderJob: Job? = null
    private var reloadCheckInterval: Duration = CONFIG_RELOAD_CHECK_INTERVAL

    @Suppress("unused")
    suspend fun warmup() {
        if (!warmedUp) {
            val now = clock.now()
            sources.forEach { it.reloadInfo(); sourcesLastUpdated[it] = now }

            warmedUp = true
            startReloaderDaemon()
        }
    }

    inline fun <reified T : Any> getTypedConfig(): Config<T> =
        getTypedConfig(T::class)

    fun <T : Any> getTypedConfig(configClass: KClass<T>): Config<T> =
        Config(this, configClass)

    @Suppress("unchecked_cast")
    fun <T : Any> getConfig(configClass: KClass<T>): T {
        // The ideal situation is loading this at service initialization, not now
        if (!warmedUp) runBlocking { warmup() }

        return (cachedConfigs[configClass] as? T)
            ?: fetchConfig(configClass)
    }

    inline fun <reified T : Any> get(): T =
        getConfig(T::class)

    fun killReloaderJob() {
        reloaderJob?.cancel()
    }

    inline fun <reified T : ConfigSource> source(): T? =
        source(T::class)

    fun <T : Any> override(config: T) =
        source<ConstantConfigSource>()?.override(config)
            .also { cachedConfigs.clear() }

    fun <T : Any> override(configClass: KClass<T>, block: T.() -> T) =
        source<ConstantConfigSource>()?.override(configClass, block)
            .also { cachedConfigs.clear() }

    @Suppress("UNCHECKED_CAST")
    fun <T : ConfigSource> source(clazz: KClass<T>): T? =
        sources.firstOrNull { clazz.isInstance(it) } as? T

    fun withReloadCheckInterval(newCheckInterval: Duration): ConfigProvider {
        reloadCheckInterval = newCheckInterval
        return this
    }

    private fun <T : Any> fetchConfig(configClass: KClass<T>): T {
        return sources.fold(null as T?) { currentConfig, patchSource ->
            patchSource.getConfig(configClass, currentConfig)
        }
            ?.also { cachedConfigs[configClass] = it }
            ?: error("Class ${configClass.qualifiedName} not configured")
    }

    private suspend fun startReloaderDaemon() {
        if (sources.any { it.getReloadConfig()?.outdateTime != null }) {
            reloaderJob = PoketAsyncRunnerProvider.launcher.launch("config-reloader") {
                while (true) {
                    delay(reloadCheckInterval)
                    reloadOutdatedConfigSources()
                }
            }
        }
    }

    private suspend fun reloadOutdatedConfigSources() {
        val now = clock.now()
        val somethingChanged = sources.map { source ->
            val ttl = source.getReloadConfig()?.outdateTime
            val lastUpdate = sourcesLastUpdated[source]
            if (ttl != null && lastUpdate != null && (now - lastUpdate > ttl)) {
                source.reloadInfo()
                    .also { sourcesLastUpdated[source] = now }
            } else {
                false
            }
        }.contains(true)
        if (somethingChanged) cachedConfigs.clear()
    }
}
