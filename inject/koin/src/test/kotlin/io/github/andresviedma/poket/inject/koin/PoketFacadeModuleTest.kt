package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.cache.CacheSystemProvider
import io.github.andresviedma.poket.cache.MapCacheSystem
import io.github.andresviedma.poket.cache.ObjectCacheFactory
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.mutex.DistributedMutexFactory
import io.github.andresviedma.poket.mutex.LockSystem
import io.github.andresviedma.poket.mutex.LockSystemProvider
import io.github.andresviedma.poket.mutex.local.DisabledLockSystem
import io.github.andresviedma.poket.mutex.local.LocalLockSystem
import io.github.andresviedma.poket.poketCoreModule
import io.github.andresviedma.poket.transaction.TransactionDataHandler
import io.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.andresviedma.poket.transaction.utils.SagaTransactionHandler
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.datetime.Clock
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module

class PoketFacadeModuleTest : StringSpec({
    val koin = startKoin {
        modules(
            poketModule(poketCoreModule),

            module {
                single { SimpleMeterRegistry() }.bind<MeterRegistry>()
                single { Clock.System }.bind<Clock>()
            }
        )
    }.koin

    afterSpec { GlobalContext.stopKoin() }

    "config bindings" {
        val configProvider = koin.get<ConfigProvider>()
        val sources = configProvider.getPrivateProperty<List<ConfigSource>>("sources")
        sources.toSet() shouldBe setOf(
        )
    }

    "transaction bindings" {
        val transactionManager = TransactionWrapper.transactionManager
        val handlers = transactionManager.getPrivateProperty<List<TransactionDataHandler>>("transactionHandlers")

        handlers.size shouldBe 2
        handlers shouldContain koin.get<SagaTransactionHandler>()
        handlers.map { it.javaClass.simpleName } shouldContain "ObjectCacheTransactionHandler"
    }

    "mutex bindings" {
        val distributedMutexFactory = koin.get<DistributedMutexFactory>()
        val lockSystemProvider = distributedMutexFactory.getPrivateProperty<LockSystemProvider>("lockSystemProvider")
        val registeredSystems = lockSystemProvider.getPrivateProperty<Set<LockSystem>>("registeredSystems")
        registeredSystems shouldBe setOf(
            koin.get<LocalLockSystem>(),
            koin.get<DisabledLockSystem>(),
        )
    }

    "cache bindings" {
        val objectCacheFactory = koin.get<ObjectCacheFactory>()
        val cacheSystemProvider = objectCacheFactory.getPrivateProperty<CacheSystemProvider>("cacheSystemProvider")
        val registeredSystems = cacheSystemProvider.getPrivateProperty<Set<CacheSystem>>("registeredSystems")
        registeredSystems shouldBe setOf(
            koin.get<MapCacheSystem>(),
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
