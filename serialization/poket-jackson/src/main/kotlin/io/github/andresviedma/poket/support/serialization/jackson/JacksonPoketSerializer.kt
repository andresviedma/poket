package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.andresviedma.poket.support.inject.OptionalBinder
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import kotlin.reflect.KClass

@Suppress("unused")
class JacksonPoketSerializer(
    objectMapperProvider: ObjectMapperProvider,
) : PoketSerializer {
    private val objectMapper: ObjectMapper = objectMapperProvider.objectMapper

    override fun <T : Any> serialize(deserialized: T): String =
        objectMapper.writeValueAsString(deserialized)

    override fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T =
        objectMapper.readValue(serialized, resultClass.java)

    companion object {
        fun withObjectMapper(objectMapper: ObjectMapper): JacksonPoketSerializer =
            JacksonPoketSerializer(ObjectMapperProvider(OptionalBinder.of(objectMapper)))
    }
}
