package io.github.andresviedma.poket.inject.testcommons

import io.github.andresviedma.poket.support.inject.InjectorBindings
import kotlin.reflect.KClass

interface GenericInjector <I : Any> {
    fun createInjector(vararg bindings: InjectorBindings): I

    fun reset()

    fun getInjector(): I

    fun <T : Any> getInstance(clazz: KClass<T>): T
}

inline fun <reified T : Any> GenericInjector<*>.getInstance(): T = getInstance(T::class)
