# 버전과 지원 범위

## 개발 도구

아래는 2026-10-13에 확인한 구성입니다. 버전의 정본은 [Gradle 버전 카탈로그](../gradle/libs.versions.toml), [Wrapper 설정](../gradle/wrapper/gradle-wrapper.properties), [package.json](../package.json)과 잠금 파일입니다.

| 도구·라이브러리 | 버전 |
| --- | --- |
| Node | 24 이상 |
| Yarn | 4.18.1 |
| JDK | 17 |
| Gradle | 9.8.1 |
| Kotlin·Compose 컴파일러 | 2.4.21 |
| Compose Multiplatform | 1.12.1 |
| Compose Material3 | 1.9.0 |
| Android Gradle Plugin | 9.4.1 |
| Android compile/target SDK | 37 |
| coroutines / serialization | 1.11.0 / 1.11.0 |
| Activity Compose | 1.13.0 |
| 앱인토스 공식 웹 SDK: 예제·생성 앱 | 3.8.0 |

Material3는 Compose Multiplatform과 별도 버전입니다. Android 공유 모듈에는 Android KMP 플러그인, Android 앱에는 AGP 내장 Kotlin을 사용합니다.

Kotlin 2.4.20–2.4.21의 [공식 완전 지원 표](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)는 Gradle 9.7.0·AGP 9.3.1·Xcode 26.4까지입니다. 이 프로젝트는 요청에 따라 더 최신 안정 버전으로 빌드를 확인했습니다. 프로젝트의 빌드 성공과 공급자의 공식 지원 범위는 구분해야 합니다.

## 실행 대상

| 구성 | 대상과 제한 |
| --- | --- |
| Kotlin SDK | JS·Wasm 호스트 연동, JVM 계약 테스트 |
| 앱인토스·독립 웹 예제 | JS/Wasm 호환 배포. 구형 WebView 대체 실행은 미검증 |
| Android 예제 | minSdk 24. 빌드는 확인했으며 기기 UI 검사는 미수행 |
| iOS 예제 | iOS 16.0 설정, arm64 기기·시뮬레이터 타깃. Intel 시뮬레이터 미구성 |
| 생성 CLI | Node 24 이상. Windows 설치·빌드 경로는 미검증 |

실제 토스앱 실행, 구형 OS, 접근성, 성능을 지원 완료로 표시하지 않습니다. iOS에는 ICU 객체의 빌드 대상과 앱 최소 버전 차이 경고가 남아 있습니다. 자세한 근거는 [검증 결과](validation.md)를 참고하세요.

## 공식 웹 SDK와의 호환

Kotlin SDK는 공식 npm 패키지를 직접 참조하지 않습니다. 내부 JS 리소스는 앱에 설치된 공식 SDK를 사용하며 npm peer 범위를 강제하지 않습니다. 공개 함수 형태를 검사한 버전은 3.0.1과 3.8.0입니다. API나 프로토콜이 바뀌면 리소스 수정과 SDK 업데이트가 필요할 수 있습니다. [연결과 버전 정책](architecture.md#공식-웹-sdk-버전과의-관계)을 참고하세요.
