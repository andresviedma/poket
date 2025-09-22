package io.github.andresviedma.poket.cache.utils

import io.github.andresviedma.poket.cache.CacheConfig
import io.github.andresviedma.poket.cache.CacheMetrics
import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.CacheSystemProvider
import io.github.andresviedma.poket.cache.CacheTypeConfig
import io.github.andresviedma.poket.cache.ObjectCacheFactory
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.config.utils.configWith
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.mutex.LockSystemProvider
import io.github.andresviedma.poket.mutex.MutexConfig
import io.github.andresviedma.poket.mutex.MutexTypeConfig
import io.github.andresviedma.poket.mutex.local.LocalLockSystem
import io.github.andresviedma.poket.utils.retry.RetryHandler

fun createCacheFactoryWithSystem(
    cacheSystem: CacheSystem,
    lockSystem: LockSystem = LocalLockSystem(),
    baseCacheConfig: CacheTypeConfig? = null,
    baseMutexConfig: MutexTypeConfig? = null,
    configProvider: ConfigProvider? = null,
): ObjectCacheFactory {
    val config = configProvider
        ?: configWith(
            CacheConfig(
                default = CacheTypeConfig(cacheSystem = cacheSystem.getId()).overriddenWith(baseCacheConfig),
            ),
            MutexConfig(
                default = MutexTypeConfig(lockSystem = lockSystem.getId()).overriddenWith(baseMutexConfig),
            ),
        )
    return ObjectCacheFactory(
        mutexFactory = DistributedMutexFactory(
            lockSystemProvider = LockSystemProvider.withLockSystems(lockSystem),
            configProvider = config,
        ),
        cacheSystemProvider = CacheSystemProvider(
            registeredSystems = setOf(cacheSystem),
            cacheMetrics = CacheMetrics(),
            configProvider = config,
            retryHandler = RetryHandler(config),
        ),
        configProvider = config,
        cacheMetrics = CacheMetrics(),
    )
}
