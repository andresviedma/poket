package io.github.andresviedma.poket.config

import io.github.andresviedma.poket.config.utils.ConstantConfigSource
import io.github.andresviedma.poket.testutils.ControlledClock
import io.github.andresviedma.poket.config.utils.PatchFunctionConfigSource
import io.github.andresviedma.poket.support.SystemProvider
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.github.andresviedma.trekkie.times
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ConfigProviderTest : FeatureSpec({
    isolationMode = IsolationMode.InstancePerTest

    val clock = ControlledClock().also { SystemProvider.overriddenClock = it }

    val configObject1 = TestConfig("a", 1)
    val configObject2 = TestConfig("b", 2)

    val source1ReloadTime = 1.minutes
    val source1 = spyk(ConstantConfigSource(
        reloadConfig = ConfigSourceReloadConfig(source1ReloadTime),
        priority = ConfigPriority.BASE),
    )
    val source2 = spyk(ConstantConfigSource(
        reloadConfig = null,
        priority = ConfigPriority.APP)
    )
    val patchSource = PatchFunctionConfigSource<TestConfig>(
        priority = ConfigPriority.APP_ENVIRONMENT,
    )
    val configProvider = ConfigProvider(
        setOf(
            source2,
            patchSource,
            source1,
        ),
    ).withReloadCheckInterval(5.milliseconds)

    afterEach { configProvider.killReloaderJob() }

    feature("source initial load") {
        scenario("reload is called just once, if warmup not called") {
            Given(source1) {
                override(configObject1)
            }
            When {
                configProvider.get<TestConfig>()
                configProvider.get<TestConfig>()
            } then {
                1 * { source1.reloadInfo() }
                1 * { source2.reloadInfo() }
            }
        }

        scenario("reload is called just once, if warmup called") {
            Given(source1) {
                override(configObject1)
            }
            When {
                configProvider.warmup()
                configProvider.get<TestConfig>()
            } then {
                1 * { source1.reloadInfo() }
                1 * { source2.reloadInfo() }
            }
        }
    }

    feature("source reload") {
        scenario("data is reloaded just once after outdate time has passed and cache is cleared") {
            Given(source1) {
                override(configObject1)
            }
            Given(configProvider) {
                warmup()
            }
            When {
                configProvider.get<TestConfig>()
            } then {
                it shouldBe configObject1
                1 * { source1.reloadInfo() }
                1 * { source2.reloadInfo() }
            }
            When {
                source1.override(configObject2)
                clock.fastForward(source1ReloadTime + 10.milliseconds)
                delay(50.milliseconds)
            } then {
                2 * { source1.reloadInfo() }
                1 * { source2.reloadInfo() }
            }
            When {
                configProvider.get<TestConfig>()
            } then {
                it shouldBe configObject2
            }
        }
    }

    feature("get value combining sources and cache") {
        scenario("value for a class is cached") {
            Given(source2) {
                override(configObject1)
            }
            When {
                configProvider.get<TestConfig>()
            } then {
                it shouldBe configObject1
            }
            When {
                source2.override(configObject2)
                configProvider.get<TestConfig>()
            } then {
                it shouldBe configObject1 // the cached object, as source2 does not reload
            }
        }

        scenario("values are overridden by sources in order") {
            Given(source1) {
                override(configObject1)
            }
            Given(source2) {
                override(configObject2)
            }
            Given(patchSource) {
                override { it?.copy(b = it.b + 1) }
            }
            When {
                configProvider.get<TestConfig>()
            } then {
                it shouldBe configObject2.copy(b = configObject2.b + 1)
            }
        }
    }
})

private data class TestConfig(
    val a: String,
    val b: Int,
)
