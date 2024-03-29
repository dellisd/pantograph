package ca.derekellis.pantograph

import ca.derekellis.pantograph.db.PantographDatabase
import ca.derekellis.pantograph.db.TripRecord
import ca.derekellis.pantograph.di.PantographScope
import ca.derekellis.pantograph.model.ApiConfig
import ca.derekellis.pantograph.model.ConfigBase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

@Inject
@PantographScope
class TrackerService(
    private val database: PantographDatabase,
    private val engine: HttpClientEngine,
    private val config: ConfigBase
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            // The OC Transpo API returns JSON with the text/html Content-Type...
            json(contentType = ContentType.parse("text/html"))
        }
    }

    suspend fun getData(stop: String, routes: Set<String>) {
        val apiConfig = config.api
        requireNotNull(apiConfig)

        val url =
            "https://api.octranspo1.com/v2.0/GetNextTripsForStopWithIdAndGps?appID=${apiConfig.appId}&apiKey=${apiConfig.apiKey}&stopNo=${stop}"
        logger.debug("Making request to $url")

        val data = client.get(url).body<JsonObject>()
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

    internal fun parse(stop: String, data: JsonObject): List<TripRecord> {
        val container = data.getValue("GetNextTripsForStopWithIdAndGpsResult")
            .jsonObject.getValue("Route")
            .jsonObject.getValue("RouteDirection")

        val array = when (container) {
            is JsonArray -> container
            else -> buildJsonArray { add(container) }
        }

        return array.flatMap { element ->
            val tripJson = element.jsonObject["Trips"]?.jsonObject?.get("Trip") ?: return@flatMap emptyList()

            val trips = when (tripJson) {
                is JsonArray -> tripJson
                else -> buildJsonArray { add(tripJson) }
            }

            trips.map(JsonElement::jsonObject).map { trip ->
                TripRecord(
                    LocalDateTime.now(),
                    trip.getValue("TripID").jsonPrimitive.content,
                    stop,
                    Duration.ofMinutes(trip.getValue("AdjustedScheduleTime").jsonPrimitive.long),
                    trip.getValue("Longitude").jsonPrimitive.doubleOrNull,
                    trip.getValue("Latitude").jsonPrimitive.doubleOrNull,
                    trip.getValue("AdjustmentAge").jsonPrimitive.double.takeIf { it > 0 }
                        ?.let { Duration.ofMillis((60000 * it).toLong()) },
                    element.jsonObject["RouteNo"]?.jsonPrimitive?.content,
                )
            }
        }
    }
}
