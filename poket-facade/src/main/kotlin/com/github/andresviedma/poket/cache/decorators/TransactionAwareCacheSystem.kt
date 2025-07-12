package com.github.andresviedma.poket.cache.decorators

import com.github.andresviedma.poket.cache.CacheSystem
import com.github.andresviedma.poket.transaction.TransactionWrapper
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class TransactionAwareCacheSystem(
    private val target: CacheSystem,
    private val type: String
) : CacheSystem by target {

    override suspend fun <K : Any, V : Any> setObject(
        namespace: String,
        key: K,
        value: V,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) {
        if (!TransactionWrapper.inTransaction()) {
            effectiveSetObject(namespace, key, value, ttlSeconds, forceInvalidation)
        } else {
            val pendingOperations: PendingCacheOperations? = TransactionWrapper.currentTransactionData()
            pendingOperations?.addCacheSet(type, namespace, key, value, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any> invalidateObject(namespace: String, key: K) {
        if (!TransactionWrapper.inTransaction()) {
            effectiveInvalidate(namespace, key)
        } else {
            val pendingOperations: PendingCacheOperations? = TransactionWrapper.currentTransactionData()
            pendingOperations?.addInvalidation(type, namespace, key)
        }
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean) {
        if (!TransactionWrapper.inTransaction()) {
            effectiveSetObjectList(namespace, values, ttlSeconds, forceInvalidation)
        } else {
            val pendingOperations: PendingCacheOperations? = TransactionWrapper.currentTransactionData()
            values.forEach { pendingOperations?.addCacheSet(type, namespace, it.key, it.value, ttlSeconds, forceInvalidation) }
        }
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        if (!TransactionWrapper.inTransaction()) {
            effectiveSetObjectList(namespace, values)
        } else {
            val pendingOperations: PendingCacheOperations? = TransactionWrapper.currentTransactionData()
            values.forEach { pendingOperations?.addCacheSet(type, namespace, it.key, it.value.first, it.value.second, it.value.third) }
        }
    }

    override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) {
        if (!TransactionWrapper.inTransaction()) {
            effectiveInvalidateList(namespace, keys)
        } else {
            val pendingOperations: PendingCacheOperations? = TransactionWrapper.currentTransactionData()
            keys.forEach { pendingOperations?.addInvalidation(type, namespace, it) }
        }
    }

    /**
     * Execute the pending transaction operations on commit. If some of them fail, the error is logged
     * but the execution continues.
     */
    internal suspend fun executeTransactionOperations(transactionPending: PendingCacheOperations) {
        val namespacesOperations = mutableMapOf<String, OperationsBuffer>()
        transactionPending.pendingOperations
            .filter { it.first.cacheType == type }
            .forEach { (key, operation) ->
                val namespaceOperations = namespacesOperations.getOrPut(key.namespace) { OperationsBuffer() }
                if (operation == null) {
                    namespaceOperations.toInvalidateKeys.add(key.key)
                } else {
                    namespaceOperations.toSetObjects[key.key] = operation
                }
                transactionPending.deleteOperation(key)
            }
        namespacesOperations.forEach {
            executeNamespaceTransactionOperations(it.key, it.value)
        }
    }

    private suspend fun effectiveSetObject(namespace: String, key: Any, value: Any, ttlSeconds: Long, forceInvalidation: Boolean) {
        target.setObject(namespace, key, value, ttlSeconds, forceInvalidation)
    }

    private suspend fun effectiveInvalidate(namespace: String, key: Any) {
        target.invalidateObject(namespace, key)
    }

    private suspend fun <K : Any, V : Any> effectiveSetObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean) {
        target.setObjectList(namespace, values, ttlSeconds, forceInvalidation)
    }

    private suspend fun <K : Any, V : Any> effectiveSetObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        target.setObjectList(namespace, values)
    }

    private suspend fun effectiveInvalidateList(namespace: String, keys: List<Any>) {
        target.invalidateObjectList(namespace, keys)
    }

    private suspend fun executeNamespaceTransactionOperations(namespace: String, operations: OperationsBuffer) {
        try {
            if (operations.toInvalidateKeys.isNotEmpty()) {
                effectiveInvalidateList(namespace, operations.toInvalidateKeys)
            }
        } catch (exception: Throwable) {
            logger.warn { "Cache $type: Error trying to invalidate objects in transaction - ${exception.message}" }
        }
        try {
            if (operations.toSetObjects.isNotEmpty()) {
                effectiveSetObjectList(namespace, operations.toSetObjects.mapValues { Triple(it.value.value, it.value.ttlSeconds, it.value.forceInvalidation) })
            }
        } catch (exception: Throwable) {
            logger.warn { "Cache $type: Error trying to set values in transaction - ${exception.message}" }
        }
    }
}

internal data class OperationsBuffer(
    val toInvalidateKeys: MutableList<Any> = mutableListOf(),
    val toSetObjects: MutableMap<Any, PendingCacheOperation> = mutableMapOf()
)
