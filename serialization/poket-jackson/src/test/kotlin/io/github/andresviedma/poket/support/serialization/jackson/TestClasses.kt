@file:Suppress("unused")

package io.github.andresviedma.poket.support.serialization.jackson

import java.math.BigDecimal

data class MyConfig(
    val id: Int,
    val name: String,
    val float: BigDecimal,
    val default: String = "hello",
    val inner: MyInnerConfig = MyInnerConfig("xxx"),
    val innerList: List<MyInnerConfig> = emptyList(),
    val innerMap: Map<String, MyInnerConfig> = emptyMap(),
)

data class MyInnerConfig(
    val inner: String,
    val num: Int? = 10,
)
