package io.github.andresviedma.poket.backends.redisson

import io.github.andresviedma.poket.backends.redisson.env.IntegrationEnvironment
import io.github.andresviedma.poket.config.utils.configWith
import io.github.andresviedma.poket.mutex.DistributedMutex
import io.github.andresviedma.poket.mutex.LockSystemProvider
import io.github.andresviedma.poket.mutex.LockWaitTimedOutException
import io.github.andresviedma.poket.mutex.MutexConfig
import io.github.andresviedma.poket.mutex.MutexTypeConfig
import io.github.andresviedma.poket.support.serialization.jackson.ObjectMapperProvider
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.and
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class RedissonLockSystemTest : FeatureSpec({
    // Warm up slf4j so that it doesn't affect the times for concurrent executions
    LoggerFactory.getLogger("test").trace("* Warming up slf4j")

    val redisConfig = RedissonConfig.singleConnection(IntegrationEnvironment.redis.address)
    val lockSystem = RedissonLockSystem(RedissonClientProvider(redisConfig, ObjectMapperProvider.ofDefaultMapper()), CompositeMeterRegistry())

    val mutexSettings = MutexTypeConfig(
        lockSystem = lockSystem.getId(),
        timeoutInMillis = 10_000,
        ttlInMillis = 10_000
    )
    val mutexConfig = configWith(MutexConfig(default = mutexSettings))
    val mutex = DistributedMutex(
        LockSystemProvider.withLockSystems(lockSystem),
        mutexConfig,
        "test-mutex"
    )

    feature("synchronized with parallel requests") {
        scenario("successful execution") {
            val events = mutableListOf<String>()
            When {
                (1..2).map {
                    async {
                        mutex.synchronized {
                            events += "start"
                            delay(100)
                            events += "end"
                        }
                    }
                }.awaitAll()
            } then {
                events shouldBe listOf(
                    "start",
                    "end",
                    "start",
                    "end"
                )
            }
        }

        scenario("with exception") {
            val events = mutableListOf<String>()
            When {
                (1..2).map {
                    async {
                        runCatching {
                            mutex.synchronized {
                                events += "start"
                                delay(100)
                                events += "end"

                                error("booom")
                            }
                        }
                    }
                }.awaitAll()
            } then { result ->
                result[0].shouldBeFailure { it is IllegalStateException }
                result[1].shouldBeFailure { it is IllegalStateException }
            } and {
                events shouldBe listOf(
                    "start",
                    "end",
                    "start",
                    "end"
                )
            }
        }

        scenario("with TTL expiration") {
            val events = mutableListOf<String>()
            Given(mutexConfig) {
                override(MutexConfig(default = mutexSettings.copy(ttlInMillis = 5)))
            }
            When {
                (1..2).map {
                    async {
                        mutex.synchronized {
                            events += "start"
                            delay(400)
                            events += "end"
                        }
                    }
                }.awaitAll()
            } then {
                events shouldBe listOf(
                    "start",
                    "start",
                    "end",
                    "end"
                )
            }
        }

        scenario("with expired Timeout") {
            val events = mutableListOf<String>()
            Given(mutexConfig) {
                override(MutexConfig(default = mutexSettings.copy(timeoutInMillis = 5)))
            }
            When {
                (1..2).map {
                    async {
                        runCatching {
                            mutex.synchronized {
                                events += "start"
                                delay(100)
                                events += "end"
                                true
                            }
                        }
                    }
                }.awaitAll()
            } then { result ->
                val failures = result.filter { it.isFailure }
                failures shouldHaveSize 1
                failures[0].shouldBeFailure { it is LockWaitTimedOutException }
            } and {
                events shouldBe listOf(
                    "start",
                    "end"
                )
            }
        }
    }

    feature("maybeSynchronized") {
        scenario("gotLock parameter is correctly handled") {
            val events = mutableListOf<String>()
            When {
                (1..2).map {
                    async {
                        mutex.maybeSynchronized { gotLock ->
                            if (gotLock) {
                                events += "in mutex"
                                delay(100)
                                events += "in mutex end"
                            } else {
                                delay(20)
                                events += "not in mutex"
                            }
                        }
                    }
                }.awaitAll()
            } then {
                events shouldBe listOf(
                    "in mutex",
                    "not in mutex",
                    "in mutex end"
                )
            }
        }
    }

    feature("ifSynchronized") {
        scenario("condition is handled correctly") {
            val events = mutableListOf<String>()
            When {
                (1..2).map {
                    async {
                        mutex.ifSynchronized {
                            events += "in mutex"
                            delay(100)
                            events += "in mutex end"
                            "ok"
                        }
                    }
                }.awaitAll()
            } then {
                it.toSet() shouldBe setOf("ok", null)
                events shouldBe listOf(
                    "in mutex",
                    "in mutex end"
                )
            }
        }
    }
})
