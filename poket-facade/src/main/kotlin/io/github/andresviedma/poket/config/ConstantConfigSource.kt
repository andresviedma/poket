package io.github.andresviedma.poket.config

import kotlin.reflect.KClass

/**
 * Config source with objects passed directly on instantiation.
 * It is useful for unit testing.
 */
class ConstantConfigSource(
    vararg configObjects: Any
) : ConfigSource {
    private val objects = configObjects.associateBy { it.javaClass }.toMutableMap()

    override fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? =
        getConfigObject(configClass)

    @Suppress("unchecked_cast", "MemberVisibilityCanBePrivate")
    fun <T : Any> getConfigObject(configClass: KClass<T>): T? =
        objects[configClass.java as Class<Any>] as T?

    @Suppress("MemberVisibilityCanBePrivate")
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

    @Suppress("unused")
    fun reset() {
        objects.clear()
    }
}

fun configWith(vararg configObjects: Any): ConfigProvider =
    ConfigProvider(
        setOf(ConstantConfigSource(*configObjects)),
    )
