package io.github.andresviedma.poket.mutex.local

import io.github.andresviedma.poket.config.utils.configWith
import io.github.andresviedma.poket.mutex.*
import kotlin.time.Duration

fun distributedMutexFactoryStub(lockSystem: LockSystem = lockSystemStub()) = DistributedMutexFactory(
    LockSystemProvider.withLockSystems(lockSystem),
    configWith(
        MutexConfig(default = MutexTypeConfig(lockSystem = lockSystem.getId()))
    )
)

fun lockSystemStub(): LockSystemStub = LockSystemStub()

@Suppress("unused")
class LockSystemStub : LockSystem {
    private val willGetLock: MutableList<Boolean> = mutableListOf()

    fun willGetLock() {
        willGetLock.clear().also { willGetLock.add(true) }
    }

    fun willNotGetLock() {
        willGetLock.clear().also { willGetLock.add(false) }
    }

    fun willGetLockSequence(vararg getLock: Boolean) {
        willGetLock.clear().also { willGetLock += getLock.toList() }
    }

    override fun getId(): String =
        "stub"

    override suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext =
        LockContext(pullGetLock())

    override suspend fun getLockIfFree(name: String, ttl: Duration): LockContext =
        LockContext(pullGetLock())

    override suspend fun releaseLock(name: String, lockContext: LockContext): Boolean =
        true

    private fun pullGetLock(): Boolean = when (willGetLock.size) {
        0 -> true
        1 -> willGetLock.first()
        else -> willGetLock.removeFirst()
    }
}
