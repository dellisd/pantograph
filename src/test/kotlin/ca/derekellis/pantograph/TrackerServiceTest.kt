package ca.derekellis.pantograph

import ca.derekellis.pantograph.di.PantographComponent
import ca.derekellis.pantograph.di.TestNetworkComponent
import ca.derekellis.pantograph.di.create
import ca.derekellis.pantograph.model.ApiConfig
import ca.derekellis.pantograph.model.Config
import ca.derekellis.pantograph.util.RESOURCES
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Duration
import kotlin.io.path.div
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TrackerServiceTest {
    private fun fixture(file: String): ByteArray {
        val path = RESOURCES / file
        return path.readBytes()
    }

    private lateinit var service: TrackerService

    @BeforeTest
    fun setup() {
        service = PantographComponent::class.create(
            "",
            Config(api = ApiConfig("TEST", "TEST")),
            TestNetworkComponent::class.create { scope, _ ->
                scope.respond(
                    (RESOURCES / "api/base.json").readText(),
                    headers = headersOf(HttpHeaders.ContentType, "text/html")
                )
            }).trackerService
    }

    @Test
    fun `base json parse works`() {
        val data = fixture("api/base.xml")
        val result = service.parse("STOP", data)

        assertEquals(3, result.size)

        val (first) = result
        assertEquals(Duration.ofMinutes(22), first.arrival)
        assertEquals("44", first.route)
    }
}
