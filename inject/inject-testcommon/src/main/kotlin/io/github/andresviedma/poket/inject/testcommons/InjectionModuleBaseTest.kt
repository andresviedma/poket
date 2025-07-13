package io.github.andresviedma.poket.inject.testcommons

import io.github.andresviedma.poket.support.inject.InjectorBindings
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

open class InjectionModuleBaseTest <I : Any> (
    private val engine: GenericInjector<I>,
) : StringSpec({

    afterEach { engine.reset() }

    "singleton bindings" {
        Given(engine) {
            createInjector(
                InjectorBindings(
                    singletons = listOf(B::class, A::class),
                )
            )
        }
        When {
            engine.getInstance<B>() to engine.getInstance<A>()
        } then { (b, a) ->
            b.a shouldBeSameInstanceAs a
        }
    }

    "multi-binding" {
        Given(engine) {
            createInjector(
                InjectorBindings(
                    singletons = listOf(XContainer::class),
                    multiBindings = mapOf(
                        X::class to listOf(X1::class, X2::class),
                    ),
                )
            )
        }
        When {
            engine.getInstance<XContainer>()
        } then { xcontainer ->
            xcontainer.elements.map { it.javaClass.simpleName }.toSet() shouldBe setOf("X1", "X2")
        }
    }

    "empty multi-binding" {
        Given(engine) {
            createInjector(
                InjectorBindings(
                    singletons = listOf(XContainer::class),
                    multiBindings = mapOf(
                        X::class to emptyList(),
                    ),
                )
            )
        }
        When {
            engine.getInstance<XContainer>()
        } then { xcontainer ->
            xcontainer.elements shouldBe emptySet()
        }
    }

    "static bindings" {
        Given(engine) {
            createInjector(
                InjectorBindings(
                    singletons = listOf(B::class, A::class),
                    staticWrappers = listOf(BWrapper::class),
                )
            )
        }
        When {
            engine.getInstance<B>()
        } then { b ->
            BWrapper.myb shouldBeSameInstanceAs b
        }
    }
})
