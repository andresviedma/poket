package io.github.andresviedma.poket.mutex

import kotlinx.coroutines.runBlocking

@Suppress("unused")
class DistributedBlockingMutex(
    private val mutex: DistributedMutex,
) {
    fun <T> synchronized(vararg keys: Any, mutexBlock: suspend () -> T): T =
        runBlocking { mutex.synchronized(*keys, mutexBlock = mutexBlock) }

    fun <T> maybeSynchronized(vararg keys: Any, mutexBlock: suspend (Boolean) -> T): T =
        runBlocking { mutex.maybeSynchronized(*keys, mutexBlock = mutexBlock) }

    fun <T> ifSynchronized(vararg keys: Any, mutexBlock: suspend () -> T): T? =
        runBlocking { mutex.ifSynchronized(*keys, mutexBlock = mutexBlock) }
}
