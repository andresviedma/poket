package io.github.andresviedma.poket.support.serialization

import kotlin.reflect.KClass

/**
 * Serializes and deserializes the given value, unless it is already a string.
 */
class PoketSerializerOrString(
    private val serializer: PoketSerializer,
) : PoketSerializer {
    override fun <T : Any> serialize(deserialized: T): String =
        if (deserialized is String)
            deserialized
        else
            serializer.serialize(deserialized)

    override fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T =
        if (resultClass == String::class)
            serialized as T
        else
            serializer.deserialize(serialized, resultClass)
}
