package io.github.andresviedma.poket.support

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import kotlinx.datetime.Clock

object SystemProvider {
    private var injectedClock: Clock? = null
    var overriddenClock: Clock? = null
    val clock: Clock get() = overriddenClock ?: injectedClock ?: Clock.System

    private var injectedMeterRegistry: MeterRegistry? = null
    var overriddenMeterRegistry: MeterRegistry? = null
    val meterRegistry: MeterRegistry get() = overriddenMeterRegistry ?: injectedMeterRegistry ?: CompositeMeterRegistry()

    fun init(clock: Clock?, meterRegistry: MeterRegistry?) {
        injectedClock = clock
        injectedMeterRegistry = meterRegistry
    }
}
