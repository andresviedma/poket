package io.github.andresviedma.poket.testutils

import io.micrometer.core.instrument.Statistic
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

/* Functions to make easier verifying the generation of metrics in unit tests */

fun testMicrometerRegistry() = SimpleMeterRegistry()

fun SimpleMeterRegistry.generatedMetrics(): Set<Metric> =
    meters.map { meter ->
        val measurements = meter.measure()
        val count = measurements.find { it.statistic == Statistic.COUNT }?.value?.toInt() ?: 0
        val isTimer = measurements.find { it.statistic == Statistic.TOTAL_TIME } != null
        Metric(meter.id.name, meter.id.tags.associate { it.key to it.value }, count, timer = isTimer)
    }.toSet()

data class Metric(
    val name: String,
    val tags: Map<String, String>,
    val count: Int = 1,
    val timer: Boolean = false
)
