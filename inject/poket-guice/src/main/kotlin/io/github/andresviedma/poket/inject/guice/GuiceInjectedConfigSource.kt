package io.github.andresviedma.poket.inject.guice

import com.google.inject.ConfigurationException
import com.google.inject.Injector
import io.github.andresviedma.poket.config.ConfigPriority
import io.github.andresviedma.poket.config.ConfigSource
import kotlin.reflect.KClass

class GuiceInjectedConfigSource(
    private val injector: Injector,
    private val priority: ConfigPriority = ConfigPriority.DEFAULT,
) : ConfigSource {
    override fun getConfigSourcePriority(): ConfigPriority = priority

    override fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? =
        try {
            injector.getInstance(configClass.java)
        } catch (_: ConfigurationException) {
            null
        }
}
