package com.github.andresviedma.poket.mutex

import com.github.andresviedma.poket.config.ConfigProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Duration

private val log = KotlinLogging.logger {}

class DistributedMutexFactory constructor(
    private val lockSystemProvider: LockSystemProvider,
    private val configProvider: ConfigProvider
) {
    fun createMutex(type: String, baseTypeConfig: MutexTypeConfig? = null) =
        DistributedMutex(lockSystemProvider, configProvider, type, baseTypeConfig = baseTypeConfig)

    fun createMutex(type: String, forceIgnoreLockErrors: Boolean, baseTypeConfig: MutexTypeConfig? = null) =
        DistributedMutex(lockSystemProvider, configProvider, type, forceIgnoreLockErrors, baseTypeConfig)
}

class DistributedMutex(
    private val lockSystemProvider: LockSystemProvider,
    private val configProvider: ConfigProvider,
    private val type: String,
    private val forceIgnoreLockErrors: Boolean = false,
    private val baseTypeConfig: MutexTypeConfig? = null
) {
    suspend fun <T> synchronized(vararg keys: Any, mutexBlock: suspend () -> T): T {
        val config = getMutexConfig().getTypeConfig(type, baseTypeConfig)
        val lockSystem = lockSystemProvider.getLockSystem(config.lockSystem!!)
        val name = lockName(keys)
        val ctx = getLockHandlingErrors(config, lockSystem) {
            waitLock(
                name,
                Duration.ofMillis(config.timeoutInMillis!!.toLong()),
                Duration.ofMillis(config.ttlInMillis!!.toLong())
            )
        }
        if (!ctx.hasLock) {
            throw LockWaitTimedOutException("Lock $name wait timed out")
        }
        try {
            return mutexBlock()
        } finally {
            ctx.releaseLock(name, config)
        }
    }

    suspend fun <T> maybeSynchronized(vararg keys: Any, mutexBlock: suspend (Boolean) -> T): T {
        val config = getMutexConfig().getTypeConfig(type, baseTypeConfig)
        val lockSystem = lockSystemProvider.getLockSystem(config.lockSystem!!)
        val name = lockName(keys)
        val ctx = getLockHandlingErrors(config, lockSystem) {
            getLockIfFree(name, Duration.ofMillis(config.ttlInMillis!!.toLong()))
        }
        try {
            return mutexBlock(ctx.hasLock)
        } finally {
            ctx.releaseLock(name, config)
        }
    }

    suspend fun <T> ifSynchronized(vararg keys: Any, mutexBlock: suspend () -> T): T? =
        maybeSynchronized(*keys) { gotLock ->
            if (gotLock) {
                mutexBlock()
            } else {
                null
            }
        }

    private fun lockName(keys: Array<out Any>): String =
        (listOf(type) + keys).toList().joinToString("::")

    private suspend fun getMutexConfig(): MutexConfig =
        configProvider.get()

    private suspend fun LockContext.releaseLock(name: String, config: MutexTypeConfig) {
        if (hasLock && lockSystemUsed != null) {
            val ctx = this
            withContext(NonCancellable) {
                try {
                    lockSystemUsed.releaseLock(name, ctx)
                } catch (exception: Throwable) {
                    if (!forceIgnoreLockErrors && config.failOnLockReleaseError == true) {
                        throw exception
                    } else {
                        log.warn {
                            "Error releasing lock type $type key $name (will continue execution): " +
                                    "${exception.javaClass.simpleName}: ${exception.message}"
                        }
                    }
                }
            }
        }
    }

    private suspend fun getLockHandlingErrors(
        config: MutexTypeConfig,
        lockSystem: LockSystem,
        getLockOperation: suspend LockSystem.() -> LockContext
    ): LockContext =
        try {
            lockSystem.getLockOperation()
                .copy(lockSystemUsed = lockSystem)
        } catch (exception: Throwable) {
            val errorAction = if (forceIgnoreLockErrors) MutexOnErrorAction.GET else config.onLockSystemError
            when (errorAction) {
                null -> throw exception
                MutexOnErrorAction.FAIL -> throw exception
                MutexOnErrorAction.GET -> {
                    log.warn {
                        "Error getting lock type $type (will continue execution): " +
                                "${exception.javaClass.simpleName}: ${exception.message}"
                    }
                    LockContext(hasLock = true, lockSystemUsed = null)
                }
                MutexOnErrorAction.FALLBACK -> {
                    log.warn {
                        "Error getting lock type $type with system ${lockSystem.getId()} (will try fallback): " +
                                "${exception.javaClass.simpleName}: ${exception.message}"
                    }
                    if (config.fallbackLockSystem != null && config.fallbackLockSystem != lockSystem.getId()) {
                        val fallbackSystem = lockSystemProvider.getLockSystem(config.fallbackLockSystem)
                        getLockHandlingErrors(config, fallbackSystem, getLockOperation)
                    } else {
                        throw exception
                    }
                }
            }
        }
}

data class LockContext(
    var hasLock: Boolean,
    val lockContext: Any? = null,
    val lockSystemUsed: LockSystem? = null
)

class LockWaitTimedOutException(
    message: String? = "Lock wait timed out",
    cause: Throwable? = null
) : RuntimeException(message, cause)
