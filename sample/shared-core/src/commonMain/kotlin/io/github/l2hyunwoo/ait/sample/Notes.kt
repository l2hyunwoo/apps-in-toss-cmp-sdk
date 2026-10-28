package io.github.l2hyunwoo.ait.sample

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Note(val id: String, val title: String, val body: String)

interface NotesRepository {
    suspend fun list(): List<Note>
}

// HTTPS, 인증, 플랫폼별 네트워킹은 transport가 맡는다.
class NotesApiClient(private val getNotesJson: suspend () -> String) : NotesRepository {
    override suspend fun list(): List<Note> = parseNotes(getNotesJson())
}

class DemoNotesRepository : NotesRepository {
    override suspend fun list() = listOf(
        Note("welcome", "공유 화면을 만들어요", "이 화면은 앱인토스, 웹, Android, iOS에서 함께 사용해요."),
        Note("boundary", "필요한 부분만 가져가요", "토스 전용 기능과 디자인은 앱인토스에 남겨요."),
    )
}

interface DraftStorage {
    fun read(id: String): String?
    fun write(id: String, body: String)
}
class MemoryDraftStorage : DraftStorage {
    private val drafts = mutableMapOf<String, String>()
    override fun read(id: String) = drafts[id]
    override fun write(id: String, body: String) {
        drafts[id] = body
    }
}

internal fun parseNotes(json: String): List<Note> = Json.decodeFromString(json)
