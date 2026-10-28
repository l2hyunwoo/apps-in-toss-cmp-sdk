package io.github.l2hyunwoo.ait

class ReviewApi internal constructor(private val client: AppsInTossClient) {
    /** 사용자에게 미니앱 리뷰를 요청한다. [Capability.REVIEW]가 필요하다. */
    suspend fun request() {
        client.call("reviewRequest")
    }
}
