package io.github.andresviedma.poket.backends.redisson

import org.redisson.config.Config
import org.yaml.snakeyaml.Yaml

data class RedissonConfig(
    val config: Map<String, Any>? = null
) {
    val redissonConfig: Config get() =
        config?.let { Config.fromYAML(Yaml().dump(it)) }
            ?: error("Could not bind redis config to any Connection Type")


    companion object {
        fun singleConnection(address: String) = RedissonConfig(
            config = mapOf(
                "singleServerConfig" to mapOf(
                    "address" to address
                )
            )
        )

        fun clusterConnection(vararg nodeAddresses: String) = RedissonConfig(
            config = mapOf(
                "clusterServersConfig" to mapOf(
                    "nodeAddresses" to nodeAddresses.toList()
                )
            )
        )

        fun sentinelConnection(masterName: String, vararg nodeAddresses: String) = RedissonConfig(
            config = mapOf(
                "sentinelServersConfig" to mapOf(
                    "sentinelAddresses" to nodeAddresses.toList(),
                    "masterName" to masterName
                )
            )
        )
    }
}
