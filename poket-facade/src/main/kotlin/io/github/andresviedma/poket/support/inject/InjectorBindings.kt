package io.github.andresviedma.poket.support.inject

import kotlin.reflect.KClass

/**
 * Dependency injection framework independent list of required bindings in a module, enabling
 * generic binding of dependencies.
 */
data class InjectorBindings(
    val singletons: List<KClass<*>> = emptyList(),
    val interfaceObjects: Map<KClass<*>, Any> = emptyMap(),
    val multiBindings: Map<KClass<*>, List<KClass<*>>> = emptyMap(),
    val staticWrappers: List<KClass<*>> = emptyList(),
)
