package io.github.andresviedma.poket.config.utils

import io.github.andresviedma.poket.config.ConfigPriority
import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.config.ConfigSourceReloadConfig
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class PatchFunctionConfigSource <T : Any> (
    private var patchFunction: (T?) -> T? = { it },
    private val reloadConfig: ConfigSourceReloadConfig? = null,
    private val priority: ConfigPriority = ConfigPriority.BASE,
) : ConfigSource {
    override fun <T2 : Any> getConfig(configClass: KClass<T2>, config: T2?): T2? = patchFunction(config as? T) as? T2

    override fun getConfigSourcePriority(): ConfigPriority = priority
    override fun getReloadConfig(): ConfigSourceReloadConfig? = reloadConfig

    fun override(newPatchFunction: (T?) -> T?) {
        patchFunction = newPatchFunction
    }
}
