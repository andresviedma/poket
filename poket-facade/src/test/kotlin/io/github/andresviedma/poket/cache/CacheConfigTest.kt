package io.github.andresviedma.poket.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CacheConfigTest : StringSpec({
    val baseTypeConfig = CacheTypeConfig()

    "empty config has all the defaults" {
        val config = CacheConfig()
        config.getTypeConfig("profile", baseTypeConfig) shouldBe CacheTypeConfig.DEFAULTS
    }

    "default config only" {
        val config = CacheConfig(
            default = CacheTypeConfig(cacheSystem = "mysystem", ttlInSeconds = 20L)
        )
        config.getTypeConfig("profile", baseTypeConfig).apply {
            cacheSystem shouldBe "mysystem"
            ttlInSeconds shouldBe 20L
            requestCollapsing shouldBe CacheTypeConfig.DEFAULTS.requestCollapsing
        }
    }

    "mixing profile and default" {
        val config = CacheConfig(
            default = CacheTypeConfig(
                cacheSystem = "mysystem",
                ttlInSeconds = 20L
            ),
            type = mapOf(
                "profile" to CacheTypeConfig(
                    cacheSystem = "mysystem-ok",
                    version = "2"
                )
            )
        )
        config.getTypeConfig("profile", baseTypeConfig).apply {
            cacheSystem shouldBe "mysystem-ok"
            version shouldBe "2"
            ttlInSeconds shouldBe 20L
            requestCollapsing shouldBe CacheTypeConfig.DEFAULTS.requestCollapsing
        }
    }
})
