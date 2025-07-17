package io.github.andresviedma.poket.support.serialization.jackson

import io.github.andresviedma.trekkie.Expect
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class JacksonObjectMapperTest : FeatureSpec({
    val mapper = JacksonObjectMapper()

    feature("object to map") {
        scenario("null") {
            Expect {
                mapper.objectToMap(MyConfig::class, null) shouldBe null
            }
        }

        scenario("load defaults") {
            When {
                mapper.objectToMap(
                    MyConfig::class,
                    MyConfig(
                        id = 2,
                        name = "hey",
                        float = 3.456.toBigDecimal(),
                    )
                )
            } then {
                it shouldBe mapOf(
                    "id" to 2,
                    "name" to "hey",
                    "float" to 3.456,
                    "default" to "hello",
                    "inner" to mapOf(
                        "inner" to "xxx",
                        "num" to 10,
                    ),
                    "innerList" to emptyList<Any>(),
                    "innerMap" to emptyMap<String, Any>(),
                )
            }
        }

        scenario("load with collections") {
            When {
                mapper.objectToMap(
                    MyConfig::class,
                    MyConfig(
                        id = 2,
                        name = "hey",
                        float = 3.456.toBigDecimal(),
                        inner = MyInnerConfig(inner = "inside"),
                        innerList = listOf(
                            MyInnerConfig(inner = "1"),
                            MyInnerConfig(inner = "2", num = null),
                        ),
                        innerMap = mapOf(
                            "a" to MyInnerConfig(inner = "a1", num = null),
                            "b" to MyInnerConfig(inner = "b1"),
                        )
                    )
                )
            } then {
                it shouldBe mapOf(
                    "id" to 2,
                    "name" to "hey",
                    "float" to 3.456,
                    "default" to "hello",
                    "inner" to mapOf(
                        "inner" to "inside",
                        "num" to 10,
                    ),
                    "innerList" to listOf<Any>(
                        mapOf(
                            "inner" to "1",
                            "num" to 10,
                        ),
                        mapOf(
                            "inner" to "2",
                            "num" to null,
                        ),
                    ),
                    "innerMap" to mapOf(
                        "a" to mapOf(
                            "inner" to "a1",
                            "num" to null,
                        ),
                        "b" to mapOf(
                            "inner" to "b1",
                            "num" to 10,
                        ),
                    ),
                )
            }
        }
    }

    feature("map to object") {
        scenario("load defaults") {
            When {
                mapper.mapToObject(
                    MyConfig::class,
                    mapOf(
                        "id" to 2,
                        "name" to "hey",
                        "float" to 3.456,
                        "default" to "hello",
                        "inner" to mapOf(
                            "inner" to "xxx",
                            "num" to 10,
                        ),
                        "innerList" to emptyList<Any>(),
                        "innerMap" to emptyMap<String, Any>(),
                    )
                )
            } then {
                it shouldBe MyConfig(
                    id = 2,
                    name = "hey",
                    float = 3.456.toBigDecimal(),
                )
            }
        }

        scenario("load with collections") {
            When {
                mapper.mapToObject(
                    MyConfig::class,
                    mapOf(
                        "id" to 2,
                        "name" to "hey",
                        "float" to 3.456,
                        "default" to "hello",
                        "inner" to mapOf(
                            "inner" to "inside",
                            "num" to 10,
                        ),
                        "innerList" to listOf<Any>(
                            mapOf(
                                "inner" to "1",
                                "num" to 10,
                            ),
                            mapOf(
                                "inner" to "2",
                                "num" to null,
                            ),
                        ),
                        "innerMap" to mapOf(
                            "a" to mapOf(
                                "inner" to "a1",
                                "num" to null,
                            ),
                            "b" to mapOf(
                                "inner" to "b1",
                                "num" to 10,
                            ),
                        ),
                    )
                )
            } then {
                it shouldBe MyConfig(
                    id = 2,
                    name = "hey",
                    float = 3.456.toBigDecimal(),
                    inner = MyInnerConfig(inner = "inside"),
                    innerList = listOf(
                        MyInnerConfig(inner = "1"),
                        MyInnerConfig(inner = "2", num = null),
                    ),
                    innerMap = mapOf(
                        "a" to MyInnerConfig(inner = "a1", num = null),
                        "b" to MyInnerConfig(inner = "b1"),
                    )
                )
            }
        }
    }
})

