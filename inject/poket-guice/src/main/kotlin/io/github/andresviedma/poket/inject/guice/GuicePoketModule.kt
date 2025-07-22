package io.github.andresviedma.poket.inject.guice

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Singleton
import com.google.inject.multibindings.Multibinder
import io.github.andresviedma.poket.support.inject.InjectorBindings
import java.lang.reflect.Constructor
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

class GuicePoketModule(
    private val bindings: List<InjectorBindings>,
) : AbstractModule() {
    constructor(vararg bindings: InjectorBindings) : this(bindings.toList())

    override fun configure() {
        bindings.flatMap { it.singletons }.forEach {
            bind(it.java)
                .toConstructor(classConstructor(it))
                .`in`(Singleton::class.java)
        }

        bindings.flatMap { it.multiBindings.entries }.forEach { (interfaze, implementations) ->
            val setBinder = Multibinder.newSetBinder(binder(), interfaze.java)
            implementations.forEach { implementation ->
                setBinder.addBinding()
                    .toConstructor(classConstructor(implementation))
                    .`in`(Singleton::class.java)
                bind(implementation.java)
                    .toConstructor(classConstructor(implementation))
                    .`in`(Singleton::class.java)
            }
        }

        bindings.flatMap { it.interfaceObjects.entries }.forEach { (clazz, instance) ->
            bind(clazz.java).toInstance(casted(instance))
        }
        bindings.flatMap { it.interfaceSingletons.entries }.forEach { (interfaceClass, singletonClass) ->
            bind(interfaceClass.java)
                .toConstructor(classConstructor(singletonClass))
                .`in`(Singleton::class.java)
        }

        bindings.flatMap { it.staticWrappers }.forEach { clazz ->
            StaticInitializer.addWrapper(clazz)
        }

        bind(StaticInitializer::class.java).asEagerSingleton()
    }
}

private class StaticInitializer @Inject constructor(
    private val injector: Injector
) {
    init {
        wrappers.forEach { clazz ->
            val initFunction = clazz.functions.first { it.name == "init" }
            callFunction(initFunction, clazz.objectInstance)
        }
        wrappers.clear()
    }

    private fun callFunction(function: KFunction<*>, objectInstance: Any? = null) =
        function.callBy(getInjectedParameters(function, objectInstance))

    private fun getInjectedParameters(function: KFunction<*>, objectInstance: Any? = null) =
        function.parameters.associateWith {
            when {
                objectInstance != null && (it.type.classifier as KClass<*>).isInstance(objectInstance) ->
                    objectInstance

                it.type.isMarkedNullable -> {
                    val paramClass = (it.type.classifier as KClass<*>).java
                    runCatching { injector.getInstance(paramClass) }.getOrNull()
                }

                else -> {
                    val paramClass = (it.type.classifier as KClass<*>).java
                    injector.getInstance(paramClass)
                }
            }
        }

    companion object {
        private val wrappers = mutableListOf<KClass<*>>()

        fun addWrapper(clazz: KClass<*>) {
            wrappers += clazz
        }
    }
}

@Suppress("unchecked_cast")
private fun <S> classConstructor(clazz: KClass<*>): Constructor<S> =
    clazz.java.constructors.first() as Constructor<S>

@Suppress("unchecked_cast")
private fun <S> casted(obj: Any): S =
    obj as S
