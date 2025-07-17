package io.github.andresviedma.poket.config.utils

import io.github.andresviedma.poket.config.ConfigProvider
import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.config.ConfigSourceReloadConfig
import kotlin.reflect.KClass

/**
 * Config source with objects passed directly on instantiation.
 * It is useful for unit testing.
 */
class ConstantConfigSource(
    configObjects: List<Any>,
    private val reloadConfig: ConfigSourceReloadConfig? = null,
) : ConfigSource {
    constructor(vararg configObjects: Any) : this(configObjects.toList())
    constructor(reloadConfig: ConfigSourceReloadConfig?, vararg configObjects: Any) : this(configObjects.toList(), reloadConfig)
    constructor(reloadConfig: ConfigSourceReloadConfig?) : this(emptyList(), reloadConfig)

    private val objects = configObjects.associateBy { it::class }.toMutableMap()

    override fun getReloadConfig(): ConfigSourceReloadConfig? = reloadConfig

    override suspend fun reloadInfo(): Boolean =
        (reloadConfig != null) // If there's some reload config, we'll assume it can change

    override fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? =
        getConfigObject(configClass) ?: config

    @Suppress("unchecked_cast", "MemberVisibilityCanBePrivate")
    fun <T : Any> getConfigObject(configClass: KClass<T>): T? =
        runCatching { objects[configClass as KClass<Any>] as T? }.getOrNull()

    @Suppress("MemberVisibilityCanBePrivate")
    fun addConfigObject(configObject: Any) {
        objects[configObject::class] = configObject
    }

    fun override(configObject: Any) {
        addConfigObject(configObject)
    }

    fun <T : Any> override(configClass: KClass<T>, block: T.() -> T) {
        getConfigObject(configClass)?.block()
            ?.let { override(it) }
    }

    @Suppress("unused")
    fun reset() {
        objects.clear()
    }
}

fun configWith(vararg configObjects: Any): ConfigProvider =
    ConfigProvider(
        setOf(ConstantConfigSource(*configObjects)),
    )
