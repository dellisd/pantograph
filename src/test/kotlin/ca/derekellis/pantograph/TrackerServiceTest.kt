package ca.derekellis.pantograph

import ca.derekellis.pantograph.di.NetworkComponent
import ca.derekellis.pantograph.di.PantographComponent
import ca.derekellis.pantograph.di.create
import ca.derekellis.pantograph.util.RESOURCES
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TrackerServiceTest {
    private fun fixture(file: String): JsonObject {
        val path = RESOURCES / file
        return Json.decodeFromString(path.readText())
    }
    private lateinit var service: TrackerService

    @BeforeTest
    fun setup() {
        service = PantographComponent::class.create("", NetworkComponent::class.create()).trackerService
    }

    @Test
    fun `base json parse works`() {
        val data = fixture("api/base.json")
        val result = service.parse("STOP", data)

        assertEquals(3, result.size)

        val (first) = result
        assertEquals(Duration.ofMinutes(24), first.arrival)
        assertEquals("85322018", first.trip_id)
    }

    @Test
    fun `single object trip parse works`() {
        val data = fixture("api/single_trip.json")
        val result = service.parse("STOP", data)

        assertEquals(1, result.size)

        val (first) = result
        assertEquals(Duration.ofMinutes(24), first.arrival)
        assertEquals("85322018", first.trip_id)
    }
}
