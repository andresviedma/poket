package io.github.andresviedma.poket.mutex

import kotlin.time.Duration


class LockSystemProvider(
    private val registeredSystems: Set<LockSystem>
) {
    fun getLockSystem(type: String): LockSystem =
        registeredSystems.firstOrNull { it.getId() == type }
            ?: error("LockSystem unknown: $type")

    companion object {
        fun withLockSystems(registeredSystems: Set<LockSystem>) =
            LockSystemProvider(registeredSystems)
        fun withLockSystems(vararg registeredSystems: LockSystem) =
            withLockSystems(registeredSystems.toSet())

    }
}

interface LockSystem {
    fun getId(): String
    suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext
    suspend fun getLockIfFree(name: String, ttl: Duration): LockContext
    suspend fun releaseLock(name: String, lockContext: LockContext): Boolean
}
