package io.github.andresviedma.poket.config.utils

import io.github.andresviedma.poket.config.ConfigSource
import kotlin.reflect.KClass

class PatchFunctionConfigSource <T : Any> (
    private var patchFunction: (T?) -> T? = { it },
) : ConfigSource {
    override fun <T2 : Any> getConfig(configClass: KClass<T2>, config: T2?): T2? = patchFunction(config as? T) as T2

    fun override(newPatchFunction: (T?) -> T?) {
        patchFunction = newPatchFunction
    }
}
