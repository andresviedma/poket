package io.github.andresviedma.poket.config.propertytree

import io.github.andresviedma.poket.config.utils.JacksonObjectMapper
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.github.andresviedma.trekkie.thenExceptionThrown
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class PropertyTreeConfigSourceTest : FeatureSpec({
    isolationMode = IsolationMode.InstancePerTest

    fun newPropertySource() = object : ConfigPropertiesSource {
        var properties: Map<String, String> = emptyMap()

        override suspend fun loadAllProperties(): Map<String, String> = properties
    }

    val propertySource1 = newPropertySource()
    val propertuSource2 = newPropertySource()

    val treeSource = object : ConfigTreeSource {
        var tree: Map<String, *> = emptyMap<String, Any>()

        override suspend fun loadTree(): Map<String, *> = tree
    }

    val config = PropertyTreeConfigSource(
        propertySources = setOf(propertySource1, propertuSource2),
        treeSources = setOf(treeSource),
        mapper = JacksonObjectMapper(),
        configClassBindings = setOf(ConfigClassBindingList("config" to MyConfig::class))
    )

    feature("get config from properties") {
        scenario("get config with minimal values and using defaults") {
            Given(propertySource1) {
                properties = mapOf(
                    "config.id" to "xxx",
                    "config.float" to "7.25",
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } then {
                it shouldBe MyConfig(
                    id = "xxx",
                    num = 0,
                    float = "7.25".toBigDecimal(),
                    composed = null,
                )
            }
        }

        scenario("load composed object") {
            Given(propertySource1) {
                properties = mapOf(
                    "config.id" to "xxx",
                    "config.float" to "7.25",
                    "config.composed.name" to "hello",
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } then {
                it shouldBe MyConfig(
                    id = "xxx",
                    num = 0,
                    float = "7.25".toBigDecimal(),
                    composed = MyInnerConfig(name = "hello"),
                )
            }
        }

        scenario("should fail if a required property is missing") {
            Given(propertySource1) {
                properties = mapOf(
                    "config.id" to "xxx",
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } thenExceptionThrown Exception::class
        }

        scenario("mix properties from different sources") {
            Given(propertySource1) {
                properties = mapOf(
                    "config.float" to "7.25",
                )
            }
            Given(propertuSource2) {
                properties = mapOf(
                    "config.id" to "xxx",
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } then {
                it shouldBe MyConfig(
                    id = "xxx",
                    num = 0,
                    float = "7.25".toBigDecimal(),
                    composed = null,
                )
            }
        }
    }

    feature("get config from tree") {
        scenario("get config with minimal values and using defaults") {
            Given(treeSource) {
                tree = mapOf(
                    "config" to mapOf(
                        "id" to "xxx",
                        "float" to "7.25",
                    ),
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } then {
                it shouldBe MyConfig(
                    id = "xxx",
                    num = 0,
                    float = "7.25".toBigDecimal(),
                    composed = null,
                )
            }
        }

        scenario("override with property") {
            Given(treeSource) {
                tree = mapOf(
                    "config" to mapOf(
                        "id" to "xxx",
                        "float" to "7.25",
                    ),
                )
            }
            Given(propertySource1) {
                properties = mapOf(
                    "config.id" to "overridden",
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } then {
                it shouldBe MyConfig(
                    id = "overridden",
                    num = 0,
                    float = "7.25".toBigDecimal(),
                    composed = null,
                )
            }
        }

        scenario("get embedded list of classes") {
            Given(treeSource) {
                tree = mapOf(
                    "config" to mapOf(
                        "id" to "xxx",
                        "float" to "7.25",
                        "composed" to mapOf(
                            "name" to "name1",
                        ),
                        "composedList" to listOf(
                            mapOf(
                                "name" to "name2",
                            ),
                            mapOf(
                                "name" to "name3",
                            ),
                        ),

                    ),
                )
            }
            Given(config) {
                reloadInfo()
            }
            When {
                config.getConfig<MyConfig>(null)
            } then {
                it shouldBe MyConfig(
                    id = "xxx",
                    num = 0,
                    float = "7.25".toBigDecimal(),
                    composed = MyInnerConfig("name1"),
                    composedList = listOf(
                        MyInnerConfig("name2"),
                        MyInnerConfig("name3"),
                    ),
                )
            }
        }
    }
})

data class MyConfig(
    val id: String,
    val num: Int = 0,
    val float: BigDecimal,
    val composed: MyInnerConfig? = null,
    val composedList: List<MyInnerConfig> = emptyList(),
)

data class MyInnerConfig(
    val name: String,
)
