package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.serialization.json.*

class LocationApi internal constructor(private val client: AppsInTossClient) {
    /** 현재 위치를 한 번 읽는다. [Capability.LOCATION]가 필요하다. */
    suspend fun getLocation(accuracy: Accuracy): PermissionResult<Location> {
        val result = client.call("locationGet", buildJsonObject { put("accuracy", accuracy.value) })
        return decode("location") { Json.parseToJsonElement(result).jsonObject.permissionResult { it.jsonObject.location() } }
    }

    /**
     * 취소될 때까지 위치 업데이트를 방출한다. 취소하면 공식 cleanup을 실행한다.
     * 권한이 거부되면 [PermissionResult.Denied]를 한 번 방출하고 완료한다.
     * [Capability.LOCATION_UPDATES]가 필요하다.
     */
    fun subscribeLocation(accuracy: Accuracy, timeIntervalMillis: Long, distanceIntervalMeters: Double): Flow<PermissionResult<Location>> {
        require(timeIntervalMillis >= 0 && distanceIntervalMeters.isFinite() && distanceIntervalMeters >= 0)
        val payload = buildJsonObject {
            put("accuracy", accuracy.value)
            put("timeInterval", timeIntervalMillis)
            put("distanceInterval", distanceIntervalMeters)
        }
        return client.stream("locationSubscribe", payload)
            .map { event -> decode("location") { event.permissionResult { it.jsonObject.location() } } }
            .transformWhile {
                emit(it)
                it !is PermissionResult.Denied
            }
    }

    private fun JsonObject.location(): Location {
        val coords = getValue("coords").jsonObject
        return Location(
            accessLocation = (this["accessLocation"] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.let { AccessLocation.valueOf(it.also { p -> require(p.isString) }.content) },
            timestamp = number("timestamp"),
            coords = LocationCoords(
                latitude = coords.number("latitude"),
                longitude = coords.number("longitude"),
                altitude = coords.number("altitude"),
                accuracy = coords.number("accuracy"),
                altitudeAccuracy = coords.number("altitudeAccuracy"),
                heading = coords.number("heading"),
            ),
        )
    }

    private fun JsonObject.number(key: String): Double {
        val value = getValue(key).jsonPrimitive
        require(!value.isString)
        return value.double
    }
}

/** 공식 `Accuracy` 상수다. */
enum class Accuracy(internal val value: Int) {
    LOWEST(1),
    LOW(2),
    BALANCED(3),
    HIGH(4),
    HIGHEST(5),
    BEST_FOR_NAVIGATION(6),
}

/** [accessLocation]은 Android에서만 전달된다. [timestamp]는 epoch 밀리초다. */
data class Location(val accessLocation: AccessLocation?, val timestamp: Double, val coords: LocationCoords)
data class LocationCoords(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Double,
    val altitudeAccuracy: Double,
    val heading: Double,
)
enum class AccessLocation { FINE, COARSE }
