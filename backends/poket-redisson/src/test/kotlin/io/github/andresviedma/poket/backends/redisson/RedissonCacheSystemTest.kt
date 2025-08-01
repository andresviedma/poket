package io.github.andresviedma.poket.backends.redisson

import io.github.andresviedma.poket.backends.redisson.env.BaseSpec
import io.github.andresviedma.poket.backends.redisson.env.IntegrationEnvironment
import io.github.andresviedma.poket.support.serialization.jackson.ObjectMapperProvider
import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.reflect.KClass

class RedissonCacheSystemTest : BaseSpec({

    val redisConfig = RedissonConfig.singleConnection(IntegrationEnvironment.redis.address)
    val redisCache = RedissonCacheSystem(RedissonClientProvider(redisConfig, ObjectMapperProvider.ofDefaultMapper()))

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

        @Suppress("UNCHECKED_CAST")
        scenario("cache entry exists, with generics") {
            val value = listOf(TestDataClass(foo = "fuu", bar = 3))
            Given {
                redisCache.setObject("testNamespace", "testKey", value, 3600, false)
            }
            When {
                redisCache.getObject<String, List<TestDataClass>>("testNamespace", "testKey", List::class as KClass<List<TestDataClass>>)
            } then {
                it shouldBe value
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
                redisCache.getObjectList("testNamespace", listOf("testNoExistantKey", "testKey", "testKey3"), String::class)
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

        scenario("set Instant") {
            val data = Clock.System.now()
            Given {
                redisCache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", Instant::class)
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
        scenario("set sealed class") {
            val data = SealedTestClass.Class1(50)
            Given {
                redisCache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey", SealedTestClass::class)
            } then {
                it!!::class.simpleName shouldBe "Class1"
                (it as SealedTestClass.Class1).foo shouldBe 50
            }
        }
        scenario("set multiple data") {
            val data = TestDataClass("a", 5)
            val data2 = SealedTestClass.Class1(50)
            val data3 = Clock.System.now()
            Given {
                redisCache.setObjectList("testNamespace", mapOf("testKey" to data, "testKey2" to data2, "testKey3" to data3), 3600, false)
            }
            When {
                redisCache.getObject("testNamespace", "testKey2", SealedTestClass::class)
            } then {
                it!!::class.simpleName shouldBe "Class1"
                (it as SealedTestClass.Class1).foo shouldBe 50
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
})

data class TestDataClass(val foo: String, val bar: Int)

class TestClass(val foo: String, val bar: Int)

sealed class SealedTestClass {
    class Class1(val foo: Int) : SealedTestClass()

    @Suppress("UNUSED")
    class Class2(val bar: Int) : SealedTestClass()
}
