package io.github.andresviedma.poket.config.propertytree

import kotlin.reflect.KClass

interface ConfigClassBindings {
    fun getConfigClassBindings(): List<ConfigClassBinding>
}

data class ConfigClassBindingList(
    val bindings: List<ConfigClassBinding>,
): ConfigClassBindings {
    constructor(vararg bindings: ConfigClassBinding) : this(bindings.toList())
    constructor(vararg bindings: Pair<String, KClass<*>>)
            : this(bindings.map { (path, clazz) -> ConfigClassBinding(clazz, path) })

    override fun getConfigClassBindings(): List<ConfigClassBinding> =
        bindings
}

data class ConfigClassBinding(
    val clazz: KClass<*>,
    val treePath: String,
)
