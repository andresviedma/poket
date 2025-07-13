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

fun poketModule(bindings: InjectorBindings) = module {
    bindings.singletons.forEach { clazz ->
        singleton(clazz as KClass<Any>)
    }

    bindings.multiBindings.forEach { (interfaze, implementations) ->
        implementations.forEach { implementation ->
            singleton(implementation as KClass<Any>).bind(interfaze as KClass<Any>)
        }
    }

    bindings.interfaceObjects.forEach { (clazz, value) ->
        single { value }.bind(clazz as KClass<Any>)
    }

    bindings.staticWrappers.forEach { clazz ->
        single(createdAtStart = true) {
            val initFunction = clazz.functions.first { it.name == "init" }
            clazz.cast(callFunction(initFunction, clazz.objectInstance))
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
        if (objectInstance != null && (it.type.classifier as KClass<*>).isInstance(objectInstance)) {
            objectInstance
        } else if (it.type.classifier == Lazy::class && it.type.arguments.first().type!!.classifier == Set::class) {
            val setType = it.type.arguments.first().type!!
            lazy { getAll<Any>(setType.arguments.first().type!!.classifier as KClass<*>).toSet() }
        } else if (it.type.classifier == Set::class) {
            getAll<Any>(it.type.arguments.first().type!!.classifier as KClass<*>).toSet()
        } else {
            get<Any>(it.type.classifier as KClass<*>)
        }
    }
