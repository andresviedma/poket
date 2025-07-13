package io.github.andresviedma.poket.inject.koin

import io.github.andresviedma.poket.support.inject.InjectorBindings
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.With
import io.github.andresviedma.trekkie.row
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class PoketModuleTest : StringSpec({

    afterEach { GlobalContext.stopKoin() }

    fun poketKoin(bindings: InjectorBindings) =
        startKoin {
            modules(poketModule(bindings))
        }.koin

    "singleton bindings" {
        With {
            poketKoin(
                InjectorBindings(
                    singletons = listOf(B::class, A::class),
                )
            )
        }.When { koin ->
            koin.get<B>() to koin.get<A>()
        } then { (b, a) ->
            b.a shouldBeSameInstanceAs a
        }
    }

    "multi-binding" {
        With {
            poketKoin(
                InjectorBindings(
                    singletons = listOf(XContainer::class),
                    multiBindings = mapOf(
                        X::class to listOf(X1::class, X2::class),
                    ),
                )
            )
        }.When { koin ->
            row(koin.get<XContainer>(), koin.get<X1>(), koin.get<X2>())
        } then { (xcontainer, x1, x2) ->
            xcontainer.elements.value shouldBe setOf(x1, x2)
        }
    }

    "empty multi-binding" {
        With {
            poketKoin(
                InjectorBindings(
                    singletons = listOf(XContainer::class),
                    multiBindings = mapOf(
                        X::class to emptyList(),
                    ),
                )
            )
        }.When { koin ->
            koin.get<XContainer>()
        } then { xcontainer ->
            xcontainer.elements.value shouldBe emptySet()
        }
    }

    "object" {
        println("*******" + (BWrapper::class).objectInstance)
    }
    "static bindings" {
        With {
            poketKoin(
                InjectorBindings(
                    singletons = listOf(B::class, A::class),
                    staticWrappers = listOf(BWrapper::class),
                )
            )
        }.When { koin ->
            koin.get<B>()
        } then { b ->
            BWrapper.myb shouldBeSameInstanceAs b
        }
    }
})
