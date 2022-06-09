package ca.derekellis.pantograph.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ca.derekellis.pantograph.CollectorService
import ca.derekellis.pantograph.MainCommand
import ca.derekellis.pantograph.db.Entry
import ca.derekellis.pantograph.db.LocalDateTimeAdapter
import ca.derekellis.pantograph.db.PantographDatabase
import ca.derekellis.pantograph.db.StringListAdapter
import ca.derekellis.pantograph.db.migrateIfNeeded
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@Component
@PantographScope
abstract class PantographComponent(private val dbPath: String, @Component val network: NetworkComponent) {
    abstract val collectorService: CollectorService
    abstract internal val database: PantographDatabase

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
            )
        )
    }
}
