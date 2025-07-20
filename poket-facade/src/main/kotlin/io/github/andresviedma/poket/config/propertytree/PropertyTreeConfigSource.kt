package io.github.andresviedma.poket.config.propertytree

import io.github.andresviedma.poket.config.ConfigPriority
import io.github.andresviedma.poket.config.ConfigSource
import io.github.andresviedma.poket.config.ConfigSourceReloadConfig
import kotlin.reflect.KClass

class PropertyTreeConfigSource(
    propertySources: Set<ConfigPropertiesSource>,
    treeSources: Set<ConfigTreeSource>,
    configClassBindings: Set<ConfigClassBindings>,
    private val mapper: MapObjectMapper,
    private val priority: ConfigPriority = ConfigPriority.DEFAULT,
) : ConfigSource {

    private val treeNodeSources = treeSources.map { ConfigTreeTreeNodeSource(it) } + propertySources.map { ConfigPropertiesTreeNodeSource(it) }
    private var treeNodes: List<ConfigNode.TreeNode> = emptyList()
    private val registeredConfigClasses: Map<KClass<*>, ConfigClassBinding> =
        configClassBindings.flatMap { it.getConfigClassBindings() }.associateBy { it.clazz }

    override fun getConfigSourcePriority(): ConfigPriority = priority

    override suspend fun reloadInfo(): Boolean {
        val newTreeNodes = treeNodeSources.mapNotNull { it.loadTreeNode() }
        return (newTreeNodes != treeNodes)
            .also { treeNodes = newTreeNodes }
    }

    inline fun <reified T : Any> getConfig(config: T?): T? = getConfig(T::class, config)

    override fun <T : Any> getConfig(configClass: KClass<T>, config: T?): T? {
        val classBinding = registeredConfigClasses[configClass] ?: error("Config class not configured: ${configClass.qualifiedName}")
        val originalTree = mapper.objectToMap(configClass, config)?.let {
            ConfigNode.TreeNode().addRawMap(it)
        }
        val tree = getMergedNode(classBinding.treePath, originalTree)
        return tree?.let { mapper.mapToObject(configClass, tree.toRawMap()) }
    }

    override fun getReloadConfig(): ConfigSourceReloadConfig? =
        treeNodeSources.mapNotNull { it.getReloadConfig()?.outdateTime }.ifEmpty { null }
            ?.let { ConfigSourceReloadConfig(it.min()) }

    fun getPropertyValue(property: String): String? =
        treeNodes.asSequence().firstNotNullOfOrNull {
            val propertyNode = it.getPropertyNode(property)
            when (propertyNode) {
                null -> null
                is ConfigNode.ValueNode -> propertyNode.value.toString()
                else -> null
            }
        }

    private fun getMergedNode(treePath: String, originalTree: ConfigNode.TreeNode?): ConfigNode.TreeNode? =
        treeNodes.fold(originalTree) { node, override ->
            val newNode = override.getSubtreeNode(treePath)
            node.overriddenWith(newNode)
        }

    private fun ConfigNode.TreeNode?.overriddenWith(overrides: ConfigNode.TreeNode?): ConfigNode.TreeNode? =
        this?.overriddenWith(overrides) ?: overrides
}
