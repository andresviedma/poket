package com.github.andresviedma.poket.serialization

import kotlin.reflect.KClass

interface PoketSerializer {
    fun <T : Any> serialize(deserialized: T): String
    fun <T : Any> deserialize(serialized: String, resultClass: KClass<T>): T
}
