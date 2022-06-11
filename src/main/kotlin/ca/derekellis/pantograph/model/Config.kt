package ca.derekellis.pantograph.model

import kotlinx.serialization.Serializable

interface ConfigBase {
    val api: ApiConfig?
    val collector: CollectorConfig
    val tracker: List<TrackerConfig>
}

@Serializable
data class Config(
    override val api: ApiConfig? = null,
    override val collector: CollectorConfig = CollectorConfig(),
    override val tracker: List<TrackerConfig> = emptyList()
) : ConfigBase

@Serializable
data class ApiConfig(val appId: String, val apiKey: String)

@Serializable
data class CollectorConfig(val refresh: Int = 3)

@Serializable
data class TrackerConfig(val route: String, val stops: List<String>)
