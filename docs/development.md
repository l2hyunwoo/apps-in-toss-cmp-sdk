# 개발 가이드

## 환경과 버전 관리

[시작하기](getting-started.md)에 따라 의존성을 설치합니다. Kotlin·Compose·Android 버전은 `gradle/libs.versions.toml`, Gradle 배포 버전은 Wrapper, JS 도구 버전은 `package.json`과 `yarn.lock`에서 관리합니다. [호환성 표](compatibility.md)를 확인한 뒤 버전을 함께 갱신합니다.

Kotlin/JS·Wasm의 내부 패키지 관리 도구는 Gradle 플러그인이 관리합니다. 프로젝트 루트의 Yarn 4 설정과 별개입니다.

## 린트와 테스트

저장소 루트에서 실행합니다. Kotlin 검사에는 JDK 17이 필요합니다.

```sh
yarn lint
yarn lint:js
yarn lint:kotlin
yarn lint:fix
yarn test
./gradlew :sdk:jvmTest :sample:shared-core:jvmTest
```

JS·TS는 ESLint와 typescript-eslint, Kotlin·Gradle 스크립트는 ktlint로 검사합니다. `lint:fix`는 자동 수정 가능한 항목을 변경하므로 실행 후 diff를 검토합니다. Compose 함수명과 기존 와일드카드 import는 허용하며 Kotlin import는 사전순으로 정렬합니다.

`yarn test`는 공유 의존성 경계, JS 브리지와 생성 CLI를 검사합니다. SDK·공통 core 계약 테스트는 JVM에서 실행합니다. 빌드 산출물·외부 바이너리·템플릿 사본은 중복 린트하지 않습니다.

네이티브·웹 빌드는 [시작하기](getting-started.md)와 [네이티브 연동](native.md)의 명령을 사용합니다. 웹 화면 검사는 Aside로 수행합니다. 빌드·mock 성공만으로 실제 토스앱 동작이나 접근성·성능을 확인했다고 판단하지 않습니다.

## GitHub Actions

`main` 푸시와 `main` 대상 PR, 수동 실행을 지원합니다.

| 워크플로 | 검사 |
| --- | --- |
| [CI](../.github/workflows/ci.yml) | JS·TS 린트와 브리지·CLI 테스트, Kotlin·Gradle 린트와 SDK·공통 core JVM 테스트 |
| [Generated app CI](../.github/workflows/generated-app-ci.yml) | 배포용 CLI 압축 패키지로 기본·전체 타깃 앱 생성, 자동 설치, 워크플로 복원, 생성 앱의 린트·core 테스트 |

Node 24·Yarn 4·JDK 17·Gradle Wrapper를 사용합니다. 같은 ref의 이전 실행은 새 실행이 시작되면 취소합니다. 저장소 권한은 읽기만 허용하고 액션은 SHA로 고정합니다. PR에서는 Gradle 캐시를 읽기만 합니다.

Android·iOS 빌드와 브라우저 실행 검사는 현재 CI에 포함하지 않습니다. 실행 결과의 범위는 [검증 결과](validation.md)를 참고하세요.

## 템플릿 갱신

SDK나 샘플 앱·생성 문서를 수정하면 실행합니다.

```sh
yarn template
```

[scripts/template.mjs](../scripts/template.mjs)는 JS 리소스를 포함한 SDK 바이너리를 준비하고 샘플 모듈을 `packages/create-ait-cmp-app/templates/app`에 복사합니다. 생성 앱의 루트 README와 개발 가이드는 이 스크립트가 작성합니다. 템플릿 사본만 수정하면 다음 갱신 때 사라집니다.

템플릿에는 정해진 필드에서만 `__PROJECT_NAME__`, `__PACKAGE_NAME__`, `__APP_NAME__`, `__DISPLAY_NAME__` 토큰을 사용합니다. 소스·JSON은 큰따옴표 문자열, XML·plist는 텍스트나 속성값에 둡니다. 파일 확장자별로 CLI가 대체 값을 이스케이프합니다.

| 템플릿 이름 | 생성 앱 이름 | 이유 |
| --- | --- | --- |
| `_gitignore` | `.gitignore` | 패키징 제외 방지 |
| `_yarn.lock` | `yarn.lock` | 템플릿과 생성 앱의 프로젝트 경계 분리 |
| `_yarnrc.yml` | `.yarnrc.yml` | Yarn 패키징 제외 방지 |
| `_github` | `.github` | 워크플로 패키징 제외 방지 |

CLI는 프로젝트 이름에 맞게 Yarn workspace locator와 항목 순서를 바꿉니다. `publishConfig.executableFiles`로 Wrapper 실행 권한을 유지합니다. 웹 타깃을 선택하지 않으면 웹용 스크립트를 제거합니다.

갱신 후 실제 압축 패키지에서 기본·전체 타깃 앱을 생성해 설치·린트·core 테스트를 확인합니다. 모듈 README도 생성 앱의 경로와 명령에 맞게 바뀌는지, 문서 링크가 선택하지 않은 타깃을 참조하지 않는지 확인합니다. 지원을 표시하려는 플랫폼의 빌드·실행 검사도 필요합니다.
