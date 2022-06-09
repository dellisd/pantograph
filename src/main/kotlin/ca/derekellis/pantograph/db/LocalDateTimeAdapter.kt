package ca.derekellis.pantograph.db

import app.cash.sqldelight.ColumnAdapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeAdapter : ColumnAdapter<LocalDateTime, String> {
    override fun decode(databaseValue: String): LocalDateTime =
        LocalDateTime.parse(databaseValue, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    override fun encode(value: LocalDateTime): String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value)
}

object StringListAdapter : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> = databaseValue.split(",")

    override fun encode(value: List<String>): String = value.joinToString(separator = ",")
}
