package io.github.andresviedma.poket.backends.redisson.env

object IntegrationEnvironment {
    val redis = RedisConnector()

    fun resetAll() {
        redis.reset()
    }

    fun stopAll() {
        redis.stop()
    }
}
