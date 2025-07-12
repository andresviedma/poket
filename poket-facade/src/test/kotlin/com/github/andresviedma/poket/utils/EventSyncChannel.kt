package com.github.andresviedma.poket.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Class that allows concurrency tests where some coroutine will get on hold, expecting an event generated from other
 * in order to continue the execution.
 */
class EventSyncChannel(
    private val timeoutMillis: Long = 1000,
    capacity: Int = 5
) {
    private val channel = Channel<String>(capacity)

    suspend fun waitFor(event: String) {
        coroutineScope {
            val timeoutJob = launch {
                delay(timeoutMillis)
                error("Timeout expired while waiting for a message in sync bus, expecting: $event")
            }

            val receivedEvent = channel.receive()
            check(event == receivedEvent) { "Wrong value received, expected: $event received: $receivedEvent" }
            timeoutJob.cancel()
        }
    }

    suspend fun sendAndWaitFor(sendEvent: String, waitForEvent: String) {
        channel.send(sendEvent)
        delay(5)
        waitFor(waitForEvent)
    }

    suspend fun send(event: String) {
        channel.send(event)
    }
}
