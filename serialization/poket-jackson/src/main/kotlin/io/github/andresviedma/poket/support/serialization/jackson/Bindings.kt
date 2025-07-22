package io.github.andresviedma.poket.support.serialization.jackson

import io.github.andresviedma.poket.config.propertytree.MapObjectMapper
import io.github.andresviedma.poket.support.inject.InjectorBindings
import io.github.andresviedma.poket.support.serialization.PoketSerializer

val poketJacksonBindings = InjectorBindings(
    singletons = listOf(
        ObjectMapperProvider::class, // to get an optionally injected ObjectMapper
    ),
    interfaceSingletons = mapOf(
        MapObjectMapper::class to JacksonMapObjectMapper::class, // for config Map trees
        PoketSerializer::class to JacksonPoketSerializer::class, // for default cache serializing, when appliable
    )
)
