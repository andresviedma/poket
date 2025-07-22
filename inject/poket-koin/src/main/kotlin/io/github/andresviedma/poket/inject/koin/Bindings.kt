package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.support.inject.InjectorBindings

val injectKoinBindings = InjectorBindings(
    multiBindings = mapOf(ConfigSource::class to listOf(KoinInjectedConfigSource::class))
)
