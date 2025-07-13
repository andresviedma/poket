package io.github.andresviedma.poket.backends.redisson

import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.support.inject.InjectorBindings

@Suppress("unused")
val poketResissonModule = InjectorBindings(
    multiBindings = mapOf(
        CacheSystem::class to listOf(
            RedissonCacheSystem::class,
        ),
        LockSystem::class to listOf(
            RedissonLockSystem::class,
        ),
    ),
    singletons = listOf(
        RedissonClientProvider::class,
    )
)
