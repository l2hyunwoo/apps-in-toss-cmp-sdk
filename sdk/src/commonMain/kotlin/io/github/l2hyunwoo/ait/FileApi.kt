package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*

class FileApi internal constructor(private val client: AppsInTossClient) {
    /** Opens base64 PDF [data] in the native viewer and returns when the user closes it. Requires [Capability.PDF_VIEWER]. */
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

    /** Saves base64 [data] to the device; [fileName] must include the extension. Requires [Capability.SAVE_FILE]. */
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
