package ca.derekellis.pantograph.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlCData
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.time.LocalDateTime

data class FeedItem(
     val title: String,
     val pubDate: LocalDateTime,
     val category: String,
     val description: String,
     val guid: String,
     val link: String,
     val hash: String,
)
