package ca.derekellis.pantograph

import ca.derekellis.pantograph.db.Entry
import ca.derekellis.pantograph.db.PantographDatabase
import ca.derekellis.pantograph.di.PantographScope
import ca.derekellis.pantograph.model.FeedItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

@Inject
@PantographScope
class CollectorService(private val database: PantographDatabase, private val engine: HttpClientEngine) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            xml()
        }
    }
    private val hash = MessageDigest.getInstance("SHA-256")

    suspend fun getFeed(lang: String) = withContext(Dispatchers.IO) {
        val url = "https://www.octranspo.com/$lang/feeds/updates-$lang/"
        logger.debug("Making request to $url")
        val result: ByteArray = client.get(url).body()
        logger.debug("Parsing feed")
        val items = parse(result)

        val guids = items.map { it.guid }.toSet()
        val existing = database.entryQueries.getByGuids(guids).executeAsList().groupBy { it.guid }

        database.transaction {
            val newItems = items.filter {
                val old = existing[it.guid]?.firstOrNull() ?: return@filter true
                val content = "${it.title} ${it.description}"

                old.hash != hash.digest(content.encodeToByteArray()).toHexString()
            }

            logger.debug("Adding ${newItems.size} entries")
            logger.trace(newItems.toString())
            newItems.forEach {
                database.entryQueries.insert(
                    Entry(
                        it.guid,
                        it.title,
                        it.description,
                        lang,
                        emptyList(),
                        it.link,
                        it.pubDate,
                        LocalDateTime.now(),
                        null,
                        it.hash
                    )
                )
            }

            val nowRemoved = database.entryQueries.getNotRemoved().executeAsList().filter { it.guid !in guids }
            logger.debug("Marking ${nowRemoved.size} entries as removed")
            logger.trace(nowRemoved.toString())
            nowRemoved.forEach {
                database.entryQueries.setRemoval(LocalDateTime.now(), it.guid)
            }
        }
        logger.debug("Processing of $url complete")
    }

    private fun parse(data: ByteArray): List<FeedItem> {
        val xPath = XPathFactory.newInstance().newXPath()
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(data))

        val feedRoot = xPath.evaluate(
            "/rss/channel",
            document,
            XPathConstants.NODE
        ) as? Element

        val items = xPath.evaluate("./item", feedRoot, XPathConstants.NODESET) as NodeList
        return items.map { node ->
            check(node is Element)
            val title = xPath.evaluate("./title", node, XPathConstants.STRING) as String
            val dateString = xPath.evaluate("./pubDate", node, XPathConstants.STRING) as String
            val date = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz"))

            val category = xPath.evaluate("./category", node, XPathConstants.STRING) as String
            val guid = xPath.evaluate("./guid", node, XPathConstants.STRING) as String
            val link = xPath.evaluate("./link", node, XPathConstants.STRING) as String
            val description = xPath.evaluate("./description", node, XPathConstants.STRING) as String
            val hashed = hash.digest("$title $description".encodeToByteArray()).toHexString()

            FeedItem(title, date, category, description, guid, link, hashed)
        }
    }

    private fun <R> NodeList.map(block: (Node) -> R): List<R> = buildList {
        for (i in 0 until length) {
            add(block(item(i)))
        }
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}