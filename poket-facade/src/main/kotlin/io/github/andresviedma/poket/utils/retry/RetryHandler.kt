package io.github.andresviedma.poket.utils.retry

import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.support.metrics.incrementCounter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import kotlinx.coroutines.delay

class RetryHandler(
    private val configProvider: ConfigProvider,
    private val meterRegistry: MeterRegistry,
) {
    constructor(configProvider: ConfigProvider) :
        this(configProvider, CompositeMeterRegistry())

    suspend fun <T> run(
        profile: String? = null,
        errorDetection: (Result<T>) -> Boolean = { it.isFailure },
        block: suspend () -> T
    ): T =
        runIndexed(profile, errorDetection) { _, _ -> block() }

    suspend fun <T> runIndexed(
        profile: String? = null,
        errorDetection: (Result<T>) -> Boolean = { it.isFailure },
        block: suspend (Int, Boolean) -> T
    ): T {
        val config: RetryProfileConfig = configProvider.get()
        val profileConfig = config.getProfileConfig(profile)
        return CustomRetryHandler(profile, profileConfig, meterRegistry, errorDetection).run(block)
    }
}

class CustomRetryHandler<T>(
    private val profile: String?,
    private val config: RetryPolicyConfig,
    private val meterRegistry: MeterRegistry,
    private val errorDetection: (Result<T>) -> Boolean = { it.isFailure }
) {
    suspend fun run(block: suspend (Int, Boolean) -> T): T {
        (0..config.maxRetries).forEach { step ->
            val result = try {
                Result.success(block(step, step >= config.maxRetries))
            } catch (e: Throwable) {
                Result.failure(e)
            }

            if (needsRetry(step, result)) {
                delay(retryTime(step))
            } else {
                meterRegistry.incrementCounter(
                    counter = "retry",
                    tags = mapOf(
                        "profile" to (profile ?: "default"),
                        "result" to if (errorDetection(result)) "error" else "ok",
                        "attempt" to step
                    )
                )
                return result.getOrThrow()
            }
        }
        error("Error processing retries, should never get here")
    }

    private fun needsRetry(step: Int, result: Result<T>): Boolean =
        (step < config.maxRetries) && errorDetection(result)

    private fun retryTime(step: Int): Long =
        config.retries[step]
}
