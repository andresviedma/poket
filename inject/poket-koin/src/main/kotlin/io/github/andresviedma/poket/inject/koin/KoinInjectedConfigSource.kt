package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.config.ConfigPriority
import io.github.andresviedma.poket.config.ConfigSource
import org.koin.core.context.GlobalContext
import kotlin.reflect.KClass

class KoinInjectedConfigSource(
    private val priority: ConfigPriority = ConfigPriority.DEFAULT,
) : ConfigSource {
    override fun getConfigSourcePriority(): ConfigPriority = priority

    override fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? =
        GlobalContext.get().getOrNull(configClass)
}
