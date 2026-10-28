# Kotlin SDK

앱인토스 호스트 기능을 Kotlin JS·Wasm에서 호출하는 라이브러리입니다. 공식 웹 SDK 호출은 SDK 내부 [JS 리소스](src/webMain/resources/ait-cmp-bridge)가 담당하며, Kotlin에는 JSON 기반 연결 계약만 노출합니다.

## 제공 API

| API | 역할 |
| --- | --- |
| `createAppsInTossClient()` | 현재 JS 브리지에 연결한 클라이언트 생성 |
| `host`, `supports(capability)` | 실행 환경·기능 지원 여부 확인 |
| `requestTossLogin()` | 인가 코드와 referrer 반환 |
| `getAnonymousKey()` | 익명 사용자 키 반환 |
| `shareText(text)` | 일반 텍스트 공유 |
| `closeScreen()` | 호스트 화면 닫기 |
| `generateHaptic(type)` | 햅틱 피드백(`HapticType`) |
| `getStorageItem(key)`, `setStorageItem(key, value)`, `removeStorageItem(key)` | 토스앱 로컬 저장소 읽기·쓰기·삭제 |
| `getGameProfile()` | 토스게임센터 프로필(`GameProfile.Found`/`NotFound`) |
| `submitLeaderboardScore(score)`, `openLeaderboard()` | 게임센터 리더보드 점수 제출·열기 |
| `events` | 뒤로 가기·홈·안전 영역 이벤트 Flow |
| `close()` | 진행 중 요청·이벤트 연결 정리 |

`RuntimeHost`, `Capability`, `HostEvent`, `ErrorCode`, `AppsInTossException`으로 상태와 실패를 구분합니다. 게임 API는 `Capability.HAPTIC`, `STORAGE`, `GAME_PROFILE`, `LEADERBOARD`로 지원 여부를 먼저 확인합니다. 토스앱 버전이 최소 지원 버전보다 낮아 공식 SDK가 결과를 돌려주지 않으면 `ErrorCode.UNSUPPORTED`로 실패합니다. 리더보드는 콘솔 설정이 없으면 `LEADERBOARD_NOT_FOUND`를 반환합니다. 익명 키는 인증 증명이 아니며, 로그인 인가 코드의 서버 교환은 서비스가 구현해야 합니다.

## 연결과 수명

Maven Central의 `io.github.l2hyunwoo:apps-in-toss-sdk`를 참조합니다. 생성 CLI는 SDK 의존성과 웹 빌드 연결을 함께 설정합니다.

웹 빌드가 SDK의 `web-resources` JAR를 추출하고 CMP 진입점보다 먼저 JS 브리지를 초기화합니다. 별도 npm 브리지 설치는 필요하지 않습니다. UI 호스트마다 클라이언트 하나를 소유합니다. API와 `close()`는 호스트 UI 스레드에서 호출합니다. 호스트 해제 시 `close()`를 호출해야 합니다. 연결 예제는 [앱인토스 진입점](../sample/appsInTossApp/src/webMain/kotlin/Main.kt)에 있습니다.

로그인은 한 번에 하나만 처리하고 기본 제한 시간은 60초입니다. 코루틴 취소·제한 시간 초과 시 늦게 도착한 결과를 무시하고 연결을 해제합니다. 호스트의 로그인 창 자체를 닫지는 않습니다. 미지원 기능·호스트 부재·잘못된 응답을 구분하며 원본 오류의 민감한 내용을 노출하지 않습니다.

JS·Wasm이 호스트 연동 대상이며, JVM 타깃은 계약 테스트용입니다. Android·iOS 앱은 [공통 UI](../sample/shared-ui/README.md)와 [서비스 로직](../sample/shared-core/README.md)을 재사용합니다.
