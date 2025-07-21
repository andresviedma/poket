package io.github.andresviedma.poket.backends.redisson

import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.utils.cacheKeyToString
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.time.Duration
import kotlin.reflect.KClass

class RedissonCacheSystem(
    private val redisProvider: RedissonClientProvider
) : CacheSystem {

    override fun getId(): String = "redis"

    override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: KClass<V>): V? {
        return getBucket<K, V>(namespace, key)?.get()?.awaitFirstOrNull()
    }

    override suspend fun <K : Any, V : Any> setObject(namespace: String, key: K, value: V, ttlSeconds: Long, forceInvalidation: Boolean) {
        getBucket<K, V>(namespace, key)?.set(value, Duration.ofSeconds(ttlSeconds))?.awaitFirstOrNull()
    }

    override suspend fun <K : Any> invalidateObject(namespace: String, key: K) {
        getBucket<K, Any>(namespace, key)?.andDelete?.awaitFirstOrNull()
    }

    override suspend fun <K : Any, V : Any> getObjectList(namespace: String, keys: List<K>, resultClass: KClass<V>): Map<K, V> {
        val stringKeyMap = keys.associateBy { cacheKeyToString(namespace, it) }
        val objects = redisProvider.getClient().buckets.get<V>(*stringKeyMap.keys.toTypedArray()).awaitFirstOrDefault(emptyMap())
        return objects.mapKeys { stringKeyMap[it.key]!! }
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean) {
        val batch = redisProvider.getClient().createBatch()
        values.forEach {
            batch.getBucket<V>(cacheKeyToString(namespace, it.key))
                ?.set(it.value, Duration.ofSeconds(ttlSeconds))
        }
        batch.execute().awaitFirstOrNull()
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        val batch = redisProvider.getClient().createBatch()
        values.forEach {
            batch.getBucket<V>(cacheKeyToString(namespace, it.key))
                ?.set(it.value.first, Duration.ofSeconds(it.value.second))
        }
        batch.execute().awaitFirstOrNull()
    }

    override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) {
        redisProvider.getClient().keys
            .delete(*keys.map { cacheKeyToString(namespace, it) }.toTypedArray())?.awaitFirstOrNull()
    }

    private fun <K : Any, V : Any> getBucket(namespace: String, key: K) =
        redisProvider.getClient().getBucket<V>(cacheKeyToString(namespace, key))
}
