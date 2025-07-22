package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.config.ConfigPriority
import io.github.andresviedma.poket.config.ConfigSource
import org.koin.core.context.GlobalContext
import kotlin.reflect.KClass

open class KoinInjectedConfigSource : ConfigSource {
    override fun getConfigSourcePriority(): ConfigPriority = ConfigPriority.DEFAULT

    override fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? =
        GlobalContext.get().getOrNull(configClass)
}
