package io.github.andresviedma.poket.transaction

/**
 * Properties of a transaction, declared when it is started.
 */
class TransactionMetadata(
    private val rollbackOn: Set<Class<out Throwable>> = emptySet(),
    private val dontRollbackOn: Set<Class<out Throwable>> = emptySet(),
    val isolationLevel: TransactionIsolationLevel? = null // use default
) {
    /**
     * Returns true if according to the metadata properties, the
     * transaction needs to be rolled back when the given exception has been thrown.
     */
    fun isRollbackException(e: Throwable): Boolean =
        (rollbackOn.isEmpty() || exceptionInList(e, rollbackOn)) &&
            !exceptionInList(e, dontRollbackOn)

    private fun exceptionInList(e: Throwable, classList: Set<Class<*>>): Boolean =
        classList.any { it.isInstance(e) }
}

enum class TransactionIsolationLevel {
    /** No DB locks done, a transaction will read changes done by other before committing them */
    READ_UNCOMMITTED,

    /** Any query uses committed data right before the execution of the query (non-repeatable read, same query can have different results) */
    READ_COMMITTED,

    /** Any query uses committed data, same data returned always through the transaction (repeatable) */
    REPEATABLE_READ,

    /** A row read by a transaction will be locked and cannot be updated by other */
    SERIALIZABLE
}
