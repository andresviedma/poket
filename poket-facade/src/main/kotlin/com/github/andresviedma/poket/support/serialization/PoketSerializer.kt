package com.github.andresviedma.poket.support.serialization

import kotlin.reflect.KClass

/** Interface for generic custom cache value serializers */
interface PoketSerializer {
    fun <T : Any> serialize(deserialized: T): String
    fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T
}
