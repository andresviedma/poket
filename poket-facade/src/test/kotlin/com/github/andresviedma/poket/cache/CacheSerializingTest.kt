package com.github.andresviedma.poket.cache

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe

class CacheSerializingTest : FeatureSpec({
    feature("cacheKeyToString") {
        scenario("string") {
            cacheKeyToString(null, "my-key") shouldBe "my-key"
        }
        scenario("number") {
            cacheKeyToString(null, 10) shouldBe "10"
        }
        scenario("number with prefix") {
            cacheKeyToString("1", 10) shouldBe "1::10"
        }
        scenario("long number") {
            cacheKeyToString(null, 9012345678901234567L) shouldBe "9012345678901234567"
        }
        scenario("list of values") {
            cacheKeyToString(null, listOf(1, 2)) shouldBe "1::2"
        }
        scenario("list with null") {
            cacheKeyToString(null, listOf(1, null, null)) shouldBe "1::null::null"
        }
        scenario("list of 1 value") {
            cacheKeyToString(null, listOf(1)) shouldBe "1"
        }
        scenario("empty list") {
            cacheKeyToString(null, emptyList<String>()) shouldBe "-"
        }
        scenario("data object") {
            cacheKeyToString(null, ("first" to 1)) shouldBe "first::1"
        }
        scenario("data object with null") {
            cacheKeyToString(null, (null to null)) shouldBe "null::null"
        }
    }
/*
    feature("cacheValueToString") {
        scenario("string") {
            cacheValueToString("my-value") shouldBe "\"my-value\""
        }
        scenario("number") {
            cacheValueToString(27L) shouldBe "27"
        }
        scenario("data class") {
            cacheValueToString("value1" to 2) shouldBe """{"first":"value1","second":2}"""
        }
        scenario("collection") {
            cacheValueToString(setOf(2, 7)) shouldBe "[2,7]"
        }
    }

    feature("parseCachedValue") {
        scenario("string") {
            parseCachedValue<String>("\"my-value\"") shouldBe "my-value"
        }
        scenario("number") {
            parseCachedValue<Long>("27") shouldBe 27L
        }
        scenario("data class") {
            parseCachedValue<Pair<String, Int>>("""{"first":"value1","second":2}""") shouldBe ("value1" to 2)
        }
        scenario("collection") {
            parseCachedValue<Set<Int>>("[2,7]") shouldBe setOf(2, 7)
        }
    }

 */
})
