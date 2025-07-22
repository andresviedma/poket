package io.github.andresviedma.poket.support.inject

import kotlin.reflect.KClass

interface OptionalBinder {
    fun <T : Any> getOptionalInstance(clazz: KClass<T>): T?

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun of(obj: Any) = object : OptionalBinder {
            override fun <T : Any> getOptionalInstance(clazz: KClass<T>): T =
                obj as T
        }

        fun ofNull() = object : OptionalBinder {
            override fun <T : Any> getOptionalInstance(clazz: KClass<T>): T? = null
        }
    }
}

inline fun <reified T : Any> OptionalBinder.getOptionalInstance(): T? =
    getOptionalInstance(T::class)
