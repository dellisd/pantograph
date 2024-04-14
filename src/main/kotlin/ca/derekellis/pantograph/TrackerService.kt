package ca.derekellis.pantograph

import ca.derekellis.pantograph.db.PantographDatabase
import ca.derekellis.pantograph.db.TripRecord
import ca.derekellis.pantograph.di.PantographScope
import ca.derekellis.pantograph.model.ConfigBase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.time.Duration
import java.time.LocalDateTime
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Inject
@PantographScope
class TrackerService(
    private val database: PantographDatabase,
    private val engine: HttpClientEngine,
    private val config: ConfigBase
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient(engine)

    suspend fun getData(stop: String, routes: Set<String>) {
        val apiConfig = config.api
        requireNotNull(apiConfig)

        val url =
            "https://api.octranspo1.com/v2.0/GetNextTripsForStopAllRoutes?appID=${apiConfig.appId}&apiKey=${apiConfig.apiKey}&stopNo=${stop}&format=xml"
        logger.debug("Making request to $url")

        val data = client.get(url).body<ByteArray>()
        val result: List<TripRecord> = try {
            parse(stop, data)
        } catch (e: Exception) {
            logger.error("JSON Parsing error", e)
            return
        }

        database.transaction {
            result.filter { it.route in routes }.forEach {
                database.tripRecordQueries.insert(it)
            }
        }
    }

    internal fun parse(stop: String, data: ByteArray): List<TripRecord> {
        val xPath = XPathFactory.newInstance().newXPath()
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = documentBuilder.parse(ByteArrayInputStream(data))

        val responseRoot = xPath.evaluate(
            "/Envelope/Body/GetRouteSummaryForStopResponse/GetRouteSummaryForStopResult",
            document,
            XPathConstants.NODE,
        ) as? Element ?: return emptyList()

        // Check for any errors returned from the API. TODO : Check error conditions
        val errorCode = (xPath.evaluate("./Error", responseRoot, XPathConstants.STRING) as? String)?.toIntOrNull()
        if (errorCode != null && errorCode != 0) {
            logger.warn("Error in API response, code: $errorCode")
            return emptyList()
        }

        val routeNodes = xPath.evaluate("./Routes/Route", responseRoot, XPathConstants.NODESET) as NodeList
        val records = buildList {
            routeNodes.forEach<Element> { element ->
                val number = xPath.evaluate("./RouteNo", element, XPathConstants.STRING) as String
                val tripsElements = xPath.evaluate("./Trips/Trip", element, XPathConstants.NODESET) as NodeList

                tripsElements.forEach<Element> {
                    add(buildTripRecordsFromElement(stop, number, it, xPath))
                }
            }
        }
        return records
    }

    private fun buildTripRecordsFromElement(stop: String, route: String, element: Element, xPath: XPath): TripRecord {
        val adjustedScheduleTime =
            (xPath.evaluate("./AdjustedScheduleTime", element, XPathConstants.STRING) as String).toLong()
        val latitudeText = xPath.evaluate("./Latitude", element, XPathConstants.STRING) as String
        val longitudeText = xPath.evaluate("./Longitude", element, XPathConstants.STRING) as String
        val adjustmentAge = (xPath.evaluate("./AdjustmentAge", element, XPathConstants.STRING) as String).toFloat()
        val tripStartTime = xPath.evaluate("./TripStartTime", element, XPathConstants.STRING) as String

        return TripRecord(
            timestamp = LocalDateTime.now(),
            trip_id = tripStartTime,
            stop = stop,
            Duration.ofMinutes(adjustedScheduleTime),
            longitude = longitudeText.toDoubleOrNull(),
            latitude = latitudeText.toDoubleOrNull(),
            update_age = adjustmentAge.takeIf { it > 0 }
                ?.let { Duration.ofMillis((60000 * it).toLong()) },
            route = route,
        )
    }

    /**
     * Subscript operator for the [NodeList] class.
     * More convenient to use than the [NodeList.item] method.
     *
     * @param i The index into the collection.
     */
    private operator fun NodeList.get(i: Int) = item(i)

    @Suppress("UNCHECKED_CAST")
    private inline fun <T> NodeList.forEach(block: (T) -> Unit) {
        for (i in 0 until length) {
            block(get(i) as T)
        }
    }
}
