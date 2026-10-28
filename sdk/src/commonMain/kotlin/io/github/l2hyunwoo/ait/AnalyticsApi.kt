package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

/** Log parameter values must be String, a finite Number, Boolean or null. */
class AnalyticsApi internal constructor(private val client: AppsInTossClient) {
    /** Requires [Capability.ANALYTICS]. */
    suspend fun log(logName: String, logType: LogType, params: Map<String, Any?> = emptyMap()) {
        require(logName.isNotBlank()) { "Log name cannot be blank" }
        client.call(
            "analyticsLog",
            buildJsonObject {
                put("logName", logName)
                put("logType", logType.name.lowercase())
                put("params", params.toLogParams())
            },
        )
    }

    /** Logs a screen view; the host names it after the current path when [logName] is null. Requires [Capability.ANALYTICS]. */
    suspend fun screen(logName: String? = null, params: Map<String, Any?> = emptyMap()) = logger("analyticsScreen", logName, params)

    /** Requires [Capability.ANALYTICS]. */
    suspend fun impression(logName: String? = null, params: Map<String, Any?> = emptyMap()) = logger("analyticsImpression", logName, params)

    /** Requires [Capability.ANALYTICS]. */
    suspend fun click(logName: String? = null, params: Map<String, Any?> = emptyMap()) = logger("analyticsClick", logName, params)

    private suspend fun logger(method: String, logName: String?, params: Map<String, Any?>) {
        require(logName == null || logName.isNotBlank()) { "Log name cannot be blank" }
        require("log_name" !in params) { "Use logName instead of a log_name param" }
        client.call(
            method,
            buildJsonObject {
                logName?.let { put("logName", it) }
                put("params", params.toLogParams())
            },
        )
    }
}

enum class LogType { DEBUG, INFO, WARN, ERROR, EVENT, SCREEN, IMPRESSION, CLICK, POPUP }

private fun Map<String, Any?>.toLogParams() = JsonObject(
    mapValues { (key, value) ->
        when (value) {
            null -> JsonNull

            is String -> JsonPrimitive(value)

            is Boolean -> JsonPrimitive(value)

            is Number -> {
                require(value.toDouble().isFinite()) { "Log param $key must be finite" }
                JsonPrimitive(value)
            }

            else -> throw IllegalArgumentException("Log param $key must be String, Number, Boolean or null")
        }
    },
)
