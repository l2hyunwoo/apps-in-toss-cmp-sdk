# 공유 코드와 호스트의 경계

호스트는 공통 UI에 의존하고, 공통 UI는 서비스 로직의 계약에 의존합니다. 공통 UI는 토스 SDK를 가져오지 않습니다. 공통 core는 Compose·토스·TDS·브라우저 타입을 가져오지 않습니다.

토스 전용 화면은 앱인토스 호스트에 둡니다. 공통 화면은 서비스가 소유한 테마와 플랫폼에 종속되지 않는 상태를 사용합니다. 로그인·공유·저장·화면 이동 동작은 콜백으로 주입합니다. 네이티브 앱과 독립 웹은 개별 화면이나 API 클라이언트만 재사용할 수 있습니다.

현재 Kotlin SDK는 JS·Wasm을 대상으로 하며, 계약 테스트를 위한 JVM 타깃도 제공합니다. 네이티브 호스트에는 토스 SDK를 설치하지 않습니다. 공개 통신 계약은 JSON 문자열을 사용하므로 Promise 객체와 JavaScript 타입이 공통 Kotlin 코드로 넘어가지 않습니다. 클라이언트는 UI 스레드에서만 사용하고, 호스트가 해제될 때 각 클라이언트의 `close()`를 호출해야 합니다.

익명 사용자 키는 식별자이며 인증 증명이 아닙니다. 서비스는 접근 권한을 부여하기 전에 백엔드에서 사용자 신원을 확인해야 합니다. `USER_ID` 지원 여부는 공식 SDK의 버전별 지원 확인 기능으로 검사합니다.

로그인은 한 번에 하나의 요청만 처리합니다. 제한 시간은 60초이며, 응답 타입을 검증하고 취소 시 연결을 정리합니다. 취소하거나 제한 시간을 넘으면 이후 결과를 받지 않지만, 호스트의 로그인 창을 닫지는 않습니다. 인식하지 못한 공식 SDK 오류 코드는 `SDK_FAILURE`로 반환합니다. 토큰·인가 코드·원본 호스트 오류·개인정보는 로그에 남기지 않습니다.

호스트 이벤트는 공식 뒤로 가기·홈 이벤트와 `SafeArea` 초기값·변경 구독을 사용합니다. 독립 웹은 별도 진입점을 사용하며 토스 브리지를 초기화하지 않습니다. 개발용 모의 구현(mock)은 명시적으로 선택하고 운영 배포물에서는 제외합니다. mock에서 성공한 흐름으로 실제 WebView 지원을 판단해서는 안 됩니다.

TDS 연동은 구현하지 않았습니다. 서비스에 필요하다면 React/DOM 연결과 사용 권한이 있는 에셋을 앱인토스 호스트에 한정합니다. 화면 구현이 달라져도 서비스 상태와 동작은 공유할 수 있습니다. 독립 호스트에는 재배포 권한이 있는 에셋을 사용해야 합니다.

공식 근거: [웹 SDK](https://www.npmjs.com/package/@apps-in-toss/web-framework/v/3.8.0), [프로젝트 생성 도구](https://github.com/toss/create-ait-app), [UI/UX 가이드](https://developers-apps-in-toss.toss.im/design/consumer-ux-guide.md), [TDS 사용 조건](https://developers-apps-in-toss.toss.im/design/components.md).


## 공식 웹 SDK 버전과의 관계

Kotlin SDK는 자체 JSON 브리지 계약만 사용합니다. 공식 웹 SDK를 업데이트해도 이 계약이 유지되면 Kotlin 코드를 다시 빌드할 필요가 없습니다. 브리지는 SDK와 같은 버전의 `web-resources` Maven JAR에 포함되며 별도 npm 패키지를 설치하지 않습니다.

앱인토스 호스트의 `prepareAppsInTossBridge` 작업이 리소스를 빌드 디렉터리에 추출합니다. 웹 빌드 스크립트는 운영 빌드에서 `index.mjs`, 명시적인 mock 개발에서 `mock.mjs`를 번들링합니다. 운영 진입점은 앱에 설치된 `@apps-in-toss/web-framework`를 사용하고 `globalThis.aitCmpBridge`를 초기화합니다. 독립 웹·네이티브에는 이 리소스를 넣지 않습니다.

생성 앱의 직접 npm 런타임 의존성은 `@apps-in-toss/web-framework` 하나입니다. 현재 고정 버전 3.8.0은 재현 가능한 설치를 위한 선택입니다. SDK는 npm peer 버전 범위를 강제하지 않습니다. 3.0.1과 3.8.0의 공개 함수 형태, 기능 부재와 명시적 미지원 처리를 검사하지만 모든 중간 버전이나 실제 토스앱 동작을 보장하지는 않습니다.

공식 SDK의 함수 이름·인자·응답이나 호스트 프로토콜이 바뀌면 내부 JS 리소스 수정과 SDK 업데이트가 필요할 수 있습니다. 기능 존재 검사로 응답 의미의 변경까지 감지할 수는 없습니다.

## TDS 연결

현재 공통 화면은 Compose Material 3으로 렌더링하며 TDS를 기본 설치하지 않습니다. [공식 TDS 설치 안내](https://tossmini-docs.toss.im/tds-mobile/start/)의 TDS는 React·ReactDOM·Emotion과 Provider 설정이 필요합니다. 패키지 설치만으로 CMP Canvas 화면에 TDS가 적용되지는 않습니다. 실제 TDS를 사용하려면 앱인토스 호스트에 React/DOM 화면을 연결하고 해당 의존성을 추가해야 합니다.

[공식 커뮤니티의 2025년 12월 답변](https://techchat-apps-in-toss.toss.im/t/tds/1820/2)은 TDS가 필수가 아니라고 안내합니다. 다만 [이전 모달 관련 답변](https://techchat-apps-in-toss.toss.im/t/tds/822/2)과 조건이 달라, 서비스 출시 전 [비게임 출시 가이드](https://developers-apps-in-toss.toss.im/checklist/app-nongame.html)와 실제 검수 기준을 확인해야 합니다. 이 예제의 CMP 화면은 실제 검수 승인을 확인하지 않았습니다. TDS 에셋과 컴포넌트는 사용 조건을 확인하고 앱인토스 호스트에 한정합니다.
