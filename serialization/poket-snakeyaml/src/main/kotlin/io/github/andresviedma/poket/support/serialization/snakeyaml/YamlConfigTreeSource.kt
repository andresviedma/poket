package io.github.andresviedma.poket.support.serialization.snakeyaml

import io.github.andresviedma.poket.config.propertytree.ConfigTreeSource
import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader

class YamlConfigTreeSource(
    private val reader: Reader,
) : ConfigTreeSource {

    override suspend fun loadTree(): Map<String, *> =
        Yaml().load(reader)

    companion object {
        fun fromString(content: String) = YamlConfigTreeSource(StringReader(content))
        fun fromReader(reader: Reader) = YamlConfigTreeSource(reader)
        fun fromStream(stream: InputStream) = YamlConfigTreeSource(InputStreamReader(stream, "UTF-8"))
    }
}
