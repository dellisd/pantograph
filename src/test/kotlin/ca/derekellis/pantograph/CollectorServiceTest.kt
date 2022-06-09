package ca.derekellis.pantograph

import ca.derekellis.pantograph.db.PantographDatabase
import ca.derekellis.pantograph.di.PantographComponent
import ca.derekellis.pantograph.di.TestNetworkComponent
import ca.derekellis.pantograph.di.create
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import java.nio.file.Paths
import kotlin.io.path.div
import kotlin.io.path.readText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class CollectorServiceTest {
    private fun component(block: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): PantographComponent {
        return PantographComponent::class.create(
            "",
            TestNetworkComponent::class.create { scope, request -> block.invoke(scope, request) })
    }

    private fun fixtures(vararg files: String): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData {
        var request = 0
        return {
            val path = (RESOURCES / files.getOrElse(request++) { files.last() })
            respond(path.readText(), headers = headersOf(HttpHeaders.ContentType, "text/xml"))
        }
    }

    @Test
    fun `simple collection works`() = runTest {
        val component = component(fixtures("base.xml"))

        component.collectorService.getFeed("en")

        assertEquals(3, component.database.entryQueries.getAll().executeAsList().size)
    }

    @Test
    fun `new entry is added`() = runTest {
        val component = component(fixtures("base.xml", "new_entry.xml"))

        component.collectorService.getFeed("en") // Initial request
        component.collectorService.getFeed("en") // Request with new entry

        assertEquals(4, component.database.entryQueries.getAll().executeAsList().size)
    }

    @Test
    fun `removed entry is marked removed`() = runTest {
        val component = component(fixtures("base.xml", "removed_entry.xml"))

        component.collectorService.getFeed("en") // Initial request
        component.collectorService.getFeed("en") // Request with removed entry

        assertEquals(3, component.database.entryQueries.getAll().executeAsList().size)
        assertNotNull(
            component.database.entryQueries
                .getByGuid("https://www.octranspo.com/en/alerts#alert-11-bayshore-cancelled-trip-1654798078388")
                .executeAsOne()
                .removed
        )
    }

    @Test
    fun `updated entry is entered`() = runTest {
        val component = component(fixtures("base.xml", "updated_entry.xml"))

        component.collectorService.getFeed("en") // Initial request
        delay(5)
        component.collectorService.getFeed("en") // Request with updated entry

        assertEquals(4, component.database.entryQueries.getAll().executeAsList().size)

        // Results are in reverse-chronological order
        val (updated, original) = component.database.entryQueries
            .getByGuid("https://www.octranspo.com/en/alerts#alert-57-tunneys-pasture-cancelled-trip-1654799112813")
            .executeAsList()

        assertTrue { original.updated < updated.updated }
    }

    companion object {
        private val RESOURCES = Paths.get("src", "test", "resources")
    }
}