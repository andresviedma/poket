package io.github.andresviedma.poket.backends.lettuce.env

object IntegrationEnvironment {
    val redis = RedisConnector()

    fun resetAll() {
        redis.reset()
    }

    fun stopAll() {
        redis.stop()
    }
}
