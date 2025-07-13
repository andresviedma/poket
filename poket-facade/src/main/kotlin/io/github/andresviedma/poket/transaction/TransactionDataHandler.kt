package io.github.andresviedma.poket.transaction

/**
 * Interface for pluggable transaction data handlers, suited to specific storages.
 * These handlers will have a context associated to a started transaction
 * and have the possibility of coordinating the commit or rollback of
 * any type of data.
 */
interface TransactionDataHandler {
    /**
     * Prepares the context for the given storage when a transaction is started.
     * No connection should really be established at this point, as this will be called
     * for all the data handlers but this storage might not be effectively used in
     * the transaction.
     *
     * @param metadata The metadata of the transaction just started.
     * @returns Any transaction data that can be useful to make the commit / rollback.
     */
    suspend fun startTransaction(metadata: TransactionMetadata): Any? {
        return null
    }

    /**
     * Commits the current transaction. Beware that as different sources are not really going to be completely
     * transactional, a rollback could be invoked after the commit, in case other handler fails. So only one
     * "main" storage should be effectively used in a transaction.
     *
     * @param transactionData Any transaction context data returned by the handler when it was started.
     */
    suspend fun commitTransaction(transactionData: Any?) {}

    /**
     * Rollbacks the current transaction. As different sources are not really going to be completely
     * transactional, this method might be called after a commit.
     *
     * @param transactionData Any transaction context data returned by the handler when it was started.
     */
    suspend fun rollbackTransaction(transactionData: Any?) {}

    /**
     * If true, the will be considered for "main" storage and committed first.
     */
    fun isPrimaryStorage(): Boolean
}
