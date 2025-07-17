package io.github.andresviedma.poket.support.serialization.snakeyaml

import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class YamlConfigTreeSourceTest : StringSpec({

    "load valid yaml file" {
        When {
            YamlConfigTreeSource.fromString(
                """
                id: 2
                name: hey
                float: 3.456
                default: "hello"
                inner:
                    inner: inside
                    num: 10
                innerList:
                - inner: "1"
                  num: 10
                - inner: "2"
                innerMap:
                    a:
                        inner: a1
                    b:
                        inner: b1
                        num: 10
    
                """.trimIndent()
            ).loadTree()
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
                    ),
                ),
                "innerMap" to mapOf(
                    "a" to mapOf(
                        "inner" to "a1",
                    ),
                    "b" to mapOf(
                        "inner" to "b1",
                        "num" to 10,
                    ),
                ),
            )
        }
    }
})
