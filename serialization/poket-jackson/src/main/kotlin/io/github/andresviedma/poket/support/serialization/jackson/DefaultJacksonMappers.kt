package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

@Suppress("unused")
object DefaultJacksonMappers {
    /** Default Jackson ObjectMapper to be used for serialization */
    val DEFAULT_JACKSON_OBJECT_MAPPER: ObjectMapper = JsonMapper.builder()
        .addModules(
            kotlinModule(),
            Jdk8Module(),
            JavaTimeModule()
        )
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    /** Default Jackson ObjectMapper to be used for serialization, but using snake case for properties */
    val DEFAULT_JACKSON_SNAKECASE_OBJECT_MAPPER: ObjectMapper = JsonMapper.builder()
        .addModules(
            kotlinModule(),
            Jdk8Module(),
            JavaTimeModule()
        )
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .build()
}
