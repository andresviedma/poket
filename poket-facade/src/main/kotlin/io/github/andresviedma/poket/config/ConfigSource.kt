package io.github.andresviedma.poket.config

import kotlin.reflect.KClass
import kotlin.time.Duration

interface ConfigSource {

    /** If the source supports hot-reload, configuration of how/when the data will be reloaded */
    fun getReloadConfig(): ConfigSourceReloadConfig? = null

    /** If the source accesses the network, it should load the data here and store it locally */
    suspend fun reloadInfo(): Boolean = false

    /** Priority in overrides of this source respect to others */
    fun getConfigSourcePriority(): ConfigPriority = ConfigPriority.BASE

    /** Not suspendable, should get the config with no network call */
    fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? = null
}

data class ConfigSourceReloadConfig(
    /** If not null, after the given seconds the current value will be returned but updated asynchronously (if using getOrPut) */
    val outdateTime: Duration? = null
)

enum class ConfigPriority(
    val value: Int,
) {
    DEFAULT(10), // e.g. some injected object
    BASE(20), // base config common for all instances
    BASE_PLUS(30),
    ENVIRONMENT(40), // override for environment
    ENVIRONMENT_PLUS(50),
    APP(60), // override for app
    APP_PLUS(70),
    APP_ENVIRONMENT(80), // override for environment
    APP_ENVIRONMENT_PLUS(90),
    SECRETS(100),
    ;
}
