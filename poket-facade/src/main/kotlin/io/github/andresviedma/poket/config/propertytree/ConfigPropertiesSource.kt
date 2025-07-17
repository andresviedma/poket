package io.github.andresviedma.poket.config.propertytree

import io.github.andresviedma.poket.config.ConfigSourceReloadConfig

interface TreeNodeSource {
    suspend fun loadTreeNode(): ConfigNode.TreeNode?
    fun getReloadConfig(): ConfigSourceReloadConfig? = null
}

fun interface ConfigPropertiesSource {
    suspend fun loadAllProperties(): Map<String, String>
}

fun interface ConfigTreeSource {
    suspend fun loadTree(): Map<String, *>
}

class ConfigPropertiesTreeNodeSource(
    private val configPropertiesSource: ConfigPropertiesSource,
    private val reloadConfig: ConfigSourceReloadConfig? = null,
) : TreeNodeSource {
    override suspend fun loadTreeNode(): ConfigNode.TreeNode? =
        configPropertiesSource.loadAllProperties().takeIf { it.isNotEmpty() }?.let { properties ->
            ConfigNode.TreeNode().also { it.addProperties(properties) }
        }

    override fun getReloadConfig(): ConfigSourceReloadConfig? = reloadConfig
}

class ConfigTreeTreeNodeSource(
    private val configTreeSource: ConfigTreeSource,
    private val reloadConfig: ConfigSourceReloadConfig? = null,
) : TreeNodeSource {
    override suspend fun loadTreeNode(): ConfigNode.TreeNode? =
        configTreeSource.loadTree().takeIf { it.isNotEmpty() }?.let { rawTree ->
            ConfigNode.TreeNode().also { it.addRawMap(rawTree) }
        }

    override fun getReloadConfig(): ConfigSourceReloadConfig? = reloadConfig
}
