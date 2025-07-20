package io.github.andresviedma.poket.config.propertytree

import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class TreeNodeTest : FeatureSpec({
    feature("properties") {
        scenario("Single property-value") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addProperty("a", "hello")
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to "hello",
                )
            }
        }

        scenario("Single tree property") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addProperty("a.b.c", "hello")
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to mapOf(
                            "c" to "hello",
                        ),
                    ),
                )
            }
        }

        scenario("Two properties with shared path") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addProperty("a.b.c", "hello")
                tree.addProperty("a.x.y", "bye")
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to mapOf(
                            "c" to "hello",
                        ),
                        "x" to mapOf(
                            "y" to "bye",
                        ),
                    ),
                )
            }
        }

        scenario("Simple and composed property") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addProperty("a.b.c", "hello")
                tree.addProperty("y", "bye")
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to mapOf(
                            "c" to "hello",
                        ),
                    ),
                    "y" to "bye",
                )
            }
        }

        scenario("Composed path overridden with value") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addProperty("a.b.c", "hello")
                tree.addProperty("a.b", "bye")
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to "bye",
                    ),
                )
            }
        }

        scenario("Value overridden with composed path") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addProperty("a.b", "bye")
                tree.addProperty("a.b.c", "hello")
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to mapOf(
                            "c" to "hello",
                        ),
                    ),
                )
            }
        }
    }

    feature("raw map") {
        scenario("Single map") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addRawMap(
                    mapOf(
                        "a" to mapOf(
                            "b" to mapOf(
                                "c" to "hello",
                            ),
                        ),
                        "x" to listOf(
                            mapOf("s" to 2),
                            10,
                        )
                    )
                )
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to mapOf(
                            "c" to "hello",
                        ),
                    ),
                    "x" to listOf(
                        mapOf("s" to 2),
                        10,
                    )
                )

                tree.propertyToRaw("a.b.c") shouldBe "hello"
                tree.propertyToRaw("x") shouldBe listOf(
                    mapOf("s" to 2),
                    10,
                )
                tree.propertyToRaw("a.b") shouldBe mapOf("c" to "hello")
                tree.propertyToRaw("b") shouldBe null
            }
        }

        scenario("Two maps with overrides") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addRawMaps(
                    mapOf(
                        "a" to mapOf(
                            "b" to mapOf(
                                "c" to "hello",
                                "d" to "willbedeleted",
                            ),
                        ),
                        "x" to listOf(
                            mapOf("s" to 2),
                            10,
                        ),
                    ),
                    mapOf(
                        "a" to mapOf(
                            "b" to mapOf(
                                "d" to "overridden",
                                "x" to "hellosh",
                            ),
                            "d" to "gogogo",
                        ),
                        "x" to 6,
                    ),
                )
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to mapOf(
                        "b" to mapOf(
                            "c" to "hello",
                            "d" to "overridden",
                            "x" to "hellosh",
                        ),
                        "d" to "gogogo",
                    ),
                    "x" to 6,
                )
            }
        }

        scenario("Map with implicit list") {
            val tree = ConfigNode.TreeNode()
            When {
                tree.addRawMap(
                    mapOf(
                        "a" to mapOf(
                            "1" to mapOf(
                                "b2" to mapOf(
                                    "c2" to "hello",
                                ),
                            ),
                            "0" to mapOf(
                                "b1" to mapOf(
                                    "c1" to "hello",
                                ),
                            ),
                        ),
                        "w" to mapOf(
                            "0" to "zero",
                            "1" to "one",
                        ),
                        "x" to listOf(
                            mapOf("s" to 2),
                            10,
                        )
                    )
                )
            } then {
                tree.toRawMap() shouldBe mapOf(
                    "a" to listOf(
                        mapOf(
                            "b1" to mapOf(
                                "c1" to "hello",
                            ),
                        ),
                        mapOf(
                            "b2" to mapOf(
                                "c2" to "hello",
                            ),
                        ),
                    ),
                    "w" to listOf("zero", "one"),
                    "x" to listOf(
                        mapOf("s" to 2),
                        10,
                    )
                )

                tree.propertyToRaw("a.0.b1.c1") shouldBe "hello"
                tree.propertyToRaw("w") shouldBe listOf("zero", "one")
                tree.propertyToRaw("a") shouldBe listOf(
                    mapOf(
                        "b1" to mapOf(
                            "c1" to "hello",
                        ),
                    ),
                    mapOf(
                        "b2" to mapOf(
                            "c2" to "hello",
                        ),
                    ),
                )
                tree.propertyToRaw("x") shouldBe listOf(
                    mapOf("s" to 2),
                    10,
                )
            }
        }
    }
})
