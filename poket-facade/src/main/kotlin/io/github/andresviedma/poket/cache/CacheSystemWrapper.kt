package io.github.andresviedma.poket.cache

import io.github.andresviedma.poket.cache.decorators.*
import io.github.andresviedma.poket.cache.decorators.ErrorIgnoreCacheSystem
import io.github.andresviedma.poket.cache.decorators.MetricsCacheSystem
import io.github.andresviedma.poket.cache.decorators.PendingCacheOperations
import io.github.andresviedma.poket.cache.decorators.TransactionAwareCacheSystem
import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.support.serialization.PoketSerializer

open class CacheSystemDecorator(
    protected val target: CacheSystem
) : CacheSystem by target

/**
 * Wrapper on other cache system adding transaction handling, configurable error ignore and metrics.
 * The wrapper will be added automatically to all registered cache systems in
 */
internal class CacheSystemWrapper(
    target: CacheSystem,
    type: String,
    metrics: CacheMetrics,
    configProvider: ConfigProvider,
    customSerializer: PoketSerializer?,
    defaultTypeConfig: CacheTypeConfig?

) : CacheSystemDecorator(
    TransactionAwareCacheSystem(
        ErrorIgnoreCacheSystem(
            MetricsCacheSystem(
                CacheCustomSerializerSystem(target, customSerializer),
                metrics
            ),
            type,
            configProvider,
            defaultTypeConfig
        ),
        type
    )
) {
    /**
     * Execute the pending transaction operations on commit. If some of them fail, the error is logged
     * but the execution continues.
     */
    internal suspend fun executeTransactionOperations(transactionPending: PendingCacheOperations) {
        (target as TransactionAwareCacheSystem).executeTransactionOperations(transactionPending)
    }
}
