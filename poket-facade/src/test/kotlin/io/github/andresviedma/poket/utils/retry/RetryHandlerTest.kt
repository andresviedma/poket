package io.github.andresviedma.poket.utils.retry

import io.github.andresviedma.poket.config.utils.configWith
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.on
import io.github.andresviedma.trekkie.then
import io.github.andresviedma.trekkie.thenExceptionThrown
import io.github.andresviedma.trekkie.times
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import java.io.IOException

@ExperimentalCoroutinesApi
class RetryHandlerTest : FeatureSpec({
    // Independent mocks per scenario
    isolationMode = IsolationMode.InstancePerTest

    val profile = "retry-profile"

    val delayController = spyk(StandardTestDispatcher())
    val scope = TestScope(delayController)

    val operation: Runnable = mockk {
        on { run() } returns Unit
    }

    val config = configWith(
        RetryProfileConfig(
            default = RetryPolicyConfig(enabled = false)
        )
    )
    val retryHandler = RetryHandler(
        config
    )

    feature("run") {
        scenario("Fail with no retries") {
            Given(operation) {
                on { run() } throws Exception("pum")
            }
            Given(config) {
                override(
                    RetryProfileConfig(profiles = mapOf(profile to RetryPolicyConfig(enabled = false)))
                )
            }
            When {
                scope.runTest {
                    retryHandler.run(profile) { operation.run() }
                }
            } thenExceptionThrown { _: Exception ->
                scope.currentTime shouldBe 0L
                1 * { operation.run() }
            }
        }

        scenario("Success with no retries") {
            Given(operation) {
                on { run() } returns Unit
            }
            Given(config) {
                override(
                    RetryProfileConfig(profiles = mapOf(profile to RetryPolicyConfig(enabled = false)))
                )
            }
            When {
                scope.runTest {
                    retryHandler.run(profile) { operation.run() }
                }
            } then {
                scope.currentTime shouldBe 0L
                1 * { operation.run() }
            }
        }

        scenario("Retries: Some fails and final success") {
            Given(operation) {
                on { run() } throws Exception("pum") andThenThrows Exception("pum") andThen Unit
            }
            Given(config) {
                override(
                    RetryProfileConfig(
                        profiles = mapOf(profile to RetryPolicyConfig(listOf(1000, 2000, 4000)))
                    )
                )
            }
            When {
                scope.runTest {
                    retryHandler.run(profile) { operation.run() }
                }
            } then {
                it shouldBe Unit
                scope.currentTime shouldBe 3000L
                3 * { operation.run() }
            }
        }

        scenario("Retries: all fail") {
            Given(operation) {
                on { run() } throws Exception("pum") andThenThrows Exception("pum") andThen Unit
            }
            Given(config) {
                override(
                    RetryProfileConfig(
                        profiles = mapOf(profile to RetryPolicyConfig(listOf(1000)))
                    )
                )
            }
            When {
                scope.runTest {
                    retryHandler.run(profile) { operation.run() }
                }
            } thenExceptionThrown { _: Exception ->
                scope.currentTime shouldBe 1000L
                2 * { operation.run() }
            }
        }

        scenario("Retries: using default profile") {
            Given(operation) {
                on { run() } throws Exception("pum") andThenThrows Exception("pum") andThen Unit
            }
            Given(config) {
                override(
                    RetryProfileConfig(
                        profiles = mapOf(profile to RetryPolicyConfig(retries = listOf(1000))),
                        default = RetryPolicyConfig(retries = listOf(500))
                    )
                )
            }
            When {
                scope.runTest {
                    retryHandler.run { operation.run() }
                }
            } thenExceptionThrown { _: Exception ->
                scope.currentTime shouldBe 500L
                2 * { operation.run() }
            }
        }

        scenario("Retries: Custom error handler") {
            Given(operation) {
                on { run() } throws IOException("pum") andThenThrows IllegalStateException("pum") andThen Unit
            }
            Given(config) {
                override(
                    RetryProfileConfig(
                        profiles = mapOf(profile to RetryPolicyConfig(retries = listOf(1000, 2000, 4000)))
                    )
                )
            }
            When {
                scope.runTest {
                    retryHandler.run(profile, { it.isFailure && it.exceptionOrNull() is IOException }) { operation.run() }
                }
            } thenExceptionThrown { _: IllegalStateException ->
                scope.currentTime shouldBe 1000L
                2 * { operation.run() }
            }
        }
    }
})
