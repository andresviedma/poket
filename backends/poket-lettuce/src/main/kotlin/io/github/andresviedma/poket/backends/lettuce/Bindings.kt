package io.github.andresviedma.poket.backends.lettuce

import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.support.inject.InjectorBindings

val poketLettuceBindings = InjectorBindings(
    singletons = listOf(
        RedisLettuceConnection::class,
    ),
    multiBindings = mapOf(
        LockSystem::class to listOf(
            LettuceLockSystem::class
        ),
        CacheSystem::class to listOf(
            LettuceCacheSystem::class
        ),
    )
)
