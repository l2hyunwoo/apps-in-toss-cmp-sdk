# 프로젝트 생성 CLI 사용법

`create-ait-cmp-app`은 앱인토스를 지원하는 Compose Multiplatform 프로젝트를 생성합니다. Node.js 24 이상과 Corepack이 필요합니다. 생성 앱은 Yarn 4.18.1을 사용합니다. `corepack enable`로 yarn 명령을 활성화합니다. npm 패키지에 템플릿과 Gradle Wrapper를 포함하며, 생성할 때 Git 브랜치에서 템플릿을 내려받지 않습니다.

아직 npm에 배포하지 않았습니다. 아래 `yarn dlx` 명령은 같은 이름으로 패키지를 배포한 뒤 사용할 수 있습니다. 로컬 저장소에서는 `yarn dlx create-ait-cmp-app` 대신 `node packages/create-ait-cmp-app/cli.js`를 사용합니다.

## 대화형 생성

```sh
yarn dlx create-ait-cmp-app my-app
```

생성 CLI는 타깃·Kotlin 패키지·앱인토스 appName·표시 이름을 입력받고, 파일 생성 전에 요약을 보여줍니다. `yes`를 입력하면 생성하고, 확인 입력을 비워 두면 취소합니다. 옵션으로 제공한 값은 다시 묻지 않으며 잘못된 입력은 같은 질문에서 수정할 수 있습니다. 확인 전에 Ctrl+C를 누르면 파일을 만들지 않고 취소합니다.

앱인토스는 필수 타깃이며 기본으로 선택하는 유일한 타깃입니다. 다른 호스트도 필요하면 질문에서 선택하거나 `--targets`로 지정합니다.

```sh
yarn dlx create-ait-cmp-app my-app --targets apps-in-toss,android,ios,web
```

선택하지 않은 호스트의 디렉터리는 생성하지 않습니다. 공통 Gradle 모듈은 생성된 호스트 디렉터리를 기준으로 플랫폼 타깃을 활성화합니다. 현재 미배포 버전은 JS 리소스를 포함한 로컬 SDK Maven 바이너리를 생성 앱에 포함합니다. SDK 구현 소스는 복사하지 않습니다. 이후 배포 버전에서는 정식 의존성 좌표로 대체할 예정입니다.

## 자동화 환경에서 생성

터미널 입력이 없는 환경에서는 `--yes`, 생성 경로, 세 가지 메타데이터 옵션을 모두 지정합니다.

```sh
yarn dlx create-ait-cmp-app my-app --yes \
  --package io.example.myapp \
  --app-name my-app \
  --display-name '내 앱' \
  --skip-install
```

`--yes`는 모든 질문과 확인을 생략합니다. 필수 값이 없으면 파일을 쓰기 전에 실패합니다. `--skip-install`이 없으면 생성 디렉터리에서 `corepack yarn install --immutable`을 실행합니다. 설치에 실패하면 생성 파일을 보존하고 0이 아닌 종료 코드를 반환합니다. 생성 디렉터리에서 `yarn install --immutable`을 실행해 다시 시도할 수 있습니다. `--skip-install`은 포함된 `yarn.lock`과 `.yarnrc.yml`을 유지하고 설치 명령을 안내합니다.

| 옵션 | 동작 |
| --- | --- |
| `--package <id>` | Kotlin 패키지·Android application ID·iOS bundle ID 설정 |
| `--app-name <name>` | 앱인토스 콘솔 appName 설정 |
| `--display-name <name>` | 앱 표시 이름 설정 |
| `--targets <list>` | `apps-in-toss,android,ios,web` 중 타깃을 쉼표로 구분해 선택 |
| `--yes` | 메타데이터를 명시적으로 받고 질문 생략 |
| `--skip-install` | Yarn 의존성을 설치하지 않고 파일만 생성 |
| `--help` | 명령 사용법 출력 |
| `--version` | 생성 CLI 버전 출력 |

## 이름 규칙과 기존 파일 보호

생성 경로의 마지막 디렉터리 이름을 npm·Gradle 프로젝트 이름으로 사용합니다. 소문자로 시작하고 소문자·숫자·하이픈만 포함해야 하며, 최대 길이는 80자입니다. 상위 디렉터리 이름에는 공백이 있어도 됩니다.

패키지는 점으로 구분한 두 개 이상의 구간으로 구성합니다. 각 구간은 영문자로 시작하고 영문자·숫자만 포함해야 합니다. Kotlin 예약어는 사용할 수 없습니다. Android·iOS 식별자로도 사용할 수 있도록 제한한 규칙입니다. appName은 프로젝트 이름과 같은 형식을 사용합니다. 표시 이름에는 유니코드와 문장부호를 사용할 수 있지만 제어 문자와 100자를 넘는 값은 허용하지 않습니다.

대상 경로는 없거나 비어 있어야 하며, `.git`만 있는 디렉터리도 허용합니다. 생성 경로의 심볼릭 링크·기존 파일·비어 있지 않은 디렉터리는 거부합니다. 덮어쓰기용 `--force` 옵션은 제공하지 않습니다. Git 초기화·커밋·콘솔 앱 등록·배포는 수행하지 않습니다. 생성 중에는 다른 프로세스가 대상 경로를 수정하면 안 됩니다.

## 생성 후 실행

생성된 디렉터리에서 실행합니다.

```sh
yarn lint
yarn dev
yarn build
```

일반 브라우저 개발에는 명시적인 mock을 사용합니다. `yarn dev:toss`는 샌드박스 실행을 위한 실제 앱인토스 브리지를 로컬 네트워크에 제공합니다. 운영 빌드 전에 콘솔 appName·로고·필수 브랜드 정보를 설정합니다. 생성 단계에서는 appName 소유권이나 콘솔 사용 가능 여부를 확인하지 않습니다.

Android에서는 JDK 17과 Android SDK를 설치한 뒤 실행합니다.

```sh
./gradlew :androidApp:assembleDebug
```

iOS에서는 macOS의 Xcode로 `iosApp` 안의 프로젝트를 엽니다. 실기기 빌드 전에 서명 팀을 설정해야 합니다. 독립 웹 타깃은 `yarn dev:web`과 `yarn build:web`을 사용하며, 운영 빌드에 토스 SDK나 브라우저 mock을 포함하지 않습니다. 생성 CLI는 JDK·Android SDK·Xcode 등 플랫폼 도구를 설치하지 않습니다.

## 생성 앱 문서와 CI

생성 앱의 README는 앱 기능과 공통 모듈을 설명합니다. `docs/development.md`에서 실행·검사·CI·플랫폼 추가 방법을 안내합니다. `.github/workflows/ci.yml`은 main 푸시·main 대상 PR에서 JS·TS 린트, Kotlin·Gradle 린트, 공통 core JVM 테스트를 실행합니다. 서비스별 JS 테스트는 추가한 뒤 연결합니다.

템플릿을 수정하거나 배포용 CLI를 검증하려면 [개발 가이드](development.md)를 참고하세요.
