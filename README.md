# Apps in Toss CMP SDK

Compose Multiplatform(CMP)으로 앱인토스 웹앱을 만들고, 서비스가 성장하면 필요한 화면과 API를 Android·iOS 앱이나 독립 웹서비스에 재사용하는 SDK입니다.

앱인토스 전용 기능은 호스트에 두고, 서비스 로직과 공통 화면은 별도 모듈로 구성합니다. 전체 앱을 한 번에 옮기거나 모든 화면을 공유할 필요 없이, 서비스에 필요한 부분부터 가져갈 수 있습니다.

## 제공하는 기능

- **Kotlin JS/Wasm SDK:** 토스 로그인, 익명 사용자 키, 텍스트 공유, 화면 닫기, 뒤로 가기·홈 이벤트, 안전 영역을 Kotlin API로 제공합니다.
- **SDK 내부 JS 브리지:** 공식 앱인토스 웹 SDK를 연결합니다. Kotlin SDK와 연결 계약을 분리해 공식 SDK의 특정 패치 버전에 대한 의존을 줄입니다.
- **생성 CLI:** 대화형 `create-ait-cmp-app`으로 앱인토스 전용 프로젝트를 만들고 Android·iOS·독립 웹 타깃을 선택할 수 있습니다.
- **공유 예제:** 메모 목록·상세 화면과 데이터 조회·저장 계약을 각 호스트에서 사용하는 예제를 제공합니다.

## 구성

| 구성 요소 | 설명 |
| --- | --- |
| [Kotlin SDK](sdk/README.md) | 앱인토스 기능을 호출하는 Kotlin API |
| [생성 CLI](packages/create-ait-cmp-app/README.md) | 프로젝트 생성 도구 |
| [공통 core](sample/shared-core/README.md) | 데이터 모델·API 클라이언트·저장 계약 |
| [공통 UI](sample/shared-ui/README.md) | 재사용 가능한 Compose 화면 |
| [앱인토스 앱](sample/appsInTossApp/README.md) | 토스 기능을 연결한 웹앱 예제 |
| [독립 웹 앱](sample/webApp/README.md) | 토스 의존성 없이 공통 화면을 사용하는 예제 |
| [Android 앱](sample/androidApp/README.md) | 공통 화면을 실행하는 Android 호스트 |
| [iOS 앱](sample/iosApp/README.md) | 공통 화면을 실행하는 iOS 호스트 |

## 현재 제공 범위

비공식 초기 구현이며 npm·Maven에 배포하지 않았습니다. Kotlin SDK의 앱인토스 연동 대상은 JS·Wasm입니다. Android·iOS 예제는 공통 화면과 서비스 로직을 재사용합니다.

공통 UI는 Compose Material 3을 사용합니다. TDS 패키지·폰트·에셋은 포함하지 않으며, TDS가 필요한 화면은 사용 조건을 확인한 뒤 앱인토스 호스트에 한정해야 합니다. 예제의 한글 폰트는 SIL Open Font License의 Noto Sans KR입니다.

실제 토스앱의 인증·WebView 동작·검수는 별도 검증이 필요합니다. 구현한 범위와 확인한 환경은 [호환성](docs/compatibility.md)과 [검증 결과](docs/validation.md)에 기록했습니다.

## 문서

[문서 안내](docs/index.md)에서 목적에 맞는 가이드를 찾을 수 있습니다.

- [시작하기](docs/getting-started.md): 예제 실행과 프로젝트 생성
- [공유 구조](docs/architecture.md): 공통 코드와 호스트의 경계
- [CLI 사용법](docs/cli.md): 옵션·입력 규칙·생성 후 작업
- [네이티브 연동](docs/native.md): Android·iOS에서 화면 사용
- [개발 가이드](docs/development.md): 린트·테스트·CI·템플릿 유지보수
- [출시 준비](docs/release.md): 앱인토스 배포와 패키지 배포 전 확인 사항

## 라이선스

코드는 [MIT](LICENSE), 예제 폰트는 [SIL Open Font License](licenses/NotoSansKR-OFL.txt)를 따릅니다.
