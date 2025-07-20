package io.github.andresviedma.poket.config

import kotlin.reflect.KClass

/**
 * Easier accesor to typed config.
 */
class Config<T : Any>(
    private val configProvider: ConfigProvider,
    private val clazz: KClass<T>,
) {
    fun get(): T =
        configProvider.getConfig(clazz)
}
