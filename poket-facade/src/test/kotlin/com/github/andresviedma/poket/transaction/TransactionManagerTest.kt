package com.github.andresviedma.poket.transaction

import com.github.andresviedma.poket.transaction.TransactionWrapper.blockingTransactional
import com.github.andresviedma.poket.transaction.TransactionWrapper.newTransactionCoroutineContext
import com.github.andresviedma.poket.transaction.TransactionWrapper.transactionCoroutineContext
import com.github.andresviedma.poket.transaction.suspendable.transactional
import com.github.andresviedma.poket.testutils.EventSyncChannel
import com.github.andresviedma.trekkie.Given
import com.github.andresviedma.trekkie.When
import com.github.andresviedma.trekkie.then
import com.github.andresviedma.trekkie.thenExceptionThrown
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*

class TransactionManagerTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val events = mutableListOf<String>()
    val handler1 = TestTransactionHandler(events, "1")
    val handler2 = TestTransactionHandler(events, "2")

    TransactionWrapper.overriddenTransactionManager = TransactionManager(handler1, handler2)

    "successful transaction" {
        When {
            transactional {
                events.add("action")
                "OK"
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }


    "successful nested transaction" {
        When {
            transactional {
                events.add("action pre")
                val result = transactional {
                    events.add("action")
                    "OK"
                }
                events.add("action post")
                result
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action pre",
                "action",
                "action post",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }

    "successful nested transaction in non-coroutines code" {
        When {
            blockingTransactional {
                events.add("action pre")
                val result = blockingTransactional {
                    events.add("action")
                    "OK"
                }
                events.add("action post")
                result
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action pre",
                "action",
                "action post",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }

    "transaction with body error" {
        When {
            transactional {
                error("boom")
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "boom"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "rollback 2 d2",
                "rollback 1 d1"
            )
        }
    }

    "nested transaction with body error" {
        When {
            transactional {
                events.add("action pre")
                transactional {
                    error("boom")
                }
                @Suppress("UNREACHABLE_CODE")
                events.add("action post")
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "boom"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action pre",
                "rollback 2 d2",
                "rollback 1 d1"
            )
        }
    }

    "transaction with exception in commit" {
        Given(handler2) {
            failsOnCommit()
        }
        When {
            transactional {
                events.add("action")
                "OK"
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "fail 2 d2"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action",
                "commit 2 d2",
                "rollback 2 d2",
                "rollback 1 d1"
            )
        }
    }

    "transaction with exception in second commit" {
        Given(handler1) {
            failsOnCommit()
        }
        When {
            transactional {
                events.add("action")
                "OK"
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "fail 1 d1"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action",
                "commit 2 d2", // This commit will be performed, so the order of handlers matters
                "commit 1 d1",
                "rollback 2 d2", // This rollback in general is not expected to be effective
                "rollback 1 d1"
            )
        }
    }

    "transaction with exception in rollback" {
        Given(handler2) {
            failsOnRollback()
        }
        When {
            transactional {
                error("boom")
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "boom"
            exception.suppressedExceptions.size shouldBe 0
            events shouldBe listOf(
                "start 1",
                "start 2",
                "rollback 2 d2",
                "rollback 1 d1"
            )
        }
    }

    "successful nested transaction with coroutines" {
        When {
            withContext(transactionCoroutineContext()) {
                transactional {
                    events.add("action pre")
                    coroutineScope {
                        awaitAll(
                            async {
                                transactional {
                                    events.add("action")
                                }
                            },
                            async {
                                transactional {
                                    events.add("action")
                                }
                            }
                        )
                    }
                    events.add("action post")
                    "OK"
                }
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action pre",
                "action",
                "action",
                "action post",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }

    "successful nested transaction with coroutines and an independent async context" {
        When {
            withContext(transactionCoroutineContext()) {
                transactional {
                    events.add("action pre")
                    coroutineScope {
                        awaitAll(
                            async(newTransactionCoroutineContext()) {
                                transactional {
                                    events.add("action")
                                }
                            }
                        )
                    }
                    events.add("action post")
                    "OK"
                }
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action pre",
                "start 1",
                "start 2",
                "action",
                "commit 2 d2",
                "commit 1 d1",
                "action post",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }

    "successful independent async transactions with coroutines" {
        When {
            val eventSync = EventSyncChannel()
            coroutineScope {
                awaitAll(
                    async {
                        transactional {
                            events.add("action")
                            eventSync.sendAndWaitFor("action1-done", "action2-committed")
                        }
                    },
                    async {
                        eventSync.waitFor("action1-done")
                        transactional {
                            events.add("action")
                        }
                        eventSync.send("action2-committed")
                    }
                )
            }
            "OK"
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action",
                "start 1",
                "start 2",
                "action",
                "commit 2 d2",
                "commit 1 d1",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }

    "independent async transactions with coroutines with rollback and cancellation" {
        When {
            coroutineScope {
                awaitAll(
                    async {
                        transactional {
                            events.add("action")
                            delay(200) // Should be rolled back because the job is cancelled by the other async job
                        }
                    },
                    async {
                        delay(5)
                        transactional {
                            events.add("error")
                            error("boom")
                        }
                    }
                )
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "boom"
            events shouldBe listOf(
                "start 1", "start 2", "action",
                "start 1", "start 2", "error",
                "rollback 2 d2", "rollback 1 d1", "rollback 2 d2", "rollback 1 d1"
            )
        }
    }

    "new coroutine scope starts independent transaction" {
        When {
            withContext(transactionCoroutineContext()) {
                transactional {
                    events.add("action pre")
                    CoroutineScope(Dispatchers.Default).launch {
                        transactional {
                            events.add("action")
                        }
                    }.join()
                    events.add("action post")
                    "OK"
                }
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "start 2",
                "action pre",
                "start 1",
                "start 2",
                "action",
                "commit 2 d2",
                "commit 1 d1",
                "action post",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }

    "transaction with a specified isolation level" {
        When {
            transactional(isolationLevel = TransactionIsolationLevel.READ_UNCOMMITTED) {
                events.add("action pre")
                val result = transactional {
                    events.add("action")
                    "OK"
                }
                events.add("action post")
                result
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1 READ_UNCOMMITTED",
                "start 2 READ_UNCOMMITTED",
                "action pre",
                "action",
                "action post",
                "commit 2 d2",
                "commit 1 d1"
            )
        }
    }
})
