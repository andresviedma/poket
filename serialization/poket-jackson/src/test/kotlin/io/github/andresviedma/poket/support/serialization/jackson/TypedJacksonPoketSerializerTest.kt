package io.github.andresviedma.poket.support.serialization.jackson

import com.fasterxml.jackson.core.type.TypeReference
import io.github.andresviedma.poket.cache.ObjectCache
import io.github.andresviedma.poket.cache.ObjectCacheFactory
import io.github.andresviedma.poket.cache.local.MapCacheSystem
import io.github.andresviedma.poket.cache.utils.createCacheFactoryWithSystem
import io.github.andresviedma.trekkie.Where
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.Optional

class TypedJacksonPoketSerializerTest : StringSpec({
    isolationMode = IsolationMode.InstancePerTest

    val cacheFactory = createCacheFactoryWithSystem(MapCacheSystem())

    fun <T : Any> ObjectCacheFactory.cacheForType(typeReference: TypeReference<T>): ObjectCache<String, Any> {
        val typedSerializer = TypedJacksonPoketSerializer(ObjectMapperProvider.ofDefaultMapper(), typeReference)
        return createCache<String, Any>(
            type = "memory-perpetual",
            customSerializer = typedSerializer,
        )
    }

    Where(
        Optional.of(MyData(10)) to object : TypeReference<Optional<MyData>>() {},
        Optional.empty<MyData>() to object : TypeReference<Optional<MyData>>() {},
        listOf(MyData(10), MyData(20)) to object : TypeReference<List<MyData>>() {},
        emptyList<MyData>() to object : TypeReference<List<MyData>>() {},
        mapOf(10 to MyData(20)) to object : TypeReference<Map<Int, MyData>>() {},

    ) { (value, type) ->
        "serialize generic ${value::class.simpleName}: $value" {
            val cache = cacheFactory.cacheForType(type)
            cache.put("key", value)
            val value2 = cache.get("key")
            value2 shouldBe value
        }
    }
})

private data class MyData(
    val a: Int = 7,
)

