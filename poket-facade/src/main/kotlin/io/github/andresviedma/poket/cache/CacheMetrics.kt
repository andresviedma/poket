package io.github.andresviedma.poket.cache

import io.github.andresviedma.poket.support.metrics.recordTimer
import io.github.andresviedma.poket.support.metrics.timer
import io.micrometer.core.instrument.MeterRegistry
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class CacheMetrics(
    private val meterRegistry: MeterRegistry
) {
    suspend fun <X> recordTimer(
        timer: String,
        cacheSystem: String,
        namespace: String,
        recordHitMiss: Boolean = false,
        blockSize: Int? = null,
        block: suspend () -> X
    ): X =
        meterRegistry.recordTimer(
            "cache.$timer",
            mapOf(
                "type" to namespace,
                "cacheSystem" to cacheSystem,
                "blockSize" to blockSize
            ),
            tagFromResult = { result -> "result" to if (recordHitMiss) cacheHitMiss(result) else null }
        ) {
            block()
        }.also {
            recordBlockSizeMetric(timer, cacheSystem, namespace, blockSize)
        }

    private fun recordBlockSizeMetric(
        timer: String,
        cacheSystem: String,
        namespace: String,
        blockSize: Int?
    ) {
        if (blockSize != null) {
            meterRegistry.timer(
                "cache.${timer}Size",
                mapOf(
                    "type" to namespace,
                    "cacheSystem" to cacheSystem
                )
            ).record(blockSize.seconds.toJavaDuration())
        }
    }

    private fun <X> cacheHitMiss(value: X) =
        if (value == null) "miss" else "hit"
}
