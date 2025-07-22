package io.github.andresviedma.poket

import io.github.andresviedma.poket.cache.CacheMetrics
import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.CacheSystemProvider
import io.github.andresviedma.poket.cache.ObjectCacheFactory
import io.github.andresviedma.poket.cache.decorators.ObjectCacheTransactionHandler
import io.github.andresviedma.poket.cache.local.MapCacheSystem
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.mutex.LockSystemProvider
import io.github.andresviedma.poket.mutex.local.DisabledLockSystem
import io.github.andresviedma.poket.mutex.local.LocalLockSystem
import io.github.andresviedma.poket.support.SystemProvider
import io.github.andresviedma.poket.support.async.PoketAsyncRunnerProvider
import io.github.andresviedma.poket.support.inject.InjectorBindings
import io.github.andresviedma.poket.transaction.TransactionDataHandler
import io.github.andresviedma.poket.transaction.TransactionManager
import io.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.andresviedma.poket.transaction.utils.SagaTransactionHandler

val poketCoreBindings = InjectorBindings(
    singletons = listOf(
        ConfigProvider::class,

        CacheMetrics::class,
        CacheSystemProvider::class,
        ObjectCacheFactory::class,

        DistributedMutexFactory::class,
        LockSystemProvider::class,

        TransactionManager::class,
    ),
    multiBindings = mapOf(
        ConfigSource::class to listOf(

        ),
        CacheSystem::class to listOf(
            MapCacheSystem::class,
        ),
        LockSystem::class to listOf(
            LocalLockSystem::class,
            DisabledLockSystem::class,
        ),
        TransactionDataHandler::class to listOf(
            SagaTransactionHandler::class,

            ObjectCacheTransactionHandler::class,
        ),
    ),
    staticWrappers = listOf(
        TransactionWrapper::class,
        PoketAsyncRunnerProvider::class,
        SystemProvider::class,
    ),
)
