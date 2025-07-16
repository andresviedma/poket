package io.github.andresviedma.poket.config.propertytree

import kotlin.reflect.KClass

interface MapObjectMapper {
    fun objectToMap(configClass: KClass<*>, obj: Any?): Map<String, *>?
    fun <T : Any> mapToObject(configClass: KClass<T>, map: Map<String, *>): T
}
