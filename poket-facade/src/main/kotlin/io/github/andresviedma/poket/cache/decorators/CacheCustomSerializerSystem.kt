package io.github.andresviedma.poket.cache.decorators

import io.github.andresviedma.poket.support.serialization.PoketSerializer
import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.CacheSystemDecorator
import kotlin.reflect.KClass

class CacheCustomSerializerSystem(
    target: CacheSystem,
    private val customSerializer: PoketSerializer?
) : CacheSystemDecorator(target) {
    override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: KClass<V>): V? =
        if (customSerializer != null)
            super.getObject(namespace, key, String::class)?.deserialized(resultClass)
        else
            super.getObject(namespace, key, resultClass)

    override suspend fun <K : Any, V : Any> setObject(
        namespace: String,
        key: K,
        value: V,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) =
        super.setObject(namespace, key, value.maybeSerialized(), ttlSeconds, forceInvalidation)

    override suspend fun <K : Any, V : Any> getObjectList(
        namespace: String,
        keys: List<K>,
        resultClass: KClass<V>
    ): Map<K, V> =
        if (customSerializer != null)
            super.getObjectList(namespace, keys, String::class).mapValues { (_, v) -> v.deserialized(resultClass) }
        else
            super.getObjectList(namespace, keys, resultClass)

    override suspend fun <K : Any, V : Any> setObjectList(
        namespace: String,
        values: Map<K, V>,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) =
        super.setObjectList(namespace, values.mapValues { (_, v) -> v.maybeSerialized() }, ttlSeconds, forceInvalidation)

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) =
        super.setObjectList(namespace, values)

    private fun <V : Any> V.maybeSerialized(): Any =
        customSerializer?.serialize(this) ?: this

    @Suppress("UNCHECKED_CAST")
    private fun <V : Any> String.deserialized(resultClass: KClass<V>): V =
        customSerializer!!.deserialize(this, resultClass) ?: (this as V)
}
