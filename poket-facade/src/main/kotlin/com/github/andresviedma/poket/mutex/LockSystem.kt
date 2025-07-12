package com.github.andresviedma.poket.mutex

import java.time.Duration

class LockSystemProvider(
    private val registeredSystems: Lazy<Set<LockSystem>>
) {
    constructor(registeredSystems: Set<LockSystem>) : this(lazyOf(registeredSystems))
    constructor(vararg registeredSystems: LockSystem) : this(registeredSystems.toSet())

    fun getLockSystem(type: String): LockSystem =
        registeredSystems.value.firstOrNull { it.getId() == type }
            ?: error("LockSystem unknown: $type")
}

interface LockSystem {
    fun getId(): String
    suspend fun waitLock(name: String, timeout: Duration, ttl: Duration): LockContext
    suspend fun getLockIfFree(name: String, ttl: Duration): LockContext
    suspend fun releaseLock(name: String, lockContext: LockContext): Boolean
}
