package com.github.andresviedma.poket.serialization

interface PoketSerializer {
    fun serialize(myObject: Any): String
    fun <T> deserialize(serialized: String): T
}
