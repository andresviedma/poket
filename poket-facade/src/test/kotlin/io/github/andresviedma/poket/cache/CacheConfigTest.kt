package io.github.andresviedma.poket.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CacheConfigTest : StringSpec({
    val baseTypeConfig = io.github.andresviedma.poket.cache.CacheTypeConfig()

    "empty config has all the defaults" {
        val config = io.github.andresviedma.poket.cache.CacheConfig()
        config.getTypeConfig("profile", baseTypeConfig) shouldBe io.github.andresviedma.poket.cache.CacheTypeConfig.DEFAULTS
    }

    "default config only" {
        val config = io.github.andresviedma.poket.cache.CacheConfig(
            default = io.github.andresviedma.poket.cache.CacheTypeConfig(cacheSystem = "mysystem", ttlInSeconds = 20L)
        )
        config.getTypeConfig("profile", baseTypeConfig).apply {
            cacheSystem shouldBe "mysystem"
            ttlInSeconds shouldBe 20L
            requestCollapsing shouldBe io.github.andresviedma.poket.cache.CacheTypeConfig.DEFAULTS.requestCollapsing
        }
    }

    "mixing profile and default" {
        val config = io.github.andresviedma.poket.cache.CacheConfig(
            default = io.github.andresviedma.poket.cache.CacheTypeConfig(
                cacheSystem = "mysystem",
                ttlInSeconds = 20L
            ),
            type = mapOf(
                "profile" to io.github.andresviedma.poket.cache.CacheTypeConfig(
                    cacheSystem = "mysystem-ok",
                    version = "2"
                )
            )
        )
        config.getTypeConfig("profile", baseTypeConfig).apply {
            cacheSystem shouldBe "mysystem-ok"
            version shouldBe "2"
            ttlInSeconds shouldBe 20L
            requestCollapsing shouldBe io.github.andresviedma.poket.cache.CacheTypeConfig.DEFAULTS.requestCollapsing
        }
    }
})
