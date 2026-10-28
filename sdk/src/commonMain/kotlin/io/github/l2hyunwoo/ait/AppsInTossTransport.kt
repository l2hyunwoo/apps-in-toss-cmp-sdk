package io.github.l2hyunwoo.ait

/**
 * 호스트 bridge 계약이다. payload와 결과는 JSON 문자열이고, 각 함수는 전달을 멈추는 cleanup을 반환한다.
 * cleanup은 여러 번 호출해도 안전해야 한다.
 */
interface AppsInTossTransport {
    val host: RuntimeHost
    fun supports(capability: Capability): Boolean = host != RuntimeHost.BROWSER

    /** 일회성 요청을 실행한다. [success]와 [failure] 중 정확히 하나만 호출된다. */
    fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit

    /**
     * 이벤트 stream을 연다. `replyId`가 있는 이벤트는 호스트 질문이며
     * `call("reply", {"replyId": ..., "value": ...})`로 답한다.
     */
    fun stream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit): () -> Unit
}
