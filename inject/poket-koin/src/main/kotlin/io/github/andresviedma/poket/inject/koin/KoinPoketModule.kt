@file:Suppress("unchecked_cast")

package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.support.inject.InjectorBindings
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.cast
import kotlin.reflect.full.functions

fun poketModule(vararg bindings: InjectorBindings) = module {
    bindings.flatMap { it.singletons }.forEach { clazz ->
        singleton(clazz as KClass<Any>)
    }

    bindings.flatMap { it.multiBindings.entries }.forEach { (interfaze, implementations) ->
        implementations.forEach { implementation ->
            singleton(implementation as KClass<Any>).bind(interfaze as KClass<Any>)
        }
    }

    bindings.flatMap { it.interfaceObjects.entries }.forEach { (clazz, instance) ->
        single { instance }.bind(clazz as KClass<Any>)
    }

    val staticWrappers = bindings.flatMap { it.staticWrappers }
    if (staticWrappers.isNotEmpty()) {
        single<KoinInitializer>(createdAtStart = true) { // beware, there can be only one module with static wrappers
            KoinInitializer.init(this, staticWrappers)
        }
    }
}

private class KoinInitializer {
    companion object {
        fun init(scope: Scope, wrappers: List<KClass<*>>): KoinInitializer {
            wrappers.forEach { clazz ->
                val initFunction = clazz.functions.first { it.name == "init" }
                scope.callFunction(initFunction, clazz.objectInstance)
            }
            return KoinInitializer()
        }
    }
}

private fun Module.singleton(clazz: KClass<Any>): KoinDefinition<Any> =
    single {
        val constructor = clazz.constructors.first()
        clazz.cast(callFunction(constructor))
    }.bind(clazz) as KoinDefinition<Any>

private fun Scope.callFunction(function: KFunction<*>, objectInstance: Any? = null) =
    function.callBy(getInjectedParameters(function, objectInstance))

private fun Scope.getInjectedParameters(function: KFunction<*>, objectInstance: Any? = null) =
    function.parameters.associateWith {
        when {
            objectInstance != null && (it.type.classifier as KClass<*>).isInstance(objectInstance) ->
                objectInstance
            it.type.classifier == Lazy::class && it.type.arguments.first().type!!.classifier == Set::class -> {
                val setType = it.type.arguments.first().type!!
                lazy { getAll<Any>(setType.arguments.first().type!!.classifier as KClass<*>).toSet() }
            }
            it.type.classifier == Set::class ->
                getAll<Any>(it.type.arguments.first().type!!.classifier as KClass<*>).toSet()
            it.type.isMarkedNullable ->
                getOrNull<Any>(it.type.classifier as KClass<*>)
            else ->
                get<Any>(it.type.classifier as KClass<*>)
        }
    }
