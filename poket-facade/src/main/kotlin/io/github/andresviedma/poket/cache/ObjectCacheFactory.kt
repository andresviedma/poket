package io.github.andresviedma.poket.cache

import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import kotlin.reflect.KClass

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
        createCache(type, V::class, serializationVersion, customSerializer, defaultTypeConfig)

    fun <K : Any, V : Any> createCache(
        type: String,
        valueClass: KClass<V>,
        serializationVersion: String = "1",
        customSerializer: PoketSerializer? = null,
        defaultTypeConfig: CacheTypeConfig? = null
    ): ObjectCache<K, V> =
        ObjectCache(
            mutexFactory, cacheSystemProvider, configProvider, cacheMetrics,
            type, valueClass, serializationVersion, customSerializer, defaultTypeConfig
        )

    inline fun <reified K : Any, reified V : Any> createBlockingCache(
        type: String,
        serializationVersion: String = "1",
        customSerializer: PoketSerializer? = null,
        defaultTypeConfig: CacheTypeConfig? = null
    ): BlockingObjectCache<K, V> =
        BlockingObjectCache(createCache(type, serializationVersion, customSerializer, defaultTypeConfig))

    fun <K : Any, V : Any> createBlockingCache(
        type: String,
        valueClass: KClass<V>,
        serializationVersion: String = "1",
        customSerializer: PoketSerializer? = null,
        defaultTypeConfig: CacheTypeConfig? = null
    ): BlockingObjectCache<K, V> =
        BlockingObjectCache(createCache(type, valueClass, serializationVersion, customSerializer, defaultTypeConfig))
}
