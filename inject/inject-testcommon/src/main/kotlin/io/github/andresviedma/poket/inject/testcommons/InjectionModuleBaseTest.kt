package io.github.andresviedma.poket.inject.testcommons

import io.github.andresviedma.poket.support.inject.InjectorBindings
import io.github.andresviedma.trekkie.Expect
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

open class InjectionModuleBaseTest <I : Any> (
    private val engine: GenericInjector<I>,
    private val injectorBindings: InjectorBindings,
) : StringSpec({

    afterEach { engine.reset() }

    "singleton bindings" {
        Given(engine) {
            createInjector(
                injectorBindings,
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
                injectorBindings,
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
            xcontainer.elements.map { it::class.simpleName }.toSet() shouldBe setOf("X1", "X2")
        }
    }

    "empty multi-binding" {
        Given(engine) {
            createInjector(
                injectorBindings,
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
                injectorBindings,
                InjectorBindings(
                    singletons = listOf(B::class, A::class),
                    staticWrappers = listOf(BWrapper::class),
                )
            )
        }
        Expect {
            val b = engine.getInstance<B>()
            BWrapper.myb shouldBeSameInstanceAs b

        }
    }

    "nullable static bindings" {
        Given(engine) {
            createInjector(
                injectorBindings,
                InjectorBindings(
                    staticWrappers = listOf(NullableBWrapper::class),
                )
            )
        }
        Expect {
            NullableBWrapper.myb shouldBe null
        }
    }

    "singleton with optional binding" {
        Given(engine) {
            createInjector(
                injectorBindings,
                InjectorBindings(
                    singletons = listOf(NullableBWrapperSingleton::class),
                )
            )
        }
        Expect {
            val nullable = engine.getInstance<NullableBWrapperSingleton>()
            nullable.b shouldBe null
        }
    }
})
