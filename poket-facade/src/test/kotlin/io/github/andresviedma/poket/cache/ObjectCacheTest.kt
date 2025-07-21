package io.github.andresviedma.poket.cache

import io.github.andresviedma.poket.cache.decorators.ObjectCacheTransactionHandler
import io.github.andresviedma.poket.cache.local.MapCacheSystem
import io.github.andresviedma.poket.config.utils.configWith
import io.github.andresviedma.poket.mutex.local.LocalLockSystem
import io.github.andresviedma.poket.mutex.local.distributedMutexFactoryStub
import io.github.andresviedma.poket.support.SystemProvider
import io.github.andresviedma.poket.support.async.DefaultPoketAsyncRunner
import io.github.andresviedma.poket.support.async.PoketAsyncRunnerProvider
import io.github.andresviedma.poket.support.serialization.ClassPoketSerializer
import io.github.andresviedma.poket.support.serialization.PoketSerializer
import io.github.andresviedma.poket.testutils.Metric
import io.github.andresviedma.poket.testutils.generatedMetrics
import io.github.andresviedma.poket.testutils.testMicrometerRegistry
import io.github.andresviedma.poket.transaction.TransactionManager
import io.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.andresviedma.poket.transaction.suspendable.transactional
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.Where
import io.github.andresviedma.trekkie.then
import io.github.andresviedma.trekkie.thenExceptionThrown
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.data.row
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlin.reflect.KClass

class ObjectCacheTest : FeatureSpec({
    isolationMode = IsolationMode.InstancePerTest

    val cacheSystem = MapCacheSystem()
    val cacheSystem2 = object : MapCacheSystem() {
        override fun getId(): String = "memory-perpetual-2"
    }

    val meterRegistry = testMicrometerRegistry()
    SystemProvider.overriddenMeterRegistry = meterRegistry

    @Suppress("ktlint:standard:statement-wrapping")
    val errorCacheSystem = object : MapCacheSystem() {
        override fun getId(): String = "error-system"
        override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: KClass<V>): V? { throw Exception("boom") }
        override suspend fun <K : Any, V : Any> setObject(namespace: String, key: K, value: V, ttlSeconds: Long, forceInvalidation: Boolean) { boom() }
        override suspend fun <K : Any> invalidateObject(namespace: String, key: K) { boom() }
        override suspend fun <K : Any, V : Any> getObjectList(namespace: String, keys: List<K>, resultClass: KClass<V>): Map<K, V> { throw Exception("boom") }
        override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean) { boom() }
        override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) { boom() }
        override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) { boom() }

        private fun boom() { throw Exception("boom") }
    }

    val baseConfig = CacheConfig(
        default = CacheTypeConfig(
            cacheSystem = cacheSystem.getId(),
            ttlInSeconds = 30,
            requestCollapsing = false
        ),
        type = mapOf(
            "test2" to CacheTypeConfig(
                cacheSystem = cacheSystem2.getId()
            ),
            "error" to CacheTypeConfig(
                cacheSystem = errorCacheSystem.getId()
            )
        )
    )
    val disabledConfig = baseConfig.copy(default = baseConfig.default.copy(disabled = true))
    val config = configWith(baseConfig)

    val cacheProvider = CacheSystemProvider.withCacheSystems(CacheMetrics(), config, cacheSystem, cacheSystem2, errorCacheSystem)
    TransactionWrapper.overriddenTransactionManager = TransactionManager.withHandlers(ObjectCacheTransactionHandler(cacheProvider))

    fun objectCache(type: String, serializationVersion: String = "1", customSerializer: PoketSerializer? = null) =
        ObjectCache<String, String>(
            mutexFactory = distributedMutexFactoryStub(LocalLockSystem()),
            systemProvider = cacheProvider,
            configProvider = config,
            metrics = CacheMetrics(),
            type = type,
            valueClass = String::class,
            serializationVersion = serializationVersion,
            customSerializer = customSerializer
        )

    val (objectCache, objectCache2, errorCache) = listOf("test", "test2", "error").map { type -> objectCache(type) }
    val objectCacheVersion3 = objectCache(type = "test", serializationVersion = "3")

    val customSerializer = ClassPoketSerializer(
        clazz = Int::class,
        serialize = { "ser:$it" },
        deserialize = { it.substring(4).toInt() }
    )
    val objectCacheCustomSerializer = ObjectCache<String, Int>(
        mutexFactory = distributedMutexFactoryStub(LocalLockSystem()),
        systemProvider = cacheProvider,
        configProvider = config,
        metrics = CacheMetrics(),
        type = "test",
        valueClass = Int::class,
        customSerializer = customSerializer
    )

    fun CacheConfig.withErrorConfigHandling(override: CacheTypeConfig.() -> CacheTypeConfig) =
        copy(type = mapOf("error" to CacheTypeConfig(cacheSystem = errorCacheSystem.getId())
            .override()))

    suspend fun waitForAsyncJobs() =
        (PoketAsyncRunnerProvider.launcher as DefaultPoketAsyncRunner).waitForAllPendingJobs()

    feature("get") {
        scenario("value not in cache") {
            When {
                objectCache.get("my-key")
            } then {
                it shouldBe null
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "miss", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("value in cache") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-value")
            }
            When {
                objectCache.get("my-key")
            } then {
                it shouldBe "my-value"
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "hit", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        Where(
            row("component namespace",
                CacheTypeConfig(distributedComponentNamespace = "myservice"), "myservice-test-1"),
            row("version", CacheTypeConfig(version = "v10"), "test-1-v10")
        ) { (description, cacheConfig, namespace) ->
            scenario("value in cache: namespace with specific $description") {
                Given(config) {
                    override(CacheConfig::class) {
                        copy(
                            type = mapOf(
                                "test" to cacheConfig
                            )
                        )
                    }
                }
                Given(cacheSystem) {
                    contains(namespace, "my-key", "my-value")
                }
                When {
                    objectCache.get("my-key")
                } then {
                    it shouldBe "my-value"
                    meterRegistry.generatedMetrics() shouldBe setOf(
                        Metric(
                            "cache.get",
                            mapOf(
                                "result" to "hit",
                                "cacheSystem" to "memory-perpetual",
                                "type" to namespace
                            ),
                            timer = true
                        )
                    )
                }
            }
        }

        scenario("value in cache using serializationVersion") {
            Given(cacheSystem) {
                contains("test-3", "my-key", "my-value")
            }
            When {
                objectCacheVersion3.get("my-key")
            } then {
                it shouldBe "my-value"
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric(
                        "cache.get",
                        mapOf(
                            "result" to "hit",
                            "cacheSystem" to "memory-perpetual",
                            "type" to "test-3"
                        ),
                        timer = true
                    )
                )
            }
        }

        scenario("cache disabled") {
            Given(config) {
                override(disabledConfig)
            }
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-value")
            }
            When {
                objectCache.get("my-key")
            } then {
                it shouldBe null
                meterRegistry.generatedMetrics() shouldBe emptySet()
            }
        }

        scenario("configured cache system that does not exist") {
            Given(config) {
                override(CacheConfig::class) {
                    copy(
                        type = mapOf(
                            "test" to CacheTypeConfig(cacheSystem = "kk")
                        )
                    )
                }
            }
            When {
                objectCache.get("my-key")
            }.thenExceptionThrown(IllegalStateException::class)
        }

        scenario("error in cache when exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnGetError = true) }
                }
            }
            When {
                errorCache.get("my-key")
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when exceptions disabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnGetError = false) }
                }
            }
            When {
                errorCache.get("my-key")
            }.then {
                it shouldBe null
            }
        }

        scenario("with custom serializer") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "ser:678")
            }
            When {
                objectCacheCustomSerializer.get("my-key")
            } then {
                it shouldBe 678
            }
        }

        scenario("with custom serializer and no value") {
            When {
                objectCacheCustomSerializer.get("my-key")
            } then {
                it shouldBe null
            }
        }
    }

    feature("put") {
        scenario("default params, value not yet in cache") {
            When {
                objectCache.put("my-key", "my-value")
            } then {
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem.valueTtl("test-1", "my-key") shouldBe 30
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe true

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("default params, value already in cache") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            When {
                objectCache.put("my-key", "my-value")
            } then {
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem.valueTtl("test-1", "my-key") shouldBe 30
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe true

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        Where(
            row("invalidate + no ttl override", null, true, 30L),
            row("invalidate + ttl override", 100L, true, 100L),
            row("no invalidate + no ttl override", null, false, 30L),
            row("no invalidate + ttl override", 100L, false, 100L)
        ) { (description, ttlOverride, invalidate, expectedTtl) ->
            scenario("$description, value not yet in cache") {
                When {
                    objectCache.put(
                        "my-key",
                        "my-value",
                        ttlInSecondsOverride = ttlOverride,
                        forceInvalidation = invalidate
                    )
                } then {
                    cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                    cacheSystem.valueTtl("test-1", "my-key") shouldBe expectedTtl
                    cacheSystem.valueInvalidated("test-1", "my-key") shouldBe invalidate

                    meterRegistry.generatedMetrics() shouldBe setOf(
                        Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                    )
                }
            }

            scenario("$description, value already in cache") {
                Given(cacheSystem) {
                    contains("test-1", "my-key", "my-old-value")
                }
                When {
                    objectCache.put(
                        "my-key",
                        "my-value",
                        ttlInSecondsOverride = ttlOverride,
                        forceInvalidation = invalidate
                    )
                } then {
                    cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                    cacheSystem.valueTtl("test-1", "my-key") shouldBe expectedTtl
                    cacheSystem.valueInvalidated("test-1", "my-key") shouldBe invalidate

                    meterRegistry.generatedMetrics() shouldBe setOf(
                        Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                    )
                }
            }
        }

        scenario("value not yet in cache, transaction commit") {
            When {
                transactional {
                    objectCache.put("my-key", "my-value")
                }
            } then {
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem.valueTtl("test-1", "my-key") shouldBe 30
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe true

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockPut", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("value not yet in cache, transaction rollback") {
            When {
                transactional {
                    objectCache.put("my-key", "my-value")
                    error("boom")
                }
            } thenExceptionThrown { _: IllegalStateException ->
                cacheSystem.content("test-1") shouldBe mapOf()
                meterRegistry.generatedMetrics() shouldBe setOf()
            }
        }

        scenario("value already in cache, double transaction commit") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            Given(cacheSystem2) {
                contains("test2-1", "my-key-2", "my-old-value-2")
            }
            When {
                transactional {
                    objectCache.put("my-key", "my-value")
                    objectCache2.put("my-key-2", "my-value-2")
                }
            } then {
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem2.content("test2-1") shouldBe mapOf("my-key-2" to "my-value-2")

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockPut", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPut", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual-2", "type" to "test2-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual-2", "type" to "test2-1"), timer = true)
                )
            }
        }

        scenario("value already in cache, double transaction rollback") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            Given(cacheSystem2) {
                contains("test2-v3", "my-key-2", "my-old-value-2")
            }
            When {
                transactional {
                    objectCache.put("my-key", "my-value")
                    objectCache2.put("my-key-2", "my-value-2")
                    error("boom")
                }
            } thenExceptionThrown { _: IllegalStateException ->
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-old-value")
                cacheSystem2.content("test2-v3") shouldBe mapOf("my-key-2" to "my-old-value-2")

                meterRegistry.generatedMetrics() shouldBe setOf()
            }
        }

        scenario("cache disabled") {
            Given(config) {
                override(disabledConfig)
            }
            When {
                objectCache.put("my-key", "my-value")
            } then {
                cacheSystem.content("test-1") shouldBe emptyMap<String, String>()
                meterRegistry.generatedMetrics() shouldBe emptySet()
            }
        }

        scenario("value already in cache, with serializationVersion") {
            Given(cacheSystem) {
                contains("test-3", "my-key", "my-old-value")
            }
            When {
                objectCacheVersion3.put("my-key", "my-value")
            } then {
                cacheSystem.content("test-3") shouldBe mapOf("my-key" to "my-value")
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-3"), timer = true)
                )
            }
        }

        scenario("error in cache when exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnPutError = true) }
                }
            }
            When {
                errorCache.put("my-key", "my-value")
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when exceptions disabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnPutError = false) }
                }
            }
            When {
                errorCache.put("my-key", "my-value")
            }.then {
                // no error
            }
        }

        scenario("with custom serializer, value already in cache") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "ser:444")
            }
            When {
                objectCacheCustomSerializer.put("my-key", 987)
            } then {
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "ser:987")
            }
        }
    }

    feature("invalidate") {
        scenario("value not yet in cache") {
            When {
                objectCache.invalidate("my-key")
            } then {
                cacheSystem.content("test-1").shouldBeEmpty()
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.invalidate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("value already in cache") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            When {
                objectCache.invalidate("my-key")
            } then {
                cacheSystem.content("test-1").shouldBeEmpty()
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.invalidate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("value not yet in cache, transaction commit") {
            When {
                transactional {
                    objectCache.invalidate("my-key")
                }
            } then {
                cacheSystem.content("test-1").shouldBeEmpty()
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockInvalidate", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockInvalidateSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("value not yet in cache, transaction rollback") {
            When {
                transactional {
                    objectCache.invalidate("my-key")
                    error("boom")
                }
            } thenExceptionThrown { _: IllegalStateException ->
                cacheSystem.content("test-1") shouldBe mapOf()
                meterRegistry.generatedMetrics() shouldBe setOf()
            }
        }

        scenario("value already in cache, transaction commit") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            When {
                transactional {
                    objectCache.invalidate("my-key")
                }
            } then {
                cacheSystem.content("test-1") shouldBe emptyMap()
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockInvalidate", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockInvalidateSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("value already in cache, transaction rollback") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            When {
                transactional {
                    objectCache.invalidate("my-key")
                    error("boom")
                }
            } thenExceptionThrown { _: IllegalStateException ->
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-old-value")
                meterRegistry.generatedMetrics() shouldBe setOf()
            }
        }

        scenario("cache disabled") {
            Given(config) {
                override(disabledConfig)
            }
            When {
                objectCache.invalidate("my-key")
            } then {
                cacheSystem.content("test-1") shouldBe emptyMap<String, String>()
                meterRegistry.generatedMetrics() shouldBe emptySet()
            }
        }

        scenario("value already in cache with serializationVersion") {
            Given(cacheSystem) {
                contains("test-3", "my-key", "my-old-value")
            }
            When {
                objectCacheVersion3.invalidate("my-key")
            } then {
                cacheSystem.content("test-3").shouldBeEmpty()
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.invalidate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-3"), timer = true)
                )
            }
        }

        scenario("error in cache when exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnInvalidateError = true) }
                }
            }
            When {
                errorCache.invalidate("my-key")
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when exceptions disabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnInvalidateError = false) }
                }
            }
            When {
                errorCache.invalidate("my-key")
            }.then {
                // no error
            }
        }
    }

    feature("getOrPut") {
        scenario("no collapse, value not yet in cache") {
            When {
                objectCache.getOrPut("my-key") { "my-value" }
            } then {
                it shouldBe "my-value"
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem.valueTtl("test-1", "my-key") shouldBe 30
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "miss", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.generate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("no collapse, value already in cache") {
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
            }
            When {
                objectCache.getOrPut("my-key") { "my-value" }
            } then {
                it shouldBe "my-old-value"
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-old-value")
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "hit", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("request collapsing, value not yet in cache, no ongoing request") {
            Given(config) {
                override(CacheConfig::class) {
                    copy(
                        type = mapOf(
                            "test" to CacheTypeConfig(requestCollapsing = true)
                        )
                    )
                }
            }
            When {
                objectCache.getOrPut("my-key") { "my-value" }
            } then {
                it shouldBe "my-value"
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem.valueTtl("test-1", "my-key") shouldBe 30
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "miss", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.generate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("request collapsing of two requests") {
            Given(config) {
                override(CacheConfig::class) {
                    copy(
                        type = mapOf(
                            "test" to CacheTypeConfig(requestCollapsing = true)
                        )
                    )
                }
            }
            When {
                awaitAll(
                    async {
                        objectCache.getOrPut("my-key") {
                            delay(100)
                            "my-value-1"
                        }
                    },
                    async {
                        delay(10)
                        objectCache.getOrPut("my-key") { "my-value-2" }
                    }
                )
            } then {
                it shouldBe listOf("my-value-1", "my-value-1")
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value-1")

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric(
                        "cache.get",
                        mapOf("result" to "miss", "cacheSystem" to "memory-perpetual", "type" to "test-1"),
                        count = 2,
                        timer = true
                    ),
                    Metric(
                        "cache.get",
                        mapOf("result" to "hit", "cacheSystem" to "memory-perpetual", "type" to "test-1"),
                        count = 1,
                        timer = true
                    ),
                    Metric("cache.put", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.generate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("no collapse, value not yet in cache, transaction commit") {
            When {
                transactional {
                    objectCache.getOrPut("my-key") { "my-value" }
                }
            } then {
                it shouldBe "my-value"
                cacheSystem.content("test-1") shouldBe mapOf("my-key" to "my-value")
                cacheSystem.valueTtl("test-1", "my-key") shouldBe 30
                cacheSystem.valueInvalidated("test-1", "my-key") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("cacheSystem" to "memory-perpetual", "result" to "miss", "type" to "test-1"), timer = true),
                    Metric("cache.blockPut", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.generate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("no collapse, value not yet in cache, transaction rollback") {
            When {
                transactional {
                    objectCache.getOrPut("my-key") { "my-value" }
                    error("boom")
                }
            } thenExceptionThrown { _: IllegalStateException ->
                cacheSystem.content("test-1") shouldBe emptyMap()
                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "miss", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.generate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("cache disabled") {
            Given(config) {
                override(disabledConfig)
            }
            When {
                objectCache.getOrPut("my-key") { "my-value" }
            } then {
                it shouldBe "my-value"
                cacheSystem.content("test-1") shouldBe emptyMap<String, String>()
                meterRegistry.generatedMetrics() shouldBe emptySet()
            }
        }

        scenario("error in cache when get exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnGetError = true) }
                }
            }
            When {
                errorCache.getOrPut("my-key") { "my-value" }
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when put exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnPutError = true) }
                }
            }
            When {
                errorCache.getOrPut("my-key") { "my-value" }
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when get and put exceptions disabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnGetError = false, failOnPutError = false, failOnInvalidateError = true) }
                }
            }
            When {
                errorCache.getOrPut("my-key") { "my-value" }
            }.then {
                it shouldBe "my-value"
            }
        }

        scenario("cache with background updates, value already in cache not outdated") {
            Given(config) {
                override(CacheConfig::class) {
                    copy(
                        type = mapOf(
                            "test" to CacheTypeConfig(outdateTimeInSeconds = 60 * 5)
                        )
                    )
                }
            }
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
                contains("test-gents", "my-key", Clock.System.now().toEpochMilliseconds())
            }
            When {
                objectCache.getOrPut("my-key") { "my-value" }
                    .also { waitForAsyncJobs() }
            } then {
                it shouldBe "my-old-value"

                cacheSystem.content("test-1") shouldBe
                    mapOf("my-key" to "my-old-value")

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("result" to "hit", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.get", mapOf("type" to "test-gents", "result" to "hit", "cacheSystem" to "memory-perpetual"), timer = true)
                )
            }
        }

        scenario("cache with background updates, value already in cache and outdated") {
            Given(config) {
                override(CacheConfig::class) {
                    copy(
                        type = mapOf(
                            "test" to CacheTypeConfig(outdateTimeInSeconds = 60 * 5)
                        )
                    )
                }
            }
            Given(cacheSystem) {
                contains("test-1", "my-key", "my-old-value")
                contains("test-gents", "my-key", Clock.System.now().toEpochMilliseconds() - 60 * 6 * 1000)
            }
            When {
                objectCache.getOrPut("my-key") { "my-value" }
                    .also { waitForAsyncJobs() }
            } then {
                it shouldBe "my-old-value"

                cacheSystem.content("test-1") shouldBe
                    mapOf("my-key" to "my-value")

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.get", mapOf("type" to "test-1", "result" to "hit", "cacheSystem" to "memory-perpetual"), timer = true),
                    Metric("cache.get", mapOf("type" to "test-gents", "result" to "hit", "cacheSystem" to "memory-perpetual"), timer = true),
                    Metric("cache.generate", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.put", mapOf("type" to "test-1", "cacheSystem" to "memory-perpetual"), timer = true),
                    Metric("cache.put", mapOf("type" to "test-gents", "cacheSystem" to "memory-perpetual"), timer = true)
                )
            }
        }
    }

    feature("getOrPutBlock") {
        scenario("all values already in cache") {
            Given(cacheSystem) {
                contains("test-1", "k1", "old-k1")
                contains("test-1", "k2", "old-k2")
            }
            When {
                objectCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    keys.associateWith { "new-$it" }
                }
            } then {
                it shouldBe mapOf("k1" to "old-k1", "k2" to "old-k2")
                cacheSystem.content("test-1") shouldBe mapOf("k1" to "old-k1", "k2" to "old-k2")
                cacheSystem.valueInvalidated("test-1", "k1") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockGet", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGetSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("no value in cache") {
            When {
                objectCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    keys.associateWith { "new-$it" }
                }
            } then {
                it shouldBe mapOf("k1" to "new-k1", "k2" to "new-k2")
                cacheSystem.content("test-1") shouldBe mapOf("k1" to "new-k1", "k2" to "new-k2")
                cacheSystem.valueInvalidated("test-1", "k1") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockGet", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGetSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPut", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerate", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerateSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("some values in cache") {
            var passedKeys: List<String>? = null
            Given(cacheSystem) {
                contains("test-1", "k2", "old-k2")
            }
            When {
                objectCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    passedKeys = keys
                    keys.associateWith { "new-$it" }
                }
            } then {
                it shouldBe mapOf("k1" to "new-k1", "k2" to "old-k2")
                cacheSystem.content("test-1") shouldBe mapOf("k1" to "new-k1", "k2" to "old-k2")
                passedKeys shouldBe listOf("k1")
                cacheSystem.valueInvalidated("test-1", "k1") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockGet", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGetSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPut", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerate", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerateSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("some values in cache, transaction commit") {
            var passedKeys: List<String>? = null
            Given(cacheSystem) {
                contains("test-1", "k2", "old-k2")
            }
            When {
                transactional {
                    objectCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                        passedKeys = keys
                        keys.associateWith { "new-$it" }
                    }
                }
            } then {
                it shouldBe mapOf("k1" to "new-k1", "k2" to "old-k2")
                cacheSystem.content("test-1") shouldBe mapOf("k1" to "new-k1", "k2" to "old-k2")
                passedKeys shouldBe listOf("k1")
                cacheSystem.valueInvalidated("test-1", "k1") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockGet", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGetSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPut", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockPutSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerate", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerateSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("some values in cache, transaction rollback") {
            var passedKeys: List<String>? = null
            var result: Map<String, String>? = null
            Given(cacheSystem) {
                contains("test-1", "k2", "old-k2")
            }
            When {
                transactional {
                    result = objectCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                        passedKeys = keys
                        keys.associateWith { "new-$it" }
                    }
                    error("boom")
                }
            } thenExceptionThrown { _: IllegalStateException ->
                result shouldBe mapOf("k1" to "new-k1", "k2" to "old-k2")
                cacheSystem.content("test-1") shouldBe mapOf("k2" to "old-k2")
                passedKeys shouldBe listOf("k1")
                cacheSystem.valueInvalidated("test-1", "k1") shouldBe false

                meterRegistry.generatedMetrics() shouldBe setOf(
                    Metric("cache.blockGet", mapOf("blockSize" to "2", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGetSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerate", mapOf("blockSize" to "1", "cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true),
                    Metric("cache.blockGenerateSize", mapOf("cacheSystem" to "memory-perpetual", "type" to "test-1"), timer = true)
                )
            }
        }

        scenario("cache disabled") {
            Given(config) {
                override(disabledConfig)
            }
            When {
                objectCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    keys.associateWith { "new-$it" }
                }
            } then {
                it shouldBe mapOf("k1" to "new-k1", "k2" to "new-k2")
                cacheSystem.content("test-1") shouldBe emptyMap<String, String>()
                meterRegistry.generatedMetrics() shouldBe emptySet()
            }
        }

        scenario("error in cache when get exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnGetError = true) }
                }
            }
            When {
                errorCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    keys.associateWith { "new-$it" }
                }
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when put exceptions enabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnPutError = true) }
                }
            }
            When {
                errorCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    keys.associateWith { "new-$it" }
                }
            }.thenExceptionThrown(Exception::class)
        }

        scenario("error in cache when get and put exceptions disabled") {
            Given(config) {
                override(CacheConfig::class) {
                    withErrorConfigHandling { copy(failOnGetError = false, failOnPutError = false, failOnInvalidateError = true) }
                }
            }
            When {
                errorCache.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    keys.associateWith { "new-$it" }
                }
            }.then {
                it shouldBe mapOf("k1" to "new-k1", "k2" to "new-k2")
            }
        }

        scenario("with custom serializer, some values in cache") {
            var passedKeys: List<String>? = null
            Given(cacheSystem) {
                contains("test-1", "k2", "ser:555")
            }
            When {
                objectCacheCustomSerializer.getOrPutBlock(listOf("k1", "k2")) { keys ->
                    passedKeys = keys
                    keys.mapIndexed { index, key -> key to index + 1 }.toMap()
                }
            } then {
                it shouldBe mapOf("k1" to 1, "k2" to 555)
                cacheSystem.content("test-1") shouldBe mapOf("k1" to "ser:1", "k2" to "ser:555")
                passedKeys shouldBe listOf("k1")
                cacheSystem.valueInvalidated("test-1", "k1") shouldBe false
            }
        }
    }
})
