# 개발 가이드

## 환경 준비

Node 24 이상·Corepack·JDK 17이 필요합니다. `packageManager`에 지정한 Yarn 4를 사용합니다. 프로젝트 루트에서 실행합니다.

```sh
corepack enable
yarn install --immutable
```

Android에는 Android SDK 37이, iOS에는 macOS와 Xcode가 추가로 필요합니다. 앱인토스 전용 프로젝트에는 네이티브 개발 도구가 필요하지 않습니다.

## 앱인토스 실행과 배포물 생성

```sh
yarn dev
yarn dev:toss
yarn build
```

`dev`는 명시적인 mock, `dev:toss`는 실제 브리지를 로컬 네트워크에 제공합니다. 자동 갱신은 없어 소스를 바꾸면 서버를 다시 시작해야 합니다. mock 인가 코드는 서버 인증에 사용하지 않습니다.

`build`는 실제 브리지를 포함한 `__APP_NAME__.ait`를 만듭니다. 업로드 전에 `apps-in-toss.config.ts`의 appName·브랜드·권한 등 필수 정보를 콘솔과 맞춥니다. 콘솔 등록·업로드는 자동으로 수행하지 않습니다.

생성 앱의 직접 npm 런타임 의존성은 `@apps-in-toss/web-framework` 하나입니다. JS 브리지는 Kotlin SDK와 같은 버전의 Maven 리소스에 포함되며 웹 빌드가 자동으로 추출합니다. 별도 npm 브리지 설치는 필요하지 않습니다. TDS는 기본 포함하지 않으며, 사용하려면 앱인토스 호스트에 React/DOM 화면과 관련 의존성을 연결해야 합니다.

## 선택한 플랫폼 실행

선택한 타깃의 디렉터리가 있을 때만 다음 명령을 사용합니다.

```sh
yarn dev:web
yarn build:web
./gradlew :androidApp:assembleDebug
```

iOS는 Xcode에서 `iosApp/ServiceSample.xcodeproj`를 열고 서명 팀을 설정합니다. 독립 웹에는 토스 브리지·mock을 포함하지 않습니다. 초안은 기기·브라우저 출처별로 저장하며 계정별 분리·동기화·암호화는 포함하지 않습니다.

## 검사와 GitHub Actions

```sh
yarn lint
yarn lint:fix
./gradlew :shared-core:jvmTest
```

Kotlin 검사에는 JDK 17이 필요합니다. 기본 CI는 `.github/workflows/ci.yml`에 포함됩니다. GitHub 저장소에 커밋하고 main에 푸시하면 실행되며, main 대상 PR에서도 실행됩니다. 별도의 secret 설정은 필요하지 않습니다.

JS·TS 린트, Kotlin·Gradle 린트, 공통 core JVM 테스트를 실행합니다. 서비스별 JS 테스트·Android·iOS 빌드·기기 실행 검사는 기본 CI에 포함하지 않으며 필요한 시점에 추가합니다.

## 플랫폼 확장

공통 화면과 API 계약은 `shared-ui`·`shared-core`에 유지하고 토스 전용 기능은 `appsInTossApp`에 둡니다. 나중에 플랫폼을 추가하려면 같은 CLI 버전의 템플릿에서 해당 호스트 디렉터리를 가져와 식별자를 바꾸고 Gradle 모듈을 등록합니다. 공통 모듈은 호스트 디렉터리에 따라 Android KMP 플러그인과 iOS 타깃을 활성화합니다.

CLI는 기존 프로젝트를 덮어쓰거나 호스트 추가를 자동으로 처리하지 않습니다. 호스트를 추가한 뒤 해당 플랫폼에서 빌드·실행을 확인해야 합니다.
