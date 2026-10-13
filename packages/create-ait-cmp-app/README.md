# create-ait-cmp-app

앱인토스용 Compose Multiplatform 프로젝트를 대화형으로 만드는 CLI입니다. 앱인토스 전용 앱으로 시작하고 Android·iOS·독립 웹 타깃을 함께 선택할 수 있습니다.

## 생성하는 프로젝트

`shared-core`에 서비스 모델·API 클라이언트·저장 계약을, `shared-ui`에 공통 화면을 둡니다. `appsInTossApp`은 토스 연동을 담당합니다. 선택한 타깃의 호스트 디렉터리, Gradle Wrapper, Yarn 잠금 파일, 린트 설정, 기본 GitHub Actions CI를 포함합니다.

현재 JS 리소스를 포함한 SDK Maven 바이너리를 템플릿에 포함하므로 미배포 원격 SDK에 의존하지 않습니다. SDK 구현 소스는 복사하지 않습니다.

## 사용

Node 24 이상과 Corepack이 필요합니다. 현재 npm 미배포 상태이므로 저장소 루트에서 실행합니다.

```sh
node packages/create-ait-cmp-app/cli.js my-app
```

배포 후에는 `yarn dlx create-ait-cmp-app my-app`으로 사용할 수 있습니다. 타깃·Kotlin 패키지·콘솔 appName·표시 이름을 질문하고, 생성 전에 요약과 확인을 받습니다. 기본 타깃은 앱인토스만 선택합니다.

자동화 환경에서는 입력값을 명시합니다.

```sh
node packages/create-ait-cmp-app/cli.js my-app --yes   --package io.example.myapp --app-name my-app   --display-name '내 앱' --targets apps-in-toss,android,ios,web
```

`--skip-install`로 설치를 생략할 수 있습니다. 기본 설치는 프로젝트에 지정한 Yarn 4의 `install --immutable`을 사용합니다. 기존 파일·비어 있지 않은 경로·심볼릭 링크를 보호하며 덮어쓰기 옵션은 제공하지 않습니다.

Git 초기화·커밋·앱인토스 콘솔 등록·배포·네이티브 도구 설치는 수행하지 않습니다. 생성 앱의 README는 앱 구성을, 개발 가이드는 실행·검사·CI 방법을 안내합니다. [전체 옵션과 입력 규칙](https://github.com/l2hyunwoo/apps-in-toss-cmp-sdk/blob/main/docs/cli.md)을 참고하세요.
