package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.andresviedma.poket.support.inject.OptionalBinder
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import io.github.andresviedma.poket.support.serialization.TypedPoketSerializer
import kotlin.reflect.KClass

/**
 * This class can be used when we want to store some value in a cache whose class uses generics.
 * It needs to be used as a custom serializer for the cache.
 *
 * E.g.:
 * cacheFactory.createCache<String, List<MyDataClass>(
 *     type = "xxx",
 *     customSerializer = TypedJacksonPoketSerializer(objectMapperProvider, TypeReference<List<MyDataClass>>() {}),
 * )
 */
@Suppress("unused", "UNCHECKED_CAST")
class TypedJacksonPoketSerializer(
    objectMapperProvider: ObjectMapperProvider,
    private val typeReference: TypeReference<*>,
) : PoketSerializer {
    private val objectMapper: ObjectMapper = objectMapperProvider.objectMapper

    override fun <T : Any> serialize(deserialized: T): String =
        objectMapper.writeValueAsString(deserialized)

    override fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T =
        objectMapper.readValue(serialized, typeReference) as T
}
