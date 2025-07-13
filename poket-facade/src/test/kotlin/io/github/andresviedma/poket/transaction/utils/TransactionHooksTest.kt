package io.github.andresviedma.poket.transaction.utils

import com.github.andresviedma.trekkie.When
import com.github.andresviedma.trekkie.then
import com.github.andresviedma.trekkie.thenExceptionThrown
import io.github.andresviedma.poket.transaction.TestTransactionHandler
import io.github.andresviedma.poket.transaction.TransactionManager
import io.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.andresviedma.poket.transaction.suspendable.transactional
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class TransactionHooksTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val events = mutableListOf<String>()
    val handler1 = TestTransactionHandler(events, "1")
    TransactionWrapper.overriddenTransactionManager = TransactionManager.withHandlers(handler1)

    "out of a transaction" {
        When {
            runAfterTransactionCommit {
                events.add("after-commit")
            }
        } then {
            events shouldBe listOf(
                "after-commit"
            )
        }
    }

    "suspendable" {
        When {
            runAfterTransactionCommit {
                events.add("after-commit")
                delay(1)
            }
        } then {
            events shouldBe listOf(
                "after-commit"
            )
        }
    }

    "successful transaction" {
        When {
            transactional {
                runAfterTransactionCommit {
                    events.add("after-commit")
                }
                events.add("action")
                "OK"
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "action",
                "commit 1 d1",
                "after-commit"
            )
        }
    }

    "successful nested transaction" {
        When {
            transactional {
                runAfterTransactionCommit {
                    events.add("after-commit 1")
                }
                transactional {
                    runAfterTransactionCommit {
                        events.add("after-commit 2")
                    }
                    events.add("action")
                    "OK"
                }
            }
        } then {
            it shouldBe "OK"
            events shouldBe listOf(
                "start 1",
                "action",
                "commit 1 d1",
                "after-commit 1",
                "after-commit 2"
            )
        }
    }

    "successful transaction with failed post-action" {
        When {
            transactional {
                runAfterTransactionCommit {
                    events.add("after-commit")
                }
                events.add("action")
                runAfterTransactionCommit {
                    error("boom")
                }
                "OK"
            }
        } thenExceptionThrown { exception: IllegalStateException ->
            exception.message shouldBe "boom"
            events shouldBe listOf(
                "start 1",
                "action",
                "commit 1 d1",
                "after-commit"
            )
        }
    }

    "rolled back transaction" {
        When {
            transactional {
                transactional {
                    runAfterTransactionCommit {
                        events.add("after-commit")
                    }
                    events.add("action")
                    error("boom")
                }
            }
        } thenExceptionThrown { _: Exception ->
            events shouldBe listOf(
                "start 1",
                "action",
                "rollback 1 d1"
            )
        }
    }
})
