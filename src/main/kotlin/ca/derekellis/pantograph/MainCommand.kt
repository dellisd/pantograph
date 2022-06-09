package ca.derekellis.pantograph

import ca.derekellis.pantograph.di.NetworkComponent
import ca.derekellis.pantograph.di.PantographComponent
import ca.derekellis.pantograph.di.create
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.tatarka.inject.annotations.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Inject
class MainCommand : CliktCommand() {
    private val db by argument().optional()
    private val delay by option(help = "The time between requests in minutes").int().default(3)

    override fun run() {
        val component = PantographComponent::class.create(db ?: "", NetworkComponent::class.create())
        runBlocking {
            while (true) {
                listOf(
                    async { component.collectorService.getFeed("en") },
                    async { component.collectorService.getFeed("fr") }
                ).awaitAll()

                delay(delay.minutes)
            }
            component.collectorService.getFeed("en")
        }
    }
}
