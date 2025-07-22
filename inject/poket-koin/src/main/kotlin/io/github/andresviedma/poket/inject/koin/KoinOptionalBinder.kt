package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.support.inject.OptionalBinder
import org.koin.core.context.GlobalContext
import kotlin.reflect.KClass

class KoinOptionalBinder : OptionalBinder {
    override fun <T : Any> getOptionalInstance(clazz: KClass<T>): T? =
        runCatching { GlobalContext.get().get<T>(clazz) }.getOrNull()
}
