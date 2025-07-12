package io.github.andresviedma.poket.cache

import kotlin.reflect.KClass
import kotlin.reflect.full.functions

/**
 * Utility functions to serialize arbitrary cache keys or values to strings.
 * Cache system implementations may use this at their disposal.
 */

private const val DEFAULT_KEY_SEPARATOR: String = "::"

fun cacheKeyToString(namespace: String?, key: Any): String =
    listOfNotNull(namespace, simpleKeyToString(key)).joinToString(DEFAULT_KEY_SEPARATOR)

private fun simpleKeyToString(key: Any): String =
    when (key) {
        is Collection<*> -> key.joinToString(DEFAULT_KEY_SEPARATOR).ifEmpty { "-" }
        else -> tryDecompose(key).joinToString(DEFAULT_KEY_SEPARATOR)
    }

/*
fun cacheValueToString(value: Any): String =
    objectMapper.writeValueAsString(value)

inline fun <reified T> parseCachedValue(serializedValue: String): T =
    parseCachedValue(serializedValue, T::class.java)

fun <T> parseCachedValue(serializedValue: String, resultClass: Class<T>): T =
    objectMapper.readValue(serializedValue, resultClass)
*/

private fun tryDecompose(value: Any): List<*> {
    val clazz = value.javaClass.kotlin
    return if (clazz.isData) {
        val result = mutableListOf<Any?>()
        do {
            val (found, componentValue) = getComponent(value, clazz, result.size + 1)
            if (found) result.add(componentValue)
        } while (found)
        result.ifEmpty { listOf(value) }
    } else {
        listOf(value)
    }
}

private fun getComponent(dataObject: Any, clazz: KClass<*>, position: Int): Pair<Boolean, Any?> =
    runCatching {
        true to clazz.functions.find { it.name == "component$position" }!!.call(dataObject)
    }.getOrDefault(false to null)
