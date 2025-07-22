package io.github.andresviedma.poket.inject.guice

import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.support.inject.InjectorBindings
import io.github.andresviedma.poket.support.inject.OptionalBinder

val injectGuiceBindings = InjectorBindings(
    multiBindings = mapOf(
        ConfigSource::class to listOf(
            GuiceInjectedConfigSource::class,
        ),
    ),
    interfaceSingletons = mapOf(
        OptionalBinder::class to GuiceOptionalBinder::class,
    ),
)
