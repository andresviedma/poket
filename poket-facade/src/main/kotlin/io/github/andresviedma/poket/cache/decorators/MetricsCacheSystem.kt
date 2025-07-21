package io.github.andresviedma.poket.cache.decorators

import io.github.andresviedma.poket.cache.CacheMetrics
import io.github.andresviedma.poket.cache.CacheSystem
import kotlin.reflect.KClass

internal class MetricsCacheSystem(
    private val target: CacheSystem,
    private val metrics: CacheMetrics,
) : CacheSystem by target {

    override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: KClass<V>): V? =
        metrics.recordTimer("get", getId(), namespace, recordHitMiss = true) {
            target.getObject(namespace, key, resultClass)
        }

    override suspend fun <K : Any, V : Any> setObject(
        namespace: String,
        key: K,
        value: V,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) {
        metrics.recordTimer("put", getId(), namespace) {
            target.setObject(namespace, key, value, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any> invalidateObject(namespace: String, key: K) {
        metrics.recordTimer("invalidate", getId(), namespace) {
            target.invalidateObject(namespace, key)
        }
    }

    override suspend fun <K : Any, V : Any> getObjectList(
        namespace: String,
        keys: List<K>,
        resultClass: KClass<V>
    ): Map<K, V> =
        metrics.recordTimer("blockGet", getId(), namespace, blockSize = keys.size) {
            target.getObjectList(namespace, keys, resultClass)
        }

    override suspend fun <K : Any, V : Any> setObjectList(
        namespace: String,
        values: Map<K, V>,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) {
        metrics.recordTimer("blockPut", getId(), namespace, blockSize = values.size) {
            target.setObjectList(namespace, values, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        metrics.recordTimer("blockPut", getId(), namespace, blockSize = values.size) {
            target.setObjectList(namespace, values)
        }
    }

    override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) {
        metrics.recordTimer("blockInvalidate", getId(), namespace, blockSize = keys.size) {
            target.invalidateObjectList(namespace, keys)
        }
    }
}
