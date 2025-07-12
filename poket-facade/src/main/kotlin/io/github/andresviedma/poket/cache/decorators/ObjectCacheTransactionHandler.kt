package io.github.andresviedma.poket.cache.decorators

import io.github.andresviedma.poket.cache.CacheSystemProvider
import io.github.andresviedma.poket.cache.CacheSystemWrapper
import io.github.andresviedma.poket.transaction.TransactionDataHandler
import io.github.andresviedma.poket.transaction.TransactionMetadata


internal class ObjectCacheTransactionHandler(
    private val cacheSystemProvider: CacheSystemProvider
) : TransactionDataHandler {
    override suspend fun startTransaction(metadata: TransactionMetadata): Any =
        PendingCacheOperations()

    override suspend fun commitTransaction(transactionData: Any?) {
        if (transactionData != null && transactionData is PendingCacheOperations) {
            cacheSystemProvider.getAllUsedCacheSystems().forEach {
                commitCacheSystem(it, transactionData)
            }
        }
    }

    private suspend fun commitCacheSystem(cacheSystem: CacheSystemWrapper, transactionData: PendingCacheOperations) {
        runCatching {
            cacheSystem.executeTransactionOperations(transactionData)
        }
    }

    override suspend fun rollbackTransaction(transactionData: Any?) {
    }
}

internal data class PendingCacheOperations(
    private val operations: MutableMap<PendingCacheOperationKey, PendingCacheOperation?> = mutableMapOf()
) {
    val pendingOperations: List<Pair<PendingCacheOperationKey, PendingCacheOperation?>> get() =
        operations.entries.map { it.key to it.value }

    fun addCacheSet(systemId: String, namespace: String, key: Any, value: Any, ttl: Long, forceInvalidation: Boolean) =
        addCacheSet(
            PendingCacheOperationKey(cacheType = systemId, namespace = namespace, key = key),
            PendingCacheOperation(value = value, ttlSeconds = ttl, forceInvalidation = forceInvalidation)
        )

    private fun addCacheSet(key: PendingCacheOperationKey, operation: PendingCacheOperation) {
        deleteOperation(key)
        operations[key] = operation
    }

    fun addInvalidation(systemId: String, namespace: String, key: Any) =
        addInvalidation(PendingCacheOperationKey(cacheType = systemId, namespace = namespace, key = key))

    private fun addInvalidation(key: PendingCacheOperationKey) {
        deleteOperation(key)
        operations[key] = null
    }

    fun deleteOperation(key: PendingCacheOperationKey) {
        operations.remove(key)
    }
}

internal data class PendingCacheOperationKey(
    val cacheType: String,
    val namespace: String,
    val key: Any
)

internal data class PendingCacheOperation(
    val value: Any,
    val ttlSeconds: Long,
    val forceInvalidation: Boolean
)
