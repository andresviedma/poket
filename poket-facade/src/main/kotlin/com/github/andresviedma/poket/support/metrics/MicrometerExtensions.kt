package com.github.andresviedma.poket.support.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.datetime.Clock
import kotlin.time.toJavaDuration

/**
 * Records a timer. Compatible with suspend functions (because it's inline).
 */
inline fun <X> MeterRegistry.recordTimer(
    timer: String,
    tags: Map<String, Any?> = emptyMap(),
    noinline tagFromResult: ((X) -> Pair<String, Any?>)? = null,
    block: () -> X
): X {
    val t0 = Clock.System.now()
    return block()
        .also { result ->
            val resultTag = tagFromResult?.let { tagFromResult(result) } ?: ("" to null)
            timer(timer, tags + resultTag)
                .record((Clock.System.now() - t0).toJavaDuration())
        }
}

fun MeterRegistry.timer(timer: String, tags: Map<String, Any?> = emptyMap()): Timer =
    Timer.builder(timer)
        .also { micrometerTimer ->
            tags.forEach { (tag, value) -> if (value != null) micrometerTimer.tags(tag, value.toString()) }
        }
        .publishPercentileHistogram()
        .register(this)

fun MeterRegistry.incrementCounter(counter: String, tags: Map<String, Any?> = emptyMap(), increment: Number = 1) {
    counter(counter, tags).increment(increment.toDouble())
}

fun MeterRegistry.counter(counter: String, tags: Map<String, Any?> = emptyMap()): Counter =
    Counter.builder(counter)
        .also { micrometerCounter ->
            tags.forEach { (tag, value) -> if (value != null) micrometerCounter.tags(tag, value.toString()) }
        }
        .register(this)
