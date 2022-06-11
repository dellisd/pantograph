package ca.derekellis.pantograph

import ca.derekellis.pantograph.model.Config
import ca.derekellis.pantograph.model.ConfigBase
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.inputStream

class ConfigProvider(private val config: Config) : ConfigBase by config {

    companion object {
        fun load(path: Path): ConfigProvider =
            ConfigProvider(Yaml.default.decodeFromStream(path.inputStream()))
    }
}
