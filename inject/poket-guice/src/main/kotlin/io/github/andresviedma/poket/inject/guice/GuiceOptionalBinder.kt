package io.github.andresviedma.poket.inject.guice

import com.google.inject.Injector
import io.github.andresviedma.poket.support.inject.OptionalBinder
import kotlin.reflect.KClass

class GuiceOptionalBinder(
    private val injector: Injector,
) : OptionalBinder {
    override fun <T : Any> getOptionalInstance(clazz: KClass<T>): T? =
        runCatching { injector.getInstance(clazz.java) }.getOrNull()
}
