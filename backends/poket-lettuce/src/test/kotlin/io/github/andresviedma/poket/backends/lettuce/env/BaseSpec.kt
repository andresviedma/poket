package io.github.andresviedma.poket.backends.lettuce.env

import io.kotest.core.listeners.ProjectListener
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.AutoScan
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult

/**
 * This class can be used as base for any project "base" spec with a shared test
 * environment with a Redis instance, with data deleted in every test run.
 */
abstract class BaseSpec(
    body: FeatureSpec.() -> Unit = {}
) : FeatureSpec(initialized(body)) {
    companion object {
        fun initialized(body: FeatureSpec.() -> Unit = {}): FeatureSpec.() -> Unit = {
            isolationMode = IsolationMode.InstancePerTest

            listener(
                object : TestListener, AutoCloseable {
                    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
                        IntegrationEnvironment.resetAll() // Reset servers for next test
                    }

                    override fun close() {
                        IntegrationEnvironment.resetAll()
                    }
                }
            )

            this.body()
        }
    }
}

@AutoScan
object IntegrationEnvironmentListener : ProjectListener {
    override suspend fun beforeProject() {
    }

    override suspend fun afterProject() {
        IntegrationEnvironment.stopAll()
    }
}
