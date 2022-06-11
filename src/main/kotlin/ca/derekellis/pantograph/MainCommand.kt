package ca.derekellis.pantograph

import ca.derekellis.pantograph.di.NetworkComponent
import ca.derekellis.pantograph.di.PantographComponent
import ca.derekellis.pantograph.di.create
import ca.derekellis.pantograph.model.Config
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Inject
class MainCommand : CliktCommand() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val db by argument().optional()
    private val config by option().path(mustExist = true, canBeDir = false)

    private val configProvider by lazy { config?.let { ConfigProvider.load(it) } ?: Config() }

    override fun run() {
        val component = PantographComponent::class.create(db ?: "", configProvider, NetworkComponent::class.create())
        runBlocking {
            buildList {
                add(async { collector(component.collectorService) })

                if (configProvider.tracker.isNotEmpty()) {
                    if (configProvider.api != null) {
                        add(async { tracker(component.trackerService) })
                    } else {
                        logger.warn("OC Transpo API not configured. Can not begin tracking.")
                    }
                }
            }.joinAll()
        }
    }

    private suspend fun collector(service: CollectorService) = coroutineScope {
        while (true) {
            listOf(
                async { service.getFeed("en") },
                async { service.getFeed("fr") }
            ).awaitAll()

            delay(configProvider.collector.refresh.minutes)
        }
    }

    private suspend fun tracker(service: TrackerService) = coroutineScope {
        while (true) {
            configProvider.tracker.flatMap { trackerConfig ->
                trackerConfig.stops.map { stop ->
                    async { service.getData(stop, trackerConfig.route) }
                }
            }.joinAll()

            delay(45.seconds)
        }
    }
}
