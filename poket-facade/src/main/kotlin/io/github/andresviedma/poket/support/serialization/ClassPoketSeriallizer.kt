package io.github.andresviedma.poket.support.serialization

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/** Utility class to create more easily a custom cache value serializer that only works with a given class */
@Suppress("UNCHECKED_CAST")
class ClassPoketSerializer<V : Any>(
    private val clazz: KClass<V>,
    private val typedSerializer: TypedPoketSerializer<V>
) : PoketSerializer {
    constructor(clazz: KClass<V>, serialize: (V) -> String, deserialize: (String) -> V) :
        this(
            clazz,
            object : TypedPoketSerializer<V> {
                override fun serializeValue(value: V): String = serialize(value)
                override fun deserializeValue(serialized: String): V = deserialize(serialized)
            }
        )

    override fun <T : Any> serialize(deserialized: T): String =
        if (clazz.isInstance(deserialized)) {
            typedSerializer.serializeValue(deserialized as V)
        } else {
            error("Cache: Trying to serialize the wrong class ${deserialized::class.qualifiedName}")
        }

    override fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T =
        if (resultClass.isSubclassOf(clazz)) {
            typedSerializer.deserializeValue(serialized) as T
        } else {
            error("Cache: Trying to deserialize the wrong class ${resultClass.qualifiedName}")
        }
}

interface TypedPoketSerializer<V : Any> {
    fun serializeValue(value: V): String
    fun deserializeValue(serialized: String): V
}
