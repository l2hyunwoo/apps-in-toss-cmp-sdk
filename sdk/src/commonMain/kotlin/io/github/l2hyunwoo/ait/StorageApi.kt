package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class StorageApi internal constructor(private val client: AppsInTossClient) {
    /** 호스트의 영구 저장소를 읽는다. 키가 없으면 null을 반환한다. [Capability.STORAGE]가 필요하다. */
    suspend fun getItem(key: String): String? {
        val result = client.call("storageGet", payload(key))
        return decode("storage") { nullableString(Json.parseToJsonElement(result)) }
    }

    /** 여러 키를 한 번에 읽는다. 없는 키는 null에 대응한다. [Capability.STORAGE_BATCH]가 필요하다. */
    suspend fun getItems(keys: List<String>): Map<String, String?> {
        keys.forEach(::requireKey)
        val result = client.call("storageGetItems", buildJsonObject { putJsonArray("keys") { keys.forEach { add(it) } } })
        return decode("storage items") {
            val obj = Json.parseToJsonElement(result).jsonObject
            keys.associateWith { nullableString(obj.getValue(it)) }
        }
    }

    /** [Capability.STORAGE]가 필요하다. */
    suspend fun setItem(key: String, value: String) {
        client.call("storageSet", payload(key) { put("value", value) })
    }

    /** [Capability.STORAGE_BATCH]가 필요하다. */
    suspend fun setItems(items: Map<String, String>) {
        items.keys.forEach(::requireKey)
        client.call("storageSetItems", buildJsonObject { putJsonObject("items") { items.forEach { (key, value) -> put(key, value) } } })
    }

    /** [Capability.STORAGE]가 필요하다. */
    suspend fun removeItem(key: String) {
        client.call("storageRemove", payload(key))
    }

    /** 저장된 값을 모두 지운다. [Capability.STORAGE]가 필요하다. */
    suspend fun clearItems() {
        client.call("storageClear")
    }

    private fun payload(key: String, extra: JsonObjectBuilder.() -> Unit = {}): JsonObject {
        requireKey(key)
        return buildJsonObject {
            put("key", key)
            extra()
        }
    }

    private fun requireKey(key: String) = require(key.isNotBlank()) { "Storage key cannot be blank" }

    private fun nullableString(value: JsonElement): String? = if (value is JsonNull) null else value.jsonPrimitive.also { require(it.isString) }.content
}
