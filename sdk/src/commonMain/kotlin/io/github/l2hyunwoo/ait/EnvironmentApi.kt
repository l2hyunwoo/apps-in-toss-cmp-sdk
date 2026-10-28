package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class EnvironmentApi internal constructor(private val client: AppsInTossClient) {
    /** [Capability.ENVIRONMENT_INFO]가 필요하다. */
    suspend fun deviceId(): String = value("deviceId")

    /** [Capability.ENVIRONMENT_INFO]가 필요하다. */
    suspend fun groupId(): String = value("groupId")

    /** [Capability.ENVIRONMENT_INFO]가 필요하다. */
    suspend fun environment(): OperationalEnvironment {
        val environment = value("environment")
        return decode("environment") { OperationalEnvironment.entries.first { it.wire == environment } }
    }

    /** [Capability.ENVIRONMENT_INFO]가 필요하다. */
    suspend fun tossAppVersion(): String = value("tossAppVersion")

    /** 현재 배포 ID다. 개발 중에는 `local`이다. [Capability.ENVIRONMENT_INFO]가 필요하다. */
    suspend fun deploymentId(): String = value("deploymentId")

    /** 앱을 연 scheme URL이다. 이후 이동은 반영하지 않는다. [Capability.ENVIRONMENT_INFO]가 필요하다. */
    suspend fun initialURL(): String = value("initialURL")

    /** [Capability.NETWORK_STATUS]가 필요하다. */
    suspend fun getNetworkStatus(): NetworkStatus {
        val result = client.call("networkStatus")
        return decode("network status") {
            val wire = Json.parseToJsonElement(result).jsonPrimitive.also { require(it.isString) }.content
            // 공식 wire 타입은 열린 문자열이므로 알 수 없는 값도 그대로 쓸 수 있다.
            NetworkStatus.entries.firstOrNull { it.wire == wire } ?: NetworkStatus.UNKNOWN
        }
    }

    /** 기기 시계가 아닌 토스 서버 시간(Unix 밀리초)이다. 호스트가 주지 않으면 null이다. [Capability.SERVER_TIME]가 필요하다. */
    suspend fun getServerTime(): Long? {
        val result = client.call("serverTime")
        return decode("server time") {
            val value = Json.parseToJsonElement(result)
            if (value is JsonNull) null else value.jsonPrimitive.also { require(!it.isString) }.long
        }
    }

    /**
     * 토스 앱 버전이 현재 플랫폼의 최소 버전 이상이면 true다. 각 버전은
     * `major.minor.patch`, `always`, `never` 중 하나이고, sandbox는 항상 true를 반환한다. [Capability.ENVIRONMENT_INFO]가 필요하다.
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

    /** 세대 정보가 없는 iOS 셀룰러다. */
    WWAN("WWAN"),
    UNKNOWN("UNKNOWN"),
}
