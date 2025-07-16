package io.github.andresviedma.poket.cache

import io.github.andresviedma.poket.support.async.PoketAsyncRunnerProvider
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.mutex.DistributedMutex
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import kotlinx.datetime.Clock

/**
 * Main entry point to create a typed cache with configuration settings by type of cached element.
 */
@Suppress("unused")
class ObjectCacheFactory(
    private val mutexFactory: DistributedMutexFactory,
    private val cacheSystemProvider: CacheSystemProvider,
    private val configProvider: ConfigProvider,
    private val cacheMetrics: CacheMetrics
) {
    inline fun <reified K : Any, reified V : Any> createCache(
        type: String,
        serializationVersion: String = "1",
        customSerializer: PoketSerializer? = null,
        defaultTypeConfig: CacheTypeConfig? = null
    ): ObjectCache<K, V> =
        createCache(type, V::class.java, serializationVersion, customSerializer, defaultTypeConfig)

    fun <K : Any, V : Any> createCache(
        type: String,
        valueClass: Class<V>,
        serializationVersion: String = "1",
        customSerializer: PoketSerializer? = null,
        defaultTypeConfig: CacheTypeConfig? = null
    ): ObjectCache<K, V> =
        ObjectCache(
            mutexFactory, cacheSystemProvider, configProvider, cacheMetrics,
            type, valueClass, serializationVersion, customSerializer, defaultTypeConfig
        )
}

class ObjectCache<K : Any, V : Any>(
    mutexFactory: DistributedMutexFactory,
    private val systemProvider: CacheSystemProvider,
    private val configProvider: ConfigProvider,
    private val metrics: CacheMetrics,
    private val type: String,
    private val valueClass: Class<V>,

    /** Version of the object format, should be incremented when the data format changes, invalidating the previous values */
    private val serializationVersion: String = "1",

    /**
     * Custom serializer for values, useful for systems that require serialization. Anyway, every cache system will
     * always provide some system-dependent default serialization.
     */
    private val customSerializer: PoketSerializer? = null,

    /** Config that will be used as default for this cache, with precedence over "default" cache config */
    private val defaultTypeConfig: CacheTypeConfig? = null
) {
    private val collapsingMutex: DistributedMutex =
        mutexFactory.createMutex(type = "cache::$type", forceIgnoreLockErrors = true)

    /**
     * Gets the value of the given key, or null if it is not there.
     */
    suspend fun get(key: K): V? =
        ifEnabled { config ->
            getCacheSystem(config).getObject(config.namespace(), key, valueClass)
        }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun getList(keys: List<K>): Map<K, V> =
        ifEnabled { config ->
            getCacheSystem(config).getObjectList(config.namespace(), keys, valueClass)
        } ?: emptyMap()

    /**
     * Adds a value to the cache.
     * ttlInSecondsOverride allows defining a specific TTL in seconds for this value, if null the default TTL
     * configured for the type will be used, so most of the time will not be necessary.
     * forceInvalidation should be true when the target data that is being cached has being changed. This
     * mechanism is necessary for potentially needed cross-DC invalidations of distributed cache systems.
     */
    suspend fun put(key: K, value: V, ttlInSecondsOverride: Long? = null, forceInvalidation: Boolean = true) {
        ifEnabled { config ->
            val ttl = ttlInSecondsOverride ?: config.ttlInSeconds!!
            val system = getCacheSystem(config)
            system.setObject(config.namespace(), key, value, ttl, forceInvalidation)

            if (config.isUpdatableAsynchronously()) {
                val now = Clock.System.now().toEpochMilliseconds()
                system.setObject(config.generationTimeNamespace(), key, now, ttl, forceInvalidation)
            }
        }
    }

    /**
     * Adds values to the cache.
     * ttlInSecondsOverride allows defining a specific TTL in seconds for this value, if null the default TTL
     * configured for the type will be used, so most of the time will not be necessary.
     * forceInvalidation should be true when the target data that is being cached has being changed. This
     * mechanism is necessary for potentially needed cross-DC invalidations of distributed cache systems.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun putList(values: Map<K, V>, ttlInSecondsOverride: Long? = null, forceInvalidation: Boolean = true) {
        ifEnabled { config ->
            val ttl = ttlInSecondsOverride ?: config.ttlInSeconds!!
            getCacheSystem(config).setObjectList(config.namespace(), values, ttl, forceInvalidation)
        }
    }

    /**
     * Invalidates the given key in the cache.
     */
    suspend fun invalidate(key: K) {
        ifEnabled { config ->
            val system = getCacheSystem(config)
            system.invalidateObject(config.namespace(), key)

            if (config.isUpdatableAsynchronously()) {
                system.invalidateObject(config.generationTimeNamespace(), key)
            }
        }
    }

    /**
     * Invalidates the given keys in the cache.
     */
    @Suppress("unused")
    suspend fun invalidateList(keys: List<K>) {
        ifEnabled { config ->
            getCacheSystem(config).invalidateObjectList(config.namespace(), keys)
        }
    }

    /**
     * Gets a key from the cache or generates it if it is not there.
     * If configured to do so, this method will use request collapsing in case of concurrent requests,
     * making consequent requests waiting for the first one to be finished instead of calling the generator
     * function.
     */
    suspend fun getOrPut(key: K, generator: suspend () -> V): V =
        ifEnabled {
            getAndMaybeRegenerate(key, generator)
                ?: generateOrCollapse(key, generator)
        } ?: generator()

    /**
     * Gets a list of keys from the cache. For the keys that are not in the cache, the generator
     * function will be called, only with the keys that are not already in the cache.
     * This method allows generating data in blocks except of one by one, which quite often will be more efficient.
     * It does not use request collapsing even if it is configured to do so.
     */
    suspend fun getOrPutBlock(keys: List<K>, generator: suspend (List<K>) -> Map<K, V>): Map<K, V> =
        ifEnabled<Map<K, V>> { config ->
            val cachedValues = getList(keys)
            val (pending, cached) = keys.map { it to cachedValues[it] }
                .partition { it.second == null }
            val cachedMap = cached.associate { it.first to it.second!! }

            return if (pending.isEmpty()) {
                cachedMap
            } else {
                val pendingKeys = pending.map { it.first }
                val generatedValues = recordTimer("blockGenerate", config, blockSize = pendingKeys.size) {
                    generator(pendingKeys)
                }
                putList(generatedValues, forceInvalidation = false)
                cachedMap + generatedValues
            }
        }
            ?: generator(keys)

    private suspend fun getAndMaybeRegenerate(key: K, generator: suspend () -> V): V? =
        get(key)?.also {
            val config = getConfig()
            if (config.outdateTimeInSeconds != null) {
                val ts = getCacheSystem(config).getObject(config.generationTimeNamespace(), key, Long::class.java)
                if (config.isOutdated(ts, Clock.System.now())) {
                    collapsingMutex.ifSynchronized(key) {
                        PoketAsyncRunnerProvider.launcher.launch("cache-regenerate") {
                            generate(key, generator)
                        }
                    }
                }
            }
        }

    private suspend fun generateOrCollapse(key: K, generator: suspend () -> V): V =
        if (getConfig().requestCollapsing == true) {
            generateOrWait(key, generator)
        } else {
            generate(key, generator)
        }

    private suspend fun generateOrWait(key: K, generator: suspend () -> V): V =
        collapsingMutex.maybeSynchronized(key) { firstConcurrentRequest ->
            if (firstConcurrentRequest) {
                generate(key, generator)
            } else {
                collapsingMutex.synchronized(key) {
                    get(key) ?: generate(key, generator)
                }
            }
        }

    private suspend fun generate(key: K, generator: suspend () -> V): V =
        recordTimer("generate", getConfig()) {
            generator()
        }.also { put(key, it, forceInvalidation = false) }

    private fun getConfig(): CacheTypeConfig =
        configProvider.get<CacheConfig>().getTypeConfig(type, defaultTypeConfig)

    private inline fun <T> ifEnabled(block: (CacheTypeConfig) -> T): T? {
        val config = getConfig()
        return if (config.disabled == true) null else block(config)
    }

    private fun getCacheSystem(config: CacheTypeConfig, suffix: String? = null) =
        systemProvider.getCacheSystem(
            config.cacheSystem!!,
            listOfNotNull(type, suffix).joinToString("-"),
            customSerializer,
            defaultTypeConfig
        )

    private suspend fun <X> recordTimer(
        timer: String,
        config: CacheTypeConfig,
        recordHitMiss: Boolean = false,
        blockSize: Int? = null,
        block: suspend () -> X
    ): X =
        metrics.recordTimer(timer, config.cacheSystem!!, config.namespace(), recordHitMiss, blockSize, block)

    private fun CacheTypeConfig.namespace(): String =
        namespace(type, serializationVersion)

    private fun CacheTypeConfig.generationTimeNamespace(): String =
        namespace(type, "gents")
}
