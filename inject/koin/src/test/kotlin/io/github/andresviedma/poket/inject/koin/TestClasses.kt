package io.github.andresviedma.poket.inject.koin

interface X

class X0 : X
class X1 : X
class X2 : X
class X3 : X

class XContainer(val elements: Lazy<Set<X>>)

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
