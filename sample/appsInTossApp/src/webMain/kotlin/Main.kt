import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import io.github.l2hyunwoo.ait.*
import io.github.l2hyunwoo.ait.sample.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "compose-root") {
        val client = remember { createAppsInTossClient() }
        val scope = rememberCoroutineScope()
        val navigation = remember { ServiceNavigation() }
        val storage = remember { BrowserDraftStorage() }
        var status by remember { mutableStateOf("앱인토스 전용 영역이에요. 현재 환경: ${client.host}") }
        var insets by remember { mutableStateOf(SafeAreaInsets(0.0, 0.0, 0.0, 0.0)) }
        DisposableEffect(client) { onDispose { client.close() } }
        LaunchedEffect(client) {
            if (client.host != RuntimeHost.BROWSER) {
                try {
                    client.events.collect { event ->
                        when (event) {
                            is HostEvent.SafeArea -> insets = event.insets
                            HostEvent.Back -> if (!navigation.back()) client.screen.close()
                            HostEvent.Home -> navigation.selectedId = null
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    status = "호스트 이벤트를 연결하지 못했어요."
                }
            }
        }
        // The opaque Toss navigation bar already sits above the WebView, so the status-bar top inset would add a blank band.
        Box(Modifier.padding(bottom = insets.bottom.dp, start = insets.left.dp, end = insets.right.dp)) {
            ServiceApp(state = navigation, storage = storage, showBackButton = client.host == RuntimeHost.MOCK, onShare = { text ->
                scope.launch {
                    try {
                        client.share.sendMessage(text)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        status = "공유하지 못했어요."
                    }
                }
            }, hostContent = {
                Text(status)
                Button(enabled = client.supports(Capability.TOSS_LOGIN), onClick = {
                    scope.launch {
                        status = try {
                            client.auth.login()
                            "인가 결과를 받았어요. 실제 서비스는 서버에서 교환해야 해요."
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: AppsInTossException) {
                            "로그인을 완료하지 못했어요: ${e.code}"
                        }
                    }
                }) { Text("토스로 로그인해요") }
            })
        }
    }
}
