package com.github.andresviedma.poket.mutex.local

import com.github.andresviedma.poket.config.configWith
import com.github.andresviedma.poket.mutex.*
import java.time.Duration
import java.util.LinkedList

fun distributedMutexFactoryStub(lockSystem: LockSystem = lockSystemStub()) = DistributedMutexFactory(
    LockSystemProvider(lazyOf(setOf(lockSystem))),
    configWith(
        MutexConfig(default = MutexTypeConfig(lockSystem = lockSystem.getId()))
    )
)

fun lockSystemStub(): LockSystemStub = LockSystemStub()

class LockSystemStub : LockSystem {
    private val willGetLock: MutableList<Boolean> = LinkedList()

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
