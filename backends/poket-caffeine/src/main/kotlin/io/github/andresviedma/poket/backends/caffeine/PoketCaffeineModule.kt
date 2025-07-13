package io.github.andresviedma.poket.backends.caffeine

import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.support.inject.InjectorBindings

@Suppress("unused")
val poketCaffeineModule = InjectorBindings(
    multiBindings = mapOf(
        CacheSystem::class to listOf(
            CaffeineCacheSystem::class,
        ),
    ),
)
