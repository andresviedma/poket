package io.github.andresviedma.poket.config

import kotlin.reflect.KClass

interface ConfigSource {

    /** If the source supports hot-reload, configuration of how/when the data will be reloaded */
    fun getReloadConfig(): ConfigSourceReloadConfig? = null

    /** If the source accesses the network, it should load the data here and store it locally */
    suspend fun reloadInfo(): Boolean = false

    /** Not suspendable, should get the config with no network call */
    fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? = null
}

data class ConfigSourceReloadConfig(
    /** If not null, after the given seconds the current value will be returned but updated asynchronously (if using getOrPut) */
    val outdateTimeInSeconds: Int? = null
)
