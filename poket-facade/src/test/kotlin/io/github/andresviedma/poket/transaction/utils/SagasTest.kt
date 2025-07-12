package io.github.andresviedma.poket.transaction.utils

import com.github.andresviedma.trekkie.When
import com.github.andresviedma.trekkie.then
import com.github.andresviedma.trekkie.thenExceptionThrown
import io.github.andresviedma.poket.testutils.EventSyncChannel
import io.github.andresviedma.poket.transaction.TransactionManager
import io.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.andresviedma.poket.transaction.suspendable.transactional
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job

class SagasTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val events = mutableListOf<String>()
    TransactionWrapper.overriddenTransactionManager = TransactionManager(SagaTransactionHandler())

    "successful transaction" {
        When {
            transactional {
                sagaOperation(undo = { events.add("undo") }) {
                    events.add("action")
                }
            }
        } then {
            events shouldBe listOf("action")
        }
    }

    "successful with no transaction" {
        When {
            sagaOperation(undo = { events.add("undo") }) {
                events.add("action")
            }
        } then {
            events shouldBe listOf("action")
        }
    }

    "error with no transaction" {
        When {
            sagaOperation(undo = { events.add("undo") }) {
                events.add("action")
            }
            error("boom")
        } thenExceptionThrown { _: IllegalStateException ->
            events shouldBe listOf("action")
        }
    }

    "transaction with error in saga operation" {
        When {
            transactional {
                sagaOperation(undo = { events.add("undo") }) {
                    events.add("action")
                    error("boom")
                }
            }
        } thenExceptionThrown { _: IllegalStateException ->
            events shouldBe listOf("action")
        }
    }

    "transaction with error after saga operation" {
        When {
            transactional {
                sagaOperation(undo = { events.add("undo") }) {
                    events.add("action")
                }
                error("boom")
            }
        } thenExceptionThrown { _: IllegalStateException ->
            events shouldBe listOf("action", "undo")
        }
    }

    "transaction with error after 2 saga operations" {
        When {
            transactional {
                sagaOperation(undo = { events.add("undo1") }) {
                    events.add("action1")
                }
                sagaOperation(undo = { events.add("undo2") }) {
                    events.add("action2")
                }
                sagaOperation(undo = { events.add("undo3") }) {
                    events.add("action3")
                    error("boom")
                }
            }
        } thenExceptionThrown { _: IllegalStateException ->
            events shouldBe listOf("action1", "action2", "action3", "undo2", "undo1")
        }
    }

    "transaction with cancelled coroutine after saga operation" {
        When {
            val channel = EventSyncChannel()
            coroutineScope {
                awaitAll(
                    async {
                        transactional {
                            sagaOperation(
                                undo = {
                                    // non cancellable = job appears as not completed
                                    if (!coroutineContext.job.isCompleted) {
                                        events.add("undo in NonCancellable")
                                    }
                                }
                            ) {
                                events.add("action")
                            }
                            channel.send("action-done")
                            delay(100)
                        }
                    },
                    async {
                        channel.waitFor("action-done")
                        error("boom")
                    }
                )
            }
        } thenExceptionThrown { _: IllegalStateException ->
            events shouldBe listOf("action", "undo in NonCancellable")
        }
    }
})
