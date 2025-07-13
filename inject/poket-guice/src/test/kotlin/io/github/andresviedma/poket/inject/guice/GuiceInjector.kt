package io.github.andresviedma.poket.inject.guice

import com.google.inject.Guice
import com.google.inject.Injector
import io.github.andresviedma.poket.inject.testcommons.GenericInjector
import io.github.andresviedma.poket.support.inject.InjectorBindings
import kotlin.reflect.KClass

class GuiceInjector : GenericInjector<Injector> {
    private var injector: Injector? = null

    override fun createInjector(vararg bindings: InjectorBindings): Injector =
        Guice.createInjector(
            bindings.map { PoketModule(it) }
        ).also { injector = it }

    override fun reset() {
        injector = null
    }

    override fun getInjector(): Injector = injector!!

    override fun <T : Any> getInstance(clazz: KClass<T>): T =
        injector!!.getInstance(clazz.java)
}
