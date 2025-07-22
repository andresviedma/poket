package io.github.andresviedma.poket.support.serialization.jackson

import io.github.andresviedma.poket.support.serialization.jackson.DefaultJacksonMappers.DEFAULT_JACKSON_SERIALIZER
import io.github.andresviedma.trekkie.When
import io.github.andresviedma.trekkie.Where
import io.github.andresviedma.trekkie.then
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class DefaultJacksonMapperTest : StringSpec({
    Where(
        10,
        10L,
        10.3,
        10.3.toBigDecimal(),
        true,
        Instant.now(),
    ) { value ->
        "map ${value::class.qualifiedName}" {
            When {
                val serialized = DEFAULT_JACKSON_SERIALIZER.writeValueAsString(value)
                DEFAULT_JACKSON_SERIALIZER.readValue(serialized, value::class.java)
            } then {
                it shouldBe value
            }
        }
    }
})
