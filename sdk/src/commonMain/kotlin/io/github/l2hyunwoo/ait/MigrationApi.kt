package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class MigrationApi internal constructor(private val client: AppsInTossClient) {
    /**
     * 이전 origin과 현재 origin의 localStorage, IndexedDB, OPFS를 읽는다. 두 `errors` 목록이 모두 비어 있을 때만
     * 이전한다. 일반 JSON이 아닌 레코드는 건너뛰고 그 목록에 보고한다.
     * [Capability.ORIGIN_STORAGE_MIGRATION]이 필요하다.
     */
    suspend fun getOriginStorage(): OriginStorage {
        val result = client.call("migrationOriginStorage")
        return decode("origin storage") {
            val obj = Json.parseToJsonElement(result).jsonObject
            OriginStorage(obj.getValue("previous").jsonObject.toDump(), obj.getValue("current").jsonObject.toDump())
        }
    }
}

data class OriginStorage(val previous: OriginStorageDump, val current: OriginStorageDump)

data class OriginStorageDump(
    val origin: String,
    val localStorage: Map<String, String?>,
    val indexedDB: List<IndexedDbDatabase>,
    val opfs: OpfsSnapshot,
    val errors: List<OriginStorageError>,
)

data class IndexedDbDatabase(val name: String, val version: Long, val objectStores: List<IndexedDbObjectStore>)

/** [keyPathJson]은 key path(문자열 또는 배열)의 JSON 텍스트이고, out-of-line key이면 null이다. */
data class IndexedDbObjectStore(
    val name: String,
    val keyPathJson: String?,
    val autoIncrement: Boolean,
    val indexes: List<IndexedDbIndex>,
    val records: List<IndexedDbRecord>,
)

data class IndexedDbIndex(val name: String, val keyPathJson: String, val multiEntry: Boolean, val unique: Boolean)

/** 키와 값은 JSON 텍스트다. 앱의 타입으로 디코딩한다. */
data class IndexedDbRecord(val keyJson: String, val primaryKeyJson: String, val valueJson: String)

data class OpfsSnapshot(val directories: List<String>, val files: List<OpfsFile>)

/** [data]는 base64로 된 파일 내용이다. [lastModified]는 Unix epoch 밀리초다. */
data class OpfsFile(val path: String, val type: String, val lastModified: Long, val data: String)

/** [storage]는 `localStorage`, `indexedDB`, `opfs` 중 하나다. */
data class OriginStorageError(val storage: String, val message: String)

private fun JsonObject.toDump(): OriginStorageDump {
    val opfs = getValue("opfs").jsonObject
    return OriginStorageDump(
        origin = string("origin"),
        localStorage = getValue("localStorage").jsonObject.mapValues { (_, v) -> if (v is JsonNull) null else v.jsonPrimitive.also { require(it.isString) }.content },
        indexedDB = list("indexedDB") { db ->
            IndexedDbDatabase(db.string("name"), db.long("version"), db.list("objectStores") { it.toStore() })
        },
        opfs = OpfsSnapshot(
            opfs.getValue("directories").jsonArray.map { it.jsonPrimitive.also { p -> require(p.isString) }.content },
            opfs.list("files") { OpfsFile(it.string("path"), it.string("type"), it.long("lastModified"), it.string("data")) },
        ),
        errors = list("errors") { OriginStorageError(it.string("storage"), it.string("message")) },
    )
}

private fun JsonObject.toStore() = IndexedDbObjectStore(
    name = string("name"),
    keyPathJson = getValue("keyPath").takeUnless { it is JsonNull }?.toString(),
    autoIncrement = boolean("autoIncrement"),
    indexes = list("indexes") { IndexedDbIndex(it.string("name"), it.getValue("keyPath").toString(), it.boolean("multiEntry"), it.boolean("unique")) },
    records = list("records") { IndexedDbRecord(it.getValue("key").toString(), it.getValue("primaryKey").toString(), it.getValue("value").toString()) },
)

private fun <T> JsonObject.list(key: String, item: (JsonObject) -> T): List<T> = getValue(key).jsonArray.map { item(it.jsonObject) }

private fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.also { require(!it.isString) }.long

private fun JsonObject.boolean(key: String): Boolean = getValue(key).jsonPrimitive.also { require(!it.isString) }.boolean
