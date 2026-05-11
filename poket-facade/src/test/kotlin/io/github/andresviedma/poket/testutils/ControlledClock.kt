package io.github.andresviedma.poket.testutils

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class ControlledClock(initialTime: Instant = Clock.System.now()) : Clock {
    private var time: Instant = initialTime

    fun hasTime(time: Instant) { this.time = time }
    fun fastForward(duration: Duration) { this.time += duration }
    fun rewind(duration: Duration) { this.time -= duration }

    override fun now(): Instant = time
}
