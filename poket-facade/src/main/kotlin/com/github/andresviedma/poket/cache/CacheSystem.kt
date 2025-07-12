package com.github.andresviedma.poket.cache

import com.github.andresviedma.poket.config.ConfigProvider
import com.github.andresviedma.poket.support.serialization.PoketSerializer
import java.util.concurrent.ConcurrentHashMap

class CacheSystemProvider(
    private val registeredSystems: Set<CacheSystem>,
    private val cacheMetrics: CacheMetrics,
    private val configProvider: ConfigProvider
) {
    private val usedSystems = ConcurrentHashMap<String, CacheSystemWrapper>()

    constructor(cacheMetrics: CacheMetrics, configProvider: ConfigProvider, vararg systems: CacheSystem) :
        this(systems.toSet(), cacheMetrics, configProvider)

    internal fun getCacheSystem(
        cacheSystemId: String,
        type: String,
        customSerializer: PoketSerializer?,
        defaultTypeConfig: CacheTypeConfig?
    ): CacheSystem =
        usedSystems.getOrPut(type) {
            CacheSystemWrapper(
                getNotWrappedCacheSystem(cacheSystemId),
                type,
                cacheMetrics,
                configProvider,
                customSerializer,
                defaultTypeConfig
            )
        }

    internal fun getAllUsedCacheSystems(): Collection<CacheSystemWrapper> =
        usedSystems.values

    private fun getNotWrappedCacheSystem(cacheSystemId: String): CacheSystem =
        registeredSystems.firstOrNull { it.getId() == cacheSystemId }
            ?: error("CacheSystem unknown: $cacheSystemId")
}

interface CacheSystem {
    fun getId(): String

    suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: Class<V>): V?

    suspend fun <K : Any, V : Any> setObject(namespace: String, key: K, value: V, ttlSeconds: Long, forceInvalidation: Boolean)

    suspend fun <K : Any> invalidateObject(namespace: String, key: K)

    suspend fun <K : Any, V : Any> getObjectList(namespace: String, keys: List<K>, resultClass: Class<V>): Map<K, V>

    suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, V>, ttlSeconds: Long, forceInvalidation: Boolean)

    /**
     * This set map accepts ttlSeconds and forceInvalidation values for each map entry
     */
    suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>)

    suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>)
}
