package io.github.andresviedma.poket.inject.testcommons

import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.CacheSystemProvider
import io.github.andresviedma.poket.cache.ObjectCacheFactory
import io.github.andresviedma.poket.cache.local.MapCacheSystem
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.mutex.LockSystemProvider
import io.github.andresviedma.poket.mutex.local.DisabledLockSystem
import io.github.andresviedma.poket.mutex.local.LocalLockSystem
import io.github.andresviedma.poket.poketCoreBindings
import io.github.andresviedma.poket.transaction.TransactionDataHandler
import io.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.andresviedma.poket.transaction.utils.SagaTransactionHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

open class PoketFacadeModuleBaseTest <I : Any> (
    private val engine: GenericInjector<I>,
) : StringSpec({

    engine.createInjector(poketCoreBindings)
    afterSpec { engine.reset() }

    "config bindings" {
        val configProvider = engine.getInstance<ConfigProvider>()
        val sources = configProvider.getPrivateProperty<Set<ConfigSource>>("sources")
        sources.toSet() shouldBe setOf(
        )
    }

    "transaction bindings" {
        val transactionManager = TransactionWrapper.transactionManager
        val handlers = transactionManager.getPrivateProperty<List<TransactionDataHandler>>("transactionHandlers")

        handlers.map { it.javaClass.simpleName }.toSet() shouldBe setOf(
            "ObjectCacheTransactionHandler",
            SagaTransactionHandler::class.simpleName,
        )
    }

    "mutex bindings" {
        val distributedMutexFactory = engine.getInstance<DistributedMutexFactory>()
        val lockSystemProvider = distributedMutexFactory.getPrivateProperty<LockSystemProvider>("lockSystemProvider")
        val registeredSystems = lockSystemProvider.getPrivateProperty<Set<LockSystem>>("registeredSystems")
        registeredSystems.map { it::class } shouldBe setOf(
            LocalLockSystem::class,
            DisabledLockSystem::class,
        )
    }

    "cache bindings" {
        val objectCacheFactory = engine.getInstance<ObjectCacheFactory>()
        val cacheSystemProvider = objectCacheFactory.getPrivateProperty<CacheSystemProvider>("cacheSystemProvider")
        val registeredSystems = cacheSystemProvider.getPrivateProperty<Set<CacheSystem>>("registeredSystems")
        registeredSystems.map { it::class } shouldBe setOf(
            MapCacheSystem::class,
        )
    }
})

@Suppress("unchecked_cast")
private fun <T> Any.getPrivateProperty(property: String): T {
    val propertyMethod = "get" + property.replaceFirstChar { it.titlecase() }
    val handlersMethod = this::class.java.declaredMethods.firstOrNull { it.name == propertyMethod }
    if (handlersMethod != null) {
        handlersMethod.trySetAccessible()
        return handlersMethod.invoke(this) as T
    } else {
        val propertyField = this::class.java.declaredFields.first { it.name == property }
        propertyField.trySetAccessible()
        return propertyField.get(this) as T
    }
}
