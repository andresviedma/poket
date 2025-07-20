@file:Suppress("UNCHECKED_CAST", "UNUSED")

package io.github.andresviedma.poket.config.propertytree

sealed interface ConfigNode {
    fun toRaw(): Any?

    data class TreeNode(
        val children: MutableMap<String, ConfigNode> = mutableMapOf(),
    ) : ConfigNode {
        fun addProperties(properties: Map<String, String>) {
            properties.forEach { (property, value) -> addProperty(property, value) }
        }

        fun <T : Any> addProperty(property: String, value: T) {
            PropertyKey.fromPropertyName(property)?.let {
                getOrCreateSubtree(it.propertyPathKey).setValue(it.field, value)
            }
        }

        fun addRawMap(tree: Map<String, *>): TreeNode {
            tree.forEach { (key, value) ->
                when (value) {
                    null ->
                        children.remove(key)
                    is Map<*, *> -> {
                        val child = getOrAddChildTree(key)
                        child.addRawMap(value.mapKeys { (k, _) -> k?.toString() } as Map<String, *>)
                    }
                    else ->
                        children[key] = value.toNode()
                }
            }
            return this
        }

        private fun Any.toNode(): ConfigNode = when (this) {
            is Map<*, *> ->
                TreeNode(
                    filterKeys { it != null }
                        .map { (key, value) ->
                            key?.toString() to value?.toNode()
                        }.toMap().toMutableMap() as MutableMap<String, ConfigNode>
                )
            is List<*> ->
                ListNode(mapNotNull { it?.toNode() })
            else ->
                ValueNode(this)
        }

        fun addRawMaps(treeList: List<Map<String, *>>) {
            treeList.forEach { addRawMap(it) }
        }

        fun addRawMaps(vararg trees: Map<String, *>) {
            trees.forEach { addRawMap(it) }
        }

        fun setValue(key: String, value: Any) {
            children[key] = ValueNode(value)
        }

        fun overriddenWith(tree: TreeNode?): TreeNode =
            if (tree == null) this else cloned().addRawMap(tree.toRawMap())

        private fun <T: ConfigNode> T.cloned(): T = when (this) {
            is TreeNode -> TreeNode(children.mapValues { (_, v) -> v.cloned() }.toMutableMap()) as T
            is ListNode -> ListNode(elements.map { it.cloned() }) as T
            else -> this
        }

        fun getSubtreeNode(property: String): TreeNode? =
            getPropertyValue(PropertyKey.fromPropertyName(property))?.takeIf { it is TreeNode } as? TreeNode

        fun getPropertyNode(property: String): ConfigNode? =
            getPropertyValue(PropertyKey.fromPropertyName(property))

        private fun getChildTree(key: String): TreeNode? =
            children[key]?.takeIf { it is TreeNode } as? TreeNode

        private fun getOrAddChildTree(key: String): TreeNode =
            getChildTree(key) ?: (
                TreeNode().also { children[key] = it }
            )

        override fun toRaw(): Any =
            // Detection of list as map with numeric indexes
            if (children.keys.all { runCatching { Integer.parseUnsignedInt(it) }.getOrNull() != null }) {
                ListNode(children.entries.sortedBy { it.key.toInt() }.map { it.value })
                    .toRaw()
            } else {
                toRawMap()
            }

        fun propertyToRaw(property: String): Any? =
            getPropertyValue(PropertyKey.fromPropertyName(property))?.toRaw()

        fun toRawMap(): Map<String, *> =
            children.mapValues { (_, v) -> v.toRaw() }

        private fun TreeNode.getPropertyValue(key: PropertyKey?): ConfigNode? {
            if (key == null) return this
            return when {
                key.isSingleField ->
                    children[key.firstKey]
                else ->
                    getChildTree(key.firstKey)?.getPropertyValue(key.descend)
            }
        }

        private fun TreeNode.getOrCreateSubtree(key: PropertyKey?): TreeNode {
            if (key == null) return this
            return when {
                key.isSingleField ->
                    getOrAddChildTree(key.firstKey)
                else ->
                    getOrAddChildTree(key.firstKey).getOrCreateSubtree(key.descend)
            }
        }
    }

    data class ListNode(
        val elements: List<ConfigNode> = emptyList(),
    ) : ConfigNode {
        override fun toRaw() = elements.map { it.toRaw() }
    }

    data class ValueNode(
        val value: Any, // string, number, boolean... json-like
    ) : ConfigNode {
        override fun toRaw() = value
    }
}

private data class PropertyKey(
    private val parts: List<String>,
    private val index: Int = 0,
) {
    val isSingleField: Boolean get() = (index >= parts.lastIndex)
    val field: String get() = parts.last()
    val propertyPathKey: PropertyKey? = if (isSingleField) null else PropertyKey(parts.take(parts.size - 1), index)

    val firstKey get() = parts[index]

    val descend get() = PropertyKey(parts, index + 1)

    companion object {
        fun fromPropertyName(property: String): PropertyKey? {
            val parts = property.trim().split('.').map { it.trim() }
            return PropertyKey(parts).takeIf { parts.isNotEmpty() }
        }
    }
}
