package io.github.andresviedma.poket.mutex

import io.github.andresviedma.poket.config.ConfigProvider

@Suppress("unused")
class DistributedMutexFactory(
    private val lockSystemProvider: LockSystemProvider,
    private val configProvider: ConfigProvider
) {
    fun createMutex(type: String, baseTypeConfig: MutexTypeConfig? = null) =
        DistributedMutex(lockSystemProvider, configProvider, type, baseTypeConfig = baseTypeConfig)

    fun createMutex(type: String, forceIgnoreLockErrors: Boolean, baseTypeConfig: MutexTypeConfig? = null) =
        DistributedMutex(lockSystemProvider, configProvider, type, forceIgnoreLockErrors, baseTypeConfig)

    fun createBlockingMutex(type: String, baseTypeConfig: MutexTypeConfig? = null) =
        DistributedBlockingMutex(DistributedMutex(lockSystemProvider, configProvider, type, baseTypeConfig = baseTypeConfig))

    fun createBlockingMutex(type: String, forceIgnoreLockErrors: Boolean, baseTypeConfig: MutexTypeConfig? = null) =
        DistributedBlockingMutex(DistributedMutex(lockSystemProvider, configProvider, type, forceIgnoreLockErrors, baseTypeConfig))
}
