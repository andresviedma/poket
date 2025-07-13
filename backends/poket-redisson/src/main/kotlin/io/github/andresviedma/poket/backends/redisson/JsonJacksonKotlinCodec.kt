package io.github.andresviedma.poket.backends.redisson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import org.redisson.codec.JsonJacksonCodec

class JsonJacksonKotlinCodec : JsonJacksonCodec {

    constructor(objectMapper: ObjectMapper) : super(objectMapper)

    constructor(classLoader: ClassLoader, codec: JsonJacksonKotlinCodec) : super(classLoader, codec)

    override fun initTypeInclusion(mapObjectMapper: ObjectMapper) {
        mapObjectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Any::class.java)
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING
        )
    }
}
