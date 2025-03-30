package com.github.andresviedma.poket.config

import kotlinx.datetime.Clock
import kotlin.reflect.KClass

/**
 * Config source with objects passed directly on instantiation.
 * It is useful for unit testing.
 */
class ConstantConfigSource(
    vararg configObjects: Any
) : ConfigSource {
    private val objects = configObjects.associateBy { it.javaClass }.toMutableMap()

    override suspend fun <T : Any> getConfig(configClass: KClass<T>): T? =
        getConfigObject(configClass)

    @Suppress("unchecked_cast")
    fun <T : Any> getConfigObject(configClass: KClass<T>): T? =
        objects[configClass.java as Class<Any>] as T?

    fun addConfigObject(configObject: Any) {
        objects[configObject.javaClass] = configObject
    }

    fun override(configObject: Any) {
        addConfigObject(configObject)
    }

    fun <T : Any> override(configClass: KClass<T>, block: T.() -> T) {
        getConfigObject(configClass)?.block()
            ?.let { override(it) }
    }
}

fun configWith(vararg configObjects: Any): ConfigProvider =
    ConfigProvider(
        listOf(ConstantConfigSource(configObjects)),
        Clock.System // should not matter since it's not a hot-reloading source
    )
