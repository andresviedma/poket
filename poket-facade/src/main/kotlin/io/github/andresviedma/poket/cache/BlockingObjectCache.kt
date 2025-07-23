package io.github.andresviedma.poket.cache

import kotlinx.coroutines.runBlocking

@Suppress("unused")
class BlockingObjectCache<K : Any, V : Any>(
    private val objectCache: ObjectCache<K, V>,
) {
    fun get(key: K): V? =
        runBlocking { objectCache.get(key) }

    fun getList(keys: List<K>): Map<K, V> =
        runBlocking { objectCache.getList(keys) }

    fun put(key: K, value: V, ttlInSecondsOverride: Long? = null, forceInvalidation: Boolean = true) =
        runBlocking { objectCache.put(key, value, ttlInSecondsOverride, forceInvalidation) }

    fun putList(values: Map<K, V>, ttlInSecondsOverride: Long? = null, forceInvalidation: Boolean = true) =
        runBlocking { objectCache.putList(values, ttlInSecondsOverride, forceInvalidation) }

    fun invalidate(key: K) =
        runBlocking { objectCache.invalidate(key) }

    fun invalidateList(keys: List<K>) =
        runBlocking { objectCache.invalidateList(keys) }

    fun getOrPut(key: K, generator: () -> V): V =
        runBlocking { objectCache.getOrPut(key, generator) }

    fun getOrPutBlock(keys: List<K>, generator: (List<K>) -> Map<K, V>): Map<K, V> =
        runBlocking { objectCache.getOrPutBlock(keys, generator) }

    fun invalidateChildren(parentKey: Any) =
        runBlocking { objectCache.invalidateChildren(parentKey) }

    fun invalidateAll() =
        runBlocking { objectCache.invalidateAll() }
}
