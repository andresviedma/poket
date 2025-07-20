package io.github.andresviedma.poket.cache.decorators

import io.github.andresviedma.poket.cache.CacheConfig
import io.github.andresviedma.poket.cache.CacheSystem
import io.github.andresviedma.poket.config.Config
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class ErrorIgnoreCacheSystem(
    private val target: CacheSystem,
    private val type: String,
    configProvider: ConfigProvider,
    private val defaultConfig: io.github.andresviedma.poket.cache.CacheTypeConfig?

) : CacheSystem by target {
    private val cacheConfig: Config<CacheConfig> = configProvider.getTypedConfig()

    override suspend fun <K : Any, V : Any> getObject(namespace: String, key: K, resultClass: Class<V>): V? =
        runOrFail(getConfig().failOnGetError) {
            target.getObject(namespace, key, resultClass)
        }

    override suspend fun <K : Any, V : Any> setObject(
        namespace: String,
        key: K,
        value: V,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) {
        runOrFail(
            exceptionOnError = getConfig().failOnPutError,
            onIgnoredError = {
                if (forceInvalidation) {
                    val currentVal = getObject(namespace, key, value.javaClass)
                    if (currentVal != null && currentVal != value) invalidateObject(namespace, key)
                }
            }
        ) {
            target.setObject(namespace, key, value, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any> invalidateObject(namespace: String, key: K) {
        runOrFail(getConfig().failOnInvalidateError) {
            target.invalidateObject(namespace, key)
        }
    }

    override suspend fun <K : Any, V : Any> getObjectList(
        namespace: String,
        keys: List<K>,
        resultClass: Class<V>
    ): Map<K, V> =
        runOrFail(getConfig().failOnGetError) {
            target.getObjectList(namespace, keys, resultClass)
        } ?: emptyMap()

    override suspend fun <K : Any, V : Any> setObjectList(
        namespace: String,
        values: Map<K, V>,
        ttlSeconds: Long,
        forceInvalidation: Boolean
    ) {
        runOrFail(
            getConfig().failOnPutError,
            onIgnoredError = {
                if (forceInvalidation) {
                    // Invalidate in case of error, as this will work in case of full cache
                    val keys = values.keys.toList()
                    val currentVals = getObjectList(namespace, keys, values.values.first().javaClass)
                    if (currentVals.isNotEmpty() && values != currentVals) invalidateObjectList(namespace, keys)
                }
            }
        ) {
            target.setObjectList(namespace, values, ttlSeconds, forceInvalidation)
        }
    }

    override suspend fun <K : Any, V : Any> setObjectList(namespace: String, values: Map<K, Triple<V, Long, Boolean>>) {
        runOrFail(
            getConfig().failOnPutError,
            onIgnoredError = {
                if (values.anyValueForcesInvalidation()) {
                    // Invalidate in case of error, as this will work in case of full cache
                    val keys = values.keys.toList()
                    val currentVals = getObjectList(namespace, keys, values.values.first().javaClass)
                    if (currentVals.isNotEmpty() && values != currentVals) invalidateObjectList(namespace, keys)
                }
            }
        ) {
            target.setObjectList(namespace, values)
        }
    }

    private fun <K : Any, V : Any> Map<K, Triple<V, Long, Boolean>>.anyValueForcesInvalidation() =
        values.any { it.third }

    override suspend fun <K : Any> invalidateObjectList(namespace: String, keys: List<K>) {
        runOrFail(getConfig().failOnInvalidateError) {
            target.invalidateObjectList(namespace, keys)
        }
    }

    private suspend fun <T> runOrFail(
        exceptionOnError: Boolean?,
        onIgnoredError: suspend () -> Unit = {},
        block: suspend () -> T?
    ): T? =
        try {
            block()
        } catch (exception: IllegalStateException) {
            throw exception // illegal state is due to misconfiguration, so it is always thrown
        } catch (exception: Throwable) {
            if (exceptionOnError == true) {
                throw exception
            } else {
                logger.warn(exception) { "Ignored error accessing cache $type: ${exception.message}" }
                onIgnoredError()
                null
            }
        }

    private fun getConfig(): io.github.andresviedma.poket.cache.CacheTypeConfig =
        cacheConfig.get().getTypeConfig(type, defaultConfig)
}
