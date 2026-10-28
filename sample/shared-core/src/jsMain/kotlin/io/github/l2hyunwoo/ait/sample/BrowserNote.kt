@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package io.github.l2hyunwoo.ait.sample

import kotlin.js.JsExport

@JsExport
class BrowserNote(val id: String, val title: String, val body: String)

/** fetch·인증 transport는 직접 제공한다. 파싱은 공유 서비스 모델을 쓴다. */
@JsExport
fun decodeNotesResponse(json: String): Array<BrowserNote> = parseNotes(json).map { BrowserNote(it.id, it.title, it.body) }.toTypedArray()
