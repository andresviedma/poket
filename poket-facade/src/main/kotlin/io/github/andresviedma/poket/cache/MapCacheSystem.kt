package io.github.andresviedma.poket.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of CacheSystem using a map with no TTL handling, only stored for verification.
 * It is intended mainly for tests and in general will not make much sense for production code,
 * unless exceptional cases of data that will rarely change and if so we will be notified for invalidation.
 */
open class MapCacheSystem : CacheSystem {
    private val cacheMap = ConcurrentHashMap<String, ConcurrentHashMap<Any, MapCacheEntry>>()

    override fun getId(): String = "memory-perpetual"

    @Suppress("UNCHECKED_CAST")
    override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: Class<V>): V? =
        keyMap(namespace)[key]?.value as V?

    override suspend fun <K : Any, V : Any> setObject(namespace: String, key: K, value: V, ttlSeconds: Long, forceInvalidation: Boolean) {
        keyMap(namespace)[key] = MapCacheEntry(value = value, ttlSeconds = ttlSeconds, invalidation = forceInvalidation)
    }

    override suspend fun <K : Any> invalidateObject(namespace: String, key: K) {
        keyMap(namespace)[key] = MapCacheEntry(value = null, ttlSeconds = 0, invalidation = true)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <K : Any, V : Any> getObjectList(namespace: String, keys: List<K>, resultClass: Class<V>): Map<K, V> {
        val objectList = mutableMapOf<K, V>()
        keys.forEach {
            val entry = keyMap(namespace)[it]?.value as V?
            if (entry != null) {
                objectList[it] = entry
            }
        }
        return objectList
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean) {
        keyMap(namespace).putAll(values.mapValues { MapCacheEntry(value = it.value, ttlSeconds = ttlSeconds, invalidation = forceInvalidation) })
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        keyMap(namespace).putAll(values.mapValues { MapCacheEntry(value = it.value.first, ttlSeconds = it.value.second, invalidation = it.value.third) })
    }

    override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) {
        keyMap(namespace).putAll(keys.associateWith { MapCacheEntry(value = null, ttlSeconds = 0, invalidation = true) })
    }

    private fun keyMap(namespace: String): ConcurrentHashMap<Any, MapCacheEntry> =
        cacheMap.getOrPut(namespace) { ConcurrentHashMap() }

    fun clear() {
        cacheMap.clear()
    }

    /** Function equivalent to setObject, for tests readability */
    fun contains(namespace: String, key: Any, value: Any) {
        keyMap(namespace)[key] = MapCacheEntry(value = value, ttlSeconds = 0, invalidation = false)
    }

    fun valueTtl(namespace: String, key: Any): Long? =
        keyMap(namespace)[key]?.ttlSeconds

    fun valueInvalidated(namespace: String, key: Any): Boolean =
        keyMap(namespace)[key]?.invalidation ?: false

    fun content(namespace: String): Map<Any, Any> =
        keyMap(namespace).filterValues { it.value != null }
            .mapValues { (_, valueEntry) -> valueEntry.value!! }
}

private data class MapCacheEntry(
    val ttlSeconds: Long,
    val value: Any?,
    val invalidation: Boolean
)
