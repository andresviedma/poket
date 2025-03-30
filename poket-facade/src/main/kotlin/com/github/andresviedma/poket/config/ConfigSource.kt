package com.github.andresviedma.poket.config

import kotlin.reflect.KClass

/**
 * Interface for implementations of configuration sources.
 */
interface ConfigSource {

    fun getDefaultReloadConfig(): ConfigSourceReloadConfig? = null

    suspend fun warmup() {}

    suspend fun <T : Any> getConfig(configClass: KClass<T>): T? = null
}

data class ConfigSourceReloadConfig(
    /** If not null, after the given seconds the current value will be returned but updated asynchronously (if using getOrPut) */
    val outdateTimeInSeconds: Int? = null
)
