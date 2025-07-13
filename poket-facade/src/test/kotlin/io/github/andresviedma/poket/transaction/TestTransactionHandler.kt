package io.github.andresviedma.poket.transaction

import kotlinx.coroutines.delay

class TestTransactionHandler(
    private val events: MutableList<String>,
    private val id: String
) : TransactionDataHandler {
    private var failOnCommit = false
    private var failOnRollback = false

    fun failsOnCommit() {
        failOnCommit = true
    }
    fun failsOnRollback() {
        failOnRollback = true
    }

    override suspend fun startTransaction(metadata: TransactionMetadata): Any {
        events.add("start $id" + (metadata.isolationLevel?.let { " $it" } ?: ""))
        return "d$id"
    }

    override suspend fun commitTransaction(transactionData: Any?) {
        delay(1) // Delay to make sure commit is run even if the job has been cancelled
        events.add("commit $id $transactionData")
        if (failOnCommit) {
            error("fail $id $transactionData")
        } else {
            super.commitTransaction(transactionData)
        }
    }

    override suspend fun rollbackTransaction(transactionData: Any?) {
        delay(1) // Delay to make sure rollback is run even if the job has been cancelled
        events.add("rollback $id $transactionData")
        if (failOnRollback) {
            error("fail $id $transactionData")
        } else {
            super.rollbackTransaction(transactionData)
        }
    }

    override fun isPrimaryStorage(): Boolean = true
}
