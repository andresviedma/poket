package io.github.andresviedma.poket.inject.guice

import com.google.inject.Injector
import io.github.andresviedma.poket.config.ConfigSource
import kotlin.reflect.KClass

class GuiceInjectedConfigSource(
    private val injector: Injector
) : ConfigSource {
    override suspend fun <T : Any> getConfig(configClass: KClass<T>): T =
        injector.getInstance(configClass.java)
}
