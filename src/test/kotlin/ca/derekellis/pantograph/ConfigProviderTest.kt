package ca.derekellis.pantograph

import ca.derekellis.pantograph.model.ApiConfig
import ca.derekellis.pantograph.model.CollectorConfig
import ca.derekellis.pantograph.model.TrackerConfig
import ca.derekellis.pantograph.util.RESOURCES
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


internal class ConfigProviderTest {
    @Test
    fun `config parses correctly`() {
        val file = RESOURCES / "config/config.yaml"
        val provider = ConfigProvider(Yaml.default.decodeFromStream(file.inputStream()))

        assertNotNull(provider.api)
        assertEquals(ApiConfig("APP ID!", "API KEY!"), provider.api)

        assertEquals(CollectorConfig(refresh = 5), provider.collector)

        assertEquals(TrackerConfig(listOf("3000", "2000", "1000"), setOf("10")), provider.tracker.first())
    }
}
