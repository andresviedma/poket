package io.github.andresviedma.poket.backends.redisson

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.github.andresviedma.trekkie.thenExceptionThrown
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class RedissonConfigTest : FeatureSpec({

    feature("start config") {
        scenario("with no connection config") {
            When {
                RedissonConfig().redissonConfig
            }.thenExceptionThrown(IllegalStateException::class)
        }

        scenario("with single connection") {
            When {
                RedissonConfig.singleConnection("test").redissonConfig
            } then {
                it.isClusterConfig shouldBe false
                it.isSentinelConfig shouldBe false
            }
        }

        scenario("with cluster config host list") {
            When {
                RedissonConfig.clusterConnection("redis://aaa:9090, redis://bbb:9090").redissonConfig
            } then {
                it.isClusterConfig shouldBe true
                it.isSentinelConfig shouldBe false
            }
        }

        scenario("with sentinel config host list") {
            When {
                RedissonConfig.sentinelConnection("master", "redis://aaa:9090, redis://bbb:9090").redissonConfig
            } then {
                it.isClusterConfig shouldBe false
                it.isSentinelConfig shouldBe true
            }
        }

        scenario("with redisson config") {
            When {
                RedissonConfig(
                    config = mapOf(
                        "clusterServersConfig" to mapOf(
                            "nodeAddresses" to listOf("redis://aaa:9090", "redis://bbb:9090"),
                            "connectTimeout" to 1234,
                            "timeout" to 321,
                            "subscriptionConnectionPoolSize" to 22
                        ),
                        "threads" to 11
                    )
                )
            } then {
                it.redissonConfig.isClusterConfig shouldBe true
                it.redissonConfig.threads shouldBe 11

                val yamlProperties = it.redissonConfig.toYAML().lines()
                yamlProperties shouldContain "clusterServersConfig:"
                yamlProperties shouldContain "  connectTimeout: 1234"
                yamlProperties shouldContain "  timeout: 321"
                yamlProperties shouldContain "  subscriptionConnectionPoolSize: 22"
                yamlProperties shouldContain "threads: 11"
            }
        }

        scenario("with wrong redisson config") {
            When {
                RedissonConfig(
                    config = mapOf(
                        "clusterServersConfig" to mapOf(
                            "nodeAddresses" to listOf("redis://aaa:9090", "redis://bbb:9090"),
                            "kk" to "doesnotexist"
                        )
                    )
                ).redissonConfig
            }.thenExceptionThrown(UnrecognizedPropertyException::class)
        }
    }
})
