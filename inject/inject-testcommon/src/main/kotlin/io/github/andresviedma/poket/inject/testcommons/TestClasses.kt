@file:Suppress("unused")

package io.github.andresviedma.poket.inject.testcommons

import io.github.andresviedma.poket.support.inject.OptionalBinder
import io.github.andresviedma.poket.support.inject.getOptionalInstance

interface X

class X1 : X
class X2(val x1: X1) : X

class XContainer(val elements: Set<X>)

class A {
    val x = (1..10000).random()
}

data class B(val a: A)

object BWrapper {
    private var b: B? = null
    val myb get() = b

    fun init(b: B): BWrapper = apply {
        this.b = b
    }
}

object NullableBWrapper {
    private var b: B? = null
    val myb get() = b

    fun init(b: B?): NullableBWrapper = apply {
        this.b = b
    }
}

class NullableBWrapperSingleton(optionalBinder: OptionalBinder) {
    val b: B? = optionalBinder.getOptionalInstance()
}

class BWrapperSingleton {
    val b: B? = BWrapper.myb
}
