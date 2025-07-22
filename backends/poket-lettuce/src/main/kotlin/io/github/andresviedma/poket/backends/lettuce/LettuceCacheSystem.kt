package io.github.andresviedma.poket.backends.lettuce

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.utils.cacheKeyToString
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import io.github.andresviedma.poket.support.serialization.jackson.JacksonPoketSerializer
import io.github.andresviedma.poket.support.serialization.jackson.ObjectMapperProvider
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.SetArgs
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KClass

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceCacheSystem(
    private val redisConnection: RedisLettuceConnection,
    objectMapperProvider: ObjectMapperProvider,
) : CacheSystem {
    private val cacheSerializer: PoketSerializer = JacksonPoketSerializer(objectMapperProvider)

    override fun getId(): String = "lettuce-redis"

    override suspend fun <K : Any, V : Any> getObject(
        namespace: String,
        key: K,
        resultClass: KClass<V>,
    ): V? =
        redisConnection.coroutines
            .get(cacheKeyToString(namespace, key))
            ?.deserialized(resultClass)

    override suspend fun <K : Any> invalidateObject(
        namespace: String,
        key: K,
    ) {
        redisConnection.coroutines.del(cacheKeyToString(namespace, key))
    }

    override suspend fun <K : Any, V : Any> setObject(
        namespace: String,
        key: K,
        value: V,
        ttlSeconds: Long,
        forceInvalidation: Boolean,
    ) {
        println("- set: ${cacheKeyToString(namespace, key)}")
        redisConnection.coroutines.set(
            key = cacheKeyToString(namespace, key),
            value = value.serialized(),
            setArgs = SetArgs().ex(ttlSeconds),
        )
    }

    override suspend fun <K : Any, V : Any> getObjectList(
        namespace: String,
        keys: List<K>,
        resultClass: KClass<V>,
    ): Map<K, V> {
        val keysMap = keys.associateBy { cacheKeyToString(namespace, it) }
        return redisConnection.coroutines
            .mget(*keysMap.keys.toTypedArray())
            .toList()
            .map { it.key to runCatching { it.value }.getOrNull() }
            .filter { (_, v) -> v != null }
            .associate { (key, value) -> keysMap[key]!! to value!!.deserialized(resultClass) }
    }

    override suspend fun <K : Any, V : Any> setObjectList(
        namespace: String,
        values: Map<K, V>,
        ttlSeconds: Long,
        forceInvalidation: Boolean,
    ) {
        // mset does not support TTL, so we set it individually
        values.forEach { (key, value) ->
            setObject(namespace, key, value, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any, V : Any> setObjectList(
        namespace: String,
        values: Map<K, Triple<V, Long, Boolean>>,
    ) {
        // mset does not support TTL, so we set it individually
        values.keys.forEach { key ->
            val (value, ttlSeconds, forceInvalidation) = values[key]!!
            setObject(namespace, key, value, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any> invalidateObjectList(
        namespace: String,
        keys: List<K>,
    ) {
        redisConnection.coroutines.del(*keys.map { cacheKeyToString(namespace, it) }.toTypedArray())
    }

    override suspend fun <K1 : Any> invalidateChildren(namespace: String, parentKey: K1) {
        val multiCacheKey = cacheKeyToString(namespace, parentKey to "*")
        println("*** $multiCacheKey")
        redisConnection.coroutines.keys(multiCacheKey).toList().forEach { key ->
            println("* Deleting $key")
            redisConnection.coroutines.del(key)
        }
    }

    private fun <T : Any> String.deserialized(clazz: KClass<T>): T =
        cacheSerializer.deserialize(this, clazz)

    private fun <T : Any> T.serialized(): String =
        cacheSerializer.serialize(this)

    companion object {
        fun withObjectMapper(
            redisConnection: RedisLettuceConnection,
            objectMapper: ObjectMapper,
        ): LettuceCacheSystem =
            LettuceCacheSystem(redisConnection, ObjectMapperProvider.of(objectMapper))
    }
}
