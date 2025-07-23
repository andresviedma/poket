package io.github.andresviedma.poket.utils.ratelimit

import io.github.andresviedma.poket.cache.CacheConfig
import io.github.andresviedma.poket.cache.CacheTypeConfig
import io.github.andresviedma.poket.cache.local.MapCacheSystem
import io.github.andresviedma.poket.cache.utils.createCacheFactoryWithSystem
import io.github.andresviedma.poket.config.utils.configWith
import io.github.andresviedma.poket.mutex.local.distributedMutexFactoryStub
import io.github.andresviedma.poket.support.SystemProvider
import io.github.andresviedma.poket.testutils.ControlledClock
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

class RateLimiterTest : FeatureSpec({
    // Independent mocks per scenario
    isolationMode = IsolationMode.InstancePerTest

    val timeNow: Instant = Instant.fromEpochMilliseconds(123_450_000)
    SystemProvider.overriddenClock = ControlledClock(timeNow)

    val simpleCacheSystem = MapCacheSystem()
    val config = configWith(
        RateLimitConfig(
            default = RateLimitTypeConfig(maxEvents = 3, slotTimeMillis = 2000)
        ),
        CacheConfig(default = CacheTypeConfig(cacheSystem = simpleCacheSystem.getId()))
    )

    val simpleCacheFactory = createCacheFactoryWithSystem(
        cacheSystem = simpleCacheSystem,
        configProvider = config,
    )

    val rateLimiter = RateLimiter(
        "test", config, SimpleMeterRegistry(), distributedMutexFactoryStub(), simpleCacheFactory
    )

    feature("eventInRateLimit") {
        scenario("First event") {
            When {
                rateLimiter.eventInRateLimit(timeNow)
            } then {
                it shouldBe true
            }
        }

        scenario("Number of events in the limit") {
            Given { // Previous events generated
                rateLimiter.eventInRateLimit(timeNow - 1900.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 500.milliseconds)
            }
            When {
                rateLimiter.eventInRateLimit(timeNow)
            } then {
                it shouldBe true
            }
        }

        scenario("More events than the limit") {
            Given { // Previous events generated
                rateLimiter.eventInRateLimit(timeNow - 1900.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 500.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 100.milliseconds)
            }
            When {
                rateLimiter.eventInRateLimit(timeNow)
            } then {
                it shouldBe false
            }
        }

        scenario("Events too old are not considered") {
            Given { // Previous events generated
                rateLimiter.eventInRateLimit(timeNow - 2100.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 500.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 50.milliseconds)
            }
            When {
                rateLimiter.eventInRateLimit(timeNow)
            } then {
                it shouldBe true
            }
        }

        scenario("Disabled") {
            Given(config) {
                override(RateLimitConfig(default = RateLimitTypeConfig(disabled = true)))
            }
            Given { // Previous events generated
                rateLimiter.eventInRateLimit(timeNow - 1900.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 500.milliseconds)
                rateLimiter.eventInRateLimit(timeNow - 100.milliseconds)
            }
            When {
                rateLimiter.eventInRateLimit(timeNow)
            } then {
                it shouldBe true
            }
        }

        scenario("Events with key") {
            Given { // Previous events generated
                rateLimiter.eventInRateLimit(1, timeNow - 1900.milliseconds)
                rateLimiter.eventInRateLimit(1, timeNow - 500.milliseconds)
                rateLimiter.eventInRateLimit(1, timeNow - 200.milliseconds)
                rateLimiter.eventInRateLimit(2, timeNow - 100.milliseconds)
            }
            When {
                rateLimiter.eventInRateLimit(1, timeNow) to
                    rateLimiter.eventInRateLimit(2, timeNow)
            } then { (result1, result2) ->
                result1 shouldBe false
                result2 shouldBe true
            }
        }
    }

    feature("runIfInRateLimit") {
        scenario("Without key") {
            var counter = 0
            When {
                repeat(5) {
                    rateLimiter.runIfInRateLimit { counter++ }
                }
            } then {
                counter shouldBe 3
            }
        }

        scenario("With key") {
            var counter1 = 0
            var counter2 = 0
            When {
                repeat(5) {
                    rateLimiter.runIfInRateLimit(1) { counter1++ }
                    rateLimiter.runIfInRateLimit(2) { counter2++ }
                }
            } then {
                counter1 shouldBe 3
                counter2 shouldBe 3
            }
        }
    }
})
