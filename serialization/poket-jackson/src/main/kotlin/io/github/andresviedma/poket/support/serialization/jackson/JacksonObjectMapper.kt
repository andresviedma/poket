package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.andresviedma.poket.config.propertytree.MapObjectMapper
import io.github.andresviedma.poket.support.serialization.jackson.DefaultJacksonMappers.DEFAULT_JACKSON_SERIALIZER
import kotlin.reflect.KClass

class JacksonObjectMapper : MapObjectMapper {
    private val objectMapper: ObjectMapper = DEFAULT_JACKSON_SERIALIZER

    override fun objectToMap(configClass: KClass<*>, obj: Any?): Map<String, *>? {
        if (obj == null) return null
        val string = objectMapper.writeValueAsString(obj)
        return objectMapper.readValue<Map<String, *>>(string)
    }

    override fun <T : Any> mapToObject(configClass: KClass<T>, map: Map<String, *>): T {
        val string = objectMapper.writeValueAsString(map)
        return objectMapper.readValue(string, configClass.java)
    }
}
