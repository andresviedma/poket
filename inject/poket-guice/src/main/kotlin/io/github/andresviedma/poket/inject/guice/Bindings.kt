package io.github.andresviedma.poket.inject.guice

import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.support.inject.InjectorBindings

val injectGuiceBindings = InjectorBindings(
    multiBindings = mapOf(
        ConfigSource::class to listOf(
            GuiceInjectedConfigSource::class,
        ),
    ),
)
