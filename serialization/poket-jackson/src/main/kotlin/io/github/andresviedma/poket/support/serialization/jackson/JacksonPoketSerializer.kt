package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import kotlin.reflect.KClass

@Suppress("unused")
class JacksonPoketSerializer(
    private val objectMapper: ObjectMapper,
) : PoketSerializer {
    override fun <T : Any> serialize(deserialized: T): String =
        objectMapper.writeValueAsString(deserialized)

    override fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T =
        objectMapper.readValue(serialized, resultClass.java)
}
