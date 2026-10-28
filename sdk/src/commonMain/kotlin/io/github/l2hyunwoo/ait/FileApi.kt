package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*

class FileApi internal constructor(private val client: AppsInTossClient) {
    /** base64 PDF [data]를 네이티브 뷰어로 열고 사용자가 닫으면 반환한다. [Capability.PDF_VIEWER]가 필요하다. */
    suspend fun openPDFViewer(data: String, filename: String? = null) {
        require(data.isNotBlank()) { "PDF data cannot be blank" }
        require(filename == null || filename.isNotBlank()) { "File name cannot be blank" }
        val event = client.stream(
            "pdfViewer",
            buildJsonObject {
                put("data", data)
                filename?.let { put("filename", it) }
            },
        ).first()
        decode("PDF viewer") { require(event.string("type") == "closed") }
    }

    /** base64 [data]를 기기에 저장한다. [fileName]에는 확장자가 있어야 한다. [Capability.SAVE_FILE]가 필요하다. */
    suspend fun saveBase64(data: String, fileName: String, mimeType: String) {
        require(data.isNotBlank() && fileName.isNotBlank() && mimeType.isNotBlank()) { "Data, file name and MIME type are required" }
        client.call(
            "fileSaveBase64",
            buildJsonObject {
                put("data", data)
                put("fileName", fileName)
                put("mimeType", mimeType)
            },
        )
    }
}
