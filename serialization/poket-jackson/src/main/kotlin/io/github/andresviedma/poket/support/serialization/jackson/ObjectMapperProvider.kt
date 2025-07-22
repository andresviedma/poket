package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.andresviedma.poket.support.inject.OptionalBinder
import io.github.andresviedma.poket.support.inject.getOptionalInstance
import io.github.andresviedma.poket.support.serialization.jackson.DefaultJacksonMappers.DEFAULT_JACKSON_OBJECT_MAPPER

class ObjectMapperProvider(
    private val optionalBinder: OptionalBinder,
) {
    val objectMapper: ObjectMapper get() = optionalBinder.getOptionalInstance() ?: DEFAULT_JACKSON_OBJECT_MAPPER

    companion object {
        fun of(objectMapper: ObjectMapper) = ObjectMapperProvider(OptionalBinder.of(objectMapper))
        fun ofDefaultMapper() = ObjectMapperProvider(OptionalBinder.ofNull())
    }
}
