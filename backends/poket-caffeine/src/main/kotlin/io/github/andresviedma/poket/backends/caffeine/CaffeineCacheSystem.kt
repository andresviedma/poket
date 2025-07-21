package io.github.andresviedma.poket.backends.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.andresviedma.poket.cache.CacheSystem
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class CaffeineCacheSystem : CacheSystem {
    private val cacheMap = ConcurrentHashMap<String, Cache<Any, Any>>()

    override fun getId(): String = "memory"

    @Suppress("UNCHECKED_CAST")
    override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: KClass<V>): V? =
        cacheOrNull(namespace)?.getIfPresent(key) as V?

    override suspend fun <K : Any, V : Any> setObject(namespace: String, key: K, value: V, ttlSeconds: Long, forceInvalidation: Boolean) {
        cache(namespace, ttlSeconds).put(key, value)
    }

    override suspend fun <K : Any> invalidateObject(namespace: String, key: K) {
        cacheOrNull(namespace)?.invalidate(key)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <K : Any, V : Any> getObjectList(namespace: String, keys: List<K>, resultClass: KClass<V>): Map<K, V> =
        (cacheOrNull(namespace)?.getAllPresent(keys) ?: emptyMap()) as Map<K, V>

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean) {
        cache(namespace, ttlSeconds).putAll(values)
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        if (values.isNotEmpty()) {
            val firstEntry = values.values.first()
            setObjectList(namespace, values.mapValues { it.value.first }, firstEntry.second, firstEntry.third)
        }
    }

    override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) {
        cacheOrNull(namespace)?.invalidateAll(keys)
    }

    private fun cacheOrNull(namespace: String): Cache<Any, Any>? =
        cacheMap[namespace]

    private fun cache(namespace: String, ttlSeconds: Long): Cache<Any, Any> =
        cacheMap.getOrPut(namespace) {
            Caffeine.newBuilder()
                .maximumSize(100) // TODO configurable limit per type / namespace?
                .expireAfterWrite(ttlSeconds.seconds.toJavaDuration())
                .build()
        }
}
