package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object DefaultJacksonMappers {
    /** Default Jackson ObjectMapper to be used for serialization */
    val DEFAULT_JACKSON_SERIALIZER: ObjectMapper = jacksonObjectMapper()
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    /** Default Jackson ObjectMapper to be used for serialization, but using snake case for properties */
    val DEFAULT_JACKSON_SNAKECASE_SERIALIZER: ObjectMapper = jacksonObjectMapper()
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
