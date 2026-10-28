package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class EnvironmentApi internal constructor(private val client: AppsInTossClient) {
    /** Requires [Capability.ENVIRONMENT_INFO]. */
    suspend fun deviceId(): String = value("deviceId")

    /** Requires [Capability.ENVIRONMENT_INFO]. */
    suspend fun groupId(): String = value("groupId")

    /** Requires [Capability.ENVIRONMENT_INFO]. */
    suspend fun environment(): OperationalEnvironment {
        val environment = value("environment")
        return decode("environment") { OperationalEnvironment.entries.first { it.wire == environment } }
    }

    /** Requires [Capability.ENVIRONMENT_INFO]. */
    suspend fun tossAppVersion(): String = value("tossAppVersion")

    /** Current deployment ID; `local` during development. Requires [Capability.ENVIRONMENT_INFO]. */
    suspend fun deploymentId(): String = value("deploymentId")

    /** Scheme URL the app was opened with; later navigation is not reflected. Requires [Capability.ENVIRONMENT_INFO]. */
    suspend fun initialURL(): String = value("initialURL")

    /** Requires [Capability.NETWORK_STATUS]. */
    suspend fun getNetworkStatus(): NetworkStatus {
        val result = client.call("networkStatus")
        return decode("network status") {
            val wire = Json.parseToJsonElement(result).jsonPrimitive.also { require(it.isString) }.content
            // The official wire type is an open string, so unknown values stay usable.
            NetworkStatus.entries.firstOrNull { it.wire == wire } ?: NetworkStatus.UNKNOWN
        }
    }

    /** Toss server time in Unix milliseconds, not the device clock; null when the host gives none. Requires [Capability.SERVER_TIME]. */
    suspend fun getServerTime(): Long? {
        val result = client.call("serverTime")
        return decode("server time") {
            val value = Json.parseToJsonElement(result)
            if (value is JsonNull) null else value.jsonPrimitive.also { require(!it.isString) }.long
        }
    }

    /**
     * True when the Toss app version is at least the minimum for the current platform. Each version is
     * `major.minor.patch`, `always` or `never`; sandbox always returns true. Requires [Capability.ENVIRONMENT_INFO].
     */
    suspend fun isMinVersionSupported(android: String, ios: String): Boolean {
        require(VERSION.matches(android) && VERSION.matches(ios)) { "Version must be major.minor.patch, always or never" }
        val result = client.call(
            "isMinVersionSupported",
            buildJsonObject {
                put("android", android)
                put("ios", ios)
            },
        )
        return decode("min version") { Json.parseToJsonElement(result).jsonPrimitive.also { require(!it.isString) }.boolean }
    }

    private suspend fun value(name: String): String {
        val result = client.call("environmentValue", buildJsonObject { put("name", name) })
        return decode("environment $name") { Json.parseToJsonElement(result).jsonPrimitive.also { require(it.isString) }.content }
    }

    private companion object {
        val VERSION = Regex("""\d+\.\d+\.\d+|always|never""")
    }
}

enum class OperationalEnvironment(internal val wire: String) { TOSS("toss"), SANDBOX("sandbox") }

enum class NetworkStatus(internal val wire: String) {
    OFFLINE("OFFLINE"),
    WIFI("WIFI"),
    CELLULAR_2G("2G"),
    CELLULAR_3G("3G"),
    CELLULAR_4G("4G"),
    CELLULAR_5G("5G"),

    /** iOS cellular without a generation. */
    WWAN("WWAN"),
    UNKNOWN("UNKNOWN"),
}
