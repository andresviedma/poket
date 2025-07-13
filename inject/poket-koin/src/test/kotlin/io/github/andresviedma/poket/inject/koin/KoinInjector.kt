package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.inject.testcommons.GenericInjector
import io.github.andresviedma.poket.support.inject.InjectorBindings
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import kotlin.reflect.KClass

class KoinInjector : GenericInjector<Koin> {
    private var koin: Koin? = null

    override fun createInjector(vararg bindings: InjectorBindings): Koin {
        return startKoin {
            modules(bindings.map { poketModule(it) })
        }.koin.also { koin = it }
    }

    override fun reset() {
        GlobalContext.stopKoin()
        koin = null
    }

    override fun getInjector(): Koin = koin!!

    override fun <T : Any> getInstance(clazz: KClass<T>): T =
        koin!!.get(clazz)
}
