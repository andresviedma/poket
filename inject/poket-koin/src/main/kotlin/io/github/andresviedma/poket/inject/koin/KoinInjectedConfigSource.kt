package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.config.ConfigSource
import org.koin.core.context.GlobalContext
import kotlin.reflect.KClass

class KoinInjectedConfigSource : ConfigSource {
    override suspend fun <T : Any> getConfig(configClass: KClass<T>): T? =
        GlobalContext.get().getOrNull(configClass)
}
