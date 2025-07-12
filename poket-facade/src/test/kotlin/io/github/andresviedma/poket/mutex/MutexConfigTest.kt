package io.github.andresviedma.poket.mutex

import io.github.andresviedma.poket.mutex.MutexTypeConfig.Companion.DEFAULTS
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MutexConfigTest : StringSpec({
    "mixing config parts" {
        val mutexConfig = MutexConfig(
            default = MutexTypeConfig(fallbackLockSystem = "fallback", lockSystem = "mylock"),
            type = mapOf(
                "one" to MutexTypeConfig(lockSystem = "mylock1", timeoutInMillis = 10),
                "one::two" to MutexTypeConfig(timeoutInMillis = 20, ttlInMillis = 21),
                "one::two::three" to MutexTypeConfig(ttlInMillis = 31, onLockSystemError = MutexOnErrorAction.GET),
                "one::two::three::four::five" to MutexTypeConfig(onLockSystemError = MutexOnErrorAction.FALLBACK, failOnLockReleaseError = false),
                "one::two::three::four::five::six" to MutexTypeConfig(failOnLockReleaseError = true)
            )
        )
        mutexConfig.getTypeConfig("one::two::three::four::five::six") shouldBe MutexTypeConfig(
            lockSystem = "mylock1",
            timeoutInMillis = 20,
            ttlInMillis = 31,
            onLockSystemError = MutexOnErrorAction.FALLBACK,
            fallbackLockSystem = "fallback",
            failOnLockReleaseError = true
        )
    }

    "undefined with no default" {
        MutexConfig().getTypeConfig("kk") shouldBe MutexTypeConfig(
            lockSystem = "syncdb",
            timeoutInMillis = 10000,
            ttlInMillis = 10000,
            onLockSystemError = MutexOnErrorAction.FAIL,
            failOnLockReleaseError = false
        )
    }

    "undefined with default" {
        val default = MutexTypeConfig(
            lockSystem = "kk3",
            timeoutInMillis = 1,
            ttlInMillis = 2,
            onLockSystemError = MutexOnErrorAction.FALLBACK,
            fallbackLockSystem = "kk2",
            failOnLockReleaseError = true
        )
        MutexConfig(default = default).getTypeConfig("kk") shouldBe default
    }

    "base type config precedes default but is has less precedence than any type part" {
        val default = MutexTypeConfig(
            lockSystem = "kk1",
            timeoutInMillis = 1,
            ttlInMillis = 1
        )
        val baseType = MutexTypeConfig(
            lockSystem = "kk2",
            timeoutInMillis = 2
        )
        val firstPartType = MutexTypeConfig(
            lockSystem = "kk3"
        )
        val resultConfig = MutexConfig(default = default, type = mapOf("one" to firstPartType))
            .getTypeConfig("one::two::three", baseType)
        resultConfig shouldBe DEFAULTS.copy(
            lockSystem = "kk3",
            timeoutInMillis = 2,
            ttlInMillis = 1
        )
    }
})
