package io.github.andresviedma.poket.utils.retry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RetryProfileConfigTest : StringSpec({
    val config1 = RetryPolicyConfig(retries = listOf(1))
    val config2 = RetryPolicyConfig(retries = listOf(2))
    val config3 = RetryPolicyConfig(retries = listOf(3))

    "parent profile takes precedence" {
        val config = RetryProfileConfig(
            profiles = mapOf(
                "parent.child" to config1,
                "parent" to config2,
            ),
            default = config3
        )
        config.getProfileConfig("parent.child") shouldBe config1
    }

    "child profile is retrieved when there is no parent" {
        val config = RetryProfileConfig(
            profiles = mapOf(
                "parent" to config2,
            ),
            default = config3
        )
        config.getProfileConfig("parent.child") shouldBe config2
    }

    "access to non hierarchical profile" {
        val config = RetryProfileConfig(
            profiles = mapOf(
                "myprofile" to config2,
            ),
            default = config3
        )
        config.getProfileConfig("myprofile") shouldBe config2
    }

    "default profile is retrieved when there is no parent or child" {
        val config = RetryProfileConfig(
            default = config3
        )
        config.getProfileConfig("parent.child") shouldBe config3
    }

    "null profile uses default" {
        val config = RetryProfileConfig(
            default = config3
        )
        config.getProfileConfig(null) shouldBe config3
    }
})
