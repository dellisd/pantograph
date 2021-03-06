package ca.derekellis.pantograph.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ca.derekellis.pantograph.CollectorService
import ca.derekellis.pantograph.TrackerService
import ca.derekellis.pantograph.db.DurationAdapter
import ca.derekellis.pantograph.db.Entry
import ca.derekellis.pantograph.db.LocalDateTimeAdapter
import ca.derekellis.pantograph.db.PantographDatabase
import ca.derekellis.pantograph.db.StringListAdapter
import ca.derekellis.pantograph.db.TripRecord
import ca.derekellis.pantograph.db.migrateIfNeeded
import ca.derekellis.pantograph.model.ConfigBase
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@PantographScope
abstract class PantographComponent(
    private val dbPath: String,
    private val config: ConfigBase,
    @Component val network: NetworkComponent
) {
    abstract val collectorService: CollectorService
    abstract val trackerService: TrackerService
    internal abstract val database: PantographDatabase

    @Provides
    @PantographScope
    protected fun provideDatabase(): PantographDatabase = JdbcSqliteDriver("jdbc:sqlite:$dbPath").let { driver ->
        migrateIfNeeded(driver)
        PantographDatabase(
            driver,
            EntryAdapter = Entry.Adapter(
                StringListAdapter,
                LocalDateTimeAdapter,
                LocalDateTimeAdapter,
                LocalDateTimeAdapter
            ),
            TripRecordAdapter = TripRecord.Adapter(
                LocalDateTimeAdapter, DurationAdapter, DurationAdapter
            )
        )
    }

    @Provides
    fun provideConfig(): ConfigBase = config
}
