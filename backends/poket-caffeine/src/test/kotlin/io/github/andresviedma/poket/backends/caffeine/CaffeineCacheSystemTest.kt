package io.github.andresviedma.poket.backends.caffeine

import io.github.andresviedma.trekkie.Given
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

class CaffeineCacheSystemTest : FeatureSpec({
    isolationMode = IsolationMode.InstancePerTest

    val cache = CaffeineCacheSystem()

    feature("getAndSetCacheData") {
        scenario("cache entry exists") {
            Given {
                cache.setObject("testNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                cache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe "testValue"
            }
        }

        scenario("cache entry not exists") {
            When {
                cache.getObject("testNamespace", "testNoExistantKey", String::class)
            } then {
                it shouldBe null
            }
        }

        scenario("cache entry exists in a different namespace") {
            Given {
                cache.setObject("otherNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                cache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe null
            }
        }

        scenario("multiple cache entries") {
            val data = TestDataClass("a", 5)
            val data2 = TestDataClass("b", 10)
            val data3 = TestDataClass("c", 15)
            Given {
                cache.setObjectList("testNamespace", mapOf("testKey" to data, "testKey2" to data2, "testKey3" to data3), 3600, false)
            }
            When {
                cache.getObjectList("testNamespace", listOf("testNoExistantKey", "testKey", "testKey3"), String::class)
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
                cache.setObject("testNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                cache.setObject("testNamespace", "testKey", "testValue2", 3600, false)
                cache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe "testValue2"
            }
        }

        scenario("use TTL parameter when setting the value") {
            When {
                cache.setObject("testNamespace", "testKey", "testValue2", ttlSeconds = 0, false)
                cache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe null // TTL = 0 so should expire
            }
        }

        scenario("set data class") {
            val data = TestDataClass("a", 5)
            Given {
                cache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                cache.getObject("testNamespace", "testKey", TestDataClass::class)
            } then {
                it shouldBe data
            }
        }

        scenario("set sealed class") {
            val data = SealedTestClass.Class1(50)
            Given {
                cache.setObject("testNamespace", "testKey", data, 3600, false)
            }
            When {
                cache.getObject("testNamespace", "testKey", SealedTestClass::class)
            } then {
                it.shouldBeInstanceOf<SealedTestClass.Class1>()
                it.foo shouldBe 50
            }
        }

        scenario("set multiple data") {
            val data = TestDataClass("a", 5)
            val data2 = SealedTestClass.Class1(50)
            val data3 = Clock.System.now()
            Given {
                cache.setObjectList("testNamespace", mapOf("testKey" to data, "testKey2" to data2, "testKey3" to data3), 3600, false)
            }
            When {
                cache.getObject("testNamespace", "testKey2", SealedTestClass::class)
            } then {
                it.shouldBeInstanceOf<SealedTestClass.Class1>()
                it.foo shouldBe 50
            }
        }
    }

    feature("invalidateObject") {
        scenario("invalidate object") {
            Given {
                cache.setObject("testNamespace", "testKey", "testValue", 3600, false)
            }
            When {
                cache.invalidateObject("testNamespace", "testKey")
                cache.getObject("testNamespace", "testKey", String::class)
            } then {
                it shouldBe null
            }
        }

        scenario("invalidate object list") {
            Given {
                cache.setObjectList("testNamespace", mapOf("testKey" to "testValue", "testKey2" to "testValue2", "testKey3" to "testValue3"), 3600, false)
            }
            When {
                cache.invalidateObjectList("testNamespace", listOf("testKey", "testKey2"))
                cache.getObjectList("testNamespace", listOf("testKey", "testKey2", "testKey3"), String::class)
            } then {
                it shouldBe mapOf("testKey3" to "testValue3")
            }
        }
    }
})

data class TestDataClass(val foo: String, val bar: Int)

sealed class SealedTestClass {
    class Class1(val foo: Int) : SealedTestClass()

    @Suppress("UNUSED")
    class Class2(val bar: Int) : SealedTestClass()
}
