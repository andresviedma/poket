package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.andresviedma.poket.config.propertytree.MapObjectMapper
import kotlin.reflect.KClass

class JacksonMapObjectMapper(
    objectMapperProvider: ObjectMapperProvider,
) : MapObjectMapper {
    private val objectMapper: ObjectMapper = objectMapperProvider.objectMapper

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
