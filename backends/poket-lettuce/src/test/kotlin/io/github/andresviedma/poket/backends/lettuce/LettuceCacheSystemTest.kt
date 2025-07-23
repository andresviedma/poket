package io.github.andresviedma.poket.backends.lettuce

import io.github.andresviedma.poket.backends.lettuce.env.BaseSpec
import io.github.andresviedma.poket.backends.lettuce.env.IntegrationEnvironment
import io.github.andresviedma.poket.support.serialization.jackson.ObjectMapperProvider
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.matchers.shouldBe

class LettuceCacheSystemTest : BaseSpec({

    val redisCache = LettuceCacheSystem(IntegrationEnvironment.redis.redisClient, ObjectMapperProvider.ofDefaultMapper())

    feature("getAndSetCacheData") {
        scenario("cache entry exists") {
            Given {
                redisCache.setObject("testNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe "testValue"
            }
        }

        scenario("cache entry not exists") {
            When {
                redisCache.getObject("testNamespace", "testNoExistantKey", String::class)
            } then {
                it shouldBe null
            }
        }

        scenario("multiple cache entries") {
            val data = TestDataClass("a", 5)
            val data2 = TestDataClass("b", 10)
            val data3 = TestDataClass("c", 15)
            Given {
                redisCache.setObjectList("testNamespace", mapOf("testKey" to data, "testKey2" to data2, "testKey3" to data3), 3600, false)
            }
            When {
                redisCache.getObjectList("testNamespace", listOf("testNoExistantKey", "testKey", "testKey3"), TestDataClass::class)
            } then {
                it.size shouldBe 2
                it["testKey"] shouldBe data
                it["testKey3"] shouldBe data3
            }
        }
    }

    feature("setCacheData") {
        scenario("override cache entry value") {
            Given {
                redisCache.setObject("testNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                redisCache.setObject("testNamespace", "testKey", "testValue2", 3600, false)
                redisCache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe "testValue2"
            }
        }

        scenario("set data class") {
            val data = TestDataClass("a", 5)
            Given {
                redisCache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", TestDataClass::class)
            } then {
                it shouldBe data
            }
        }

        scenario("set java Instant") {
            val data = java.time.Instant.now()
            Given {
                redisCache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", java.time.Instant::class)
            } then {
                it shouldBe data
            }
        }

        scenario("set class") {
            val data = TestClass("a", 5)
            Given {
                redisCache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", TestClass::class)
            } then {
                it!!.foo shouldBe "a"
                it.bar shouldBe 5
            }
        }

        scenario("set multiple data") {
            val data = TestDataClass("a", 5)
            val data2 = SealedTestClass.Class1(50)
            val data3 = 2.0.toBigDecimal()
            Given {
                redisCache.setObjectList("testNamespace", mapOf("testKey" to data, "testKey2" to data2, "testKey3" to data3), 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", TestDataClass::class)
            } then {
                it!!::class.simpleName shouldBe "TestDataClass"
                it.foo shouldBe "a"
            }
        }
    }

    feature("invalidateObject") {
        scenario("invalidate object") {
            Given {
                redisCache.setObject("testNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                redisCache.invalidateObject("testNamespace", "testKey")
                redisCache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe null
            }
        }

        scenario("invalidate object list") {
            Given {
                redisCache.setObjectList("testNamespace", mapOf("testKey" to "testValue", "testKey2" to "testValue2", "testKey3" to "testValue3"), 3600, false)
            }
            When {
                redisCache.invalidateObjectList("testNamespace", listOf("testKey", "testKey2"))
                redisCache.getObjectList("testNamespace", listOf("testKey", "testKey2", "testKey3"), String::class)
            } then {
                it shouldBe mapOf("testKey3" to "testValue3")
            }
        }
    }

    feature("invalidateChildren") {
        scenario("invalidates multiple keys for Pair key") {
            Given {
                redisCache.setObject("testNamespace", Pair("parent", "1"), "testValue", 3600, false)
                redisCache.setObject("testNamespace", Pair("parent", "2"), "testValue", 3600, false)
                redisCache.setObject("testNamespace", Pair("parentKK", "1"), "testValue", 3600, false)
            }
            When {
                redisCache.invalidateChildren("testNamespace", "parent")
            } then {
                redisCache.getObject("testNamespace", Pair("parent", "1"), String::class) shouldBe null
                redisCache.getObject("testNamespace", Pair("parent", "2"), String::class) shouldBe null
                redisCache.getObject("testNamespace", Pair("parentKK", "1"), String::class) shouldBe "testValue"
            }
        }
    }

    feature("invalidateAll") {
        scenario("invalidates multiple keys") {
            Given {
                redisCache.setObject("testNamespace", Pair("parent", "1"), "testValue", 3600, false)
                redisCache.setObject("testNamespace", Pair("parent", "2"), "testValue", 3600, false)
                redisCache.setObject("testNamespace", Pair("parentKK", "1"), "testValue", 3600, false)
                redisCache.setObject("testNamespace2", Pair("parentKK", "3"), "testValue", 3600, false)
            }
            When {
                redisCache.invalidateAll("testNamespace")
            } then {
                redisCache.getObject("testNamespace", Pair("parent", "1"), String::class) shouldBe null
                redisCache.getObject("testNamespace", Pair("parent", "2"), String::class) shouldBe null
                redisCache.getObject("testNamespace", Pair("parentKK", "1"), String::class) shouldBe null
                redisCache.getObject("testNamespace2", Pair("parentKK", "3"), String::class) shouldBe "testValue"
            }
        }
    }
})

data class TestDataClass(val foo: String, val bar: Int)

class TestClass(val foo: String, val bar: Int)

sealed class SealedTestClass {
    class Class1(val foo: Int) : SealedTestClass()

    @Suppress("UNUSED")
    class Class2(val bar: Int) : SealedTestClass()
}
