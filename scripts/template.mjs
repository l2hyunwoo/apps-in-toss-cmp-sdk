import { cp, mkdir, readFile, writeFile, rm, readdir, chmod, rename } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';
import { createHash } from 'node:crypto';
import { getNpmTag } from './release-version.mjs';
const root = process.cwd();
const releaseVersion = process.env.RELEASE_VERSION;
if (releaseVersion) getNpmTag(releaseVersion);
const sdkVersion = releaseVersion ?? JSON.parse(await readFile('packages/create-ait-cmp-app/package.json','utf8')).version;
function run(command,args) {
  const r=spawnSync(command,args,{cwd:root,stdio:'inherit',shell:process.platform==='win32'});
  if(r.error)throw r.error;if(r.status!==0)process.exit(r.status??1);
}
await rm(join(root,'build/template-maven'),{recursive:true,force:true});
run(process.platform==='win32'?'gradlew.bat':'./gradlew',[':sdk:publishAllPublicationsToTemplateRepository', ...(releaseVersion ? [`-PreleaseVersion=${releaseVersion}`] : []), '--console=plain']);
const dest=join(root,'packages/create-ait-cmp-app/templates/app');
await rm(dest,{recursive:true,force:true});await mkdir(dest,{recursive:true});
for(const file of ['LICENSE','.editorconfig','eslint.config.mjs','scripts/lint-kotlin.mjs','.yarnrc.yml','licenses','gradlew','gradlew.bat','gradle','gradle.properties','build.gradle.kts','scripts/web.mjs']) {
  await cp(join(root,file),join(dest,file),{recursive:true});
}
await writeFile(join(dest,'_gitignore'), (await readFile('.gitignore','utf8')).replace('packages/create-ait-cmp-app/templates/app/vendor/\n',''));
await writeFile(join(dest,'gradle.properties'), (await readFile('gradle.properties','utf8')).replace(/^sdkVersion=.*\r?\n?/gm,'').trimEnd()+'\n');
// Yarn pack omits .github; restore the directory name when scaffolding.
await mkdir(join(dest,'_github/workflows'),{recursive:true});
const ci=(await readFile('.github/workflows/ci.yml','utf8'))
  .replace('      - name: JS 테스트\n        run: yarn test\n','')
  .replace('name: JS 린트·테스트','name: JS 린트')
  .replace(':sdk:jvmTest :sample:shared-core:jvmTest',':shared-core:jvmTest');
await writeFile(join(dest,'_github/workflows/ci.yml'),ci);
for(const module of ['shared-core','shared-ui','appsInTossApp','webApp','androidApp','iosApp']) {
  await cp(join(root,'sample',module),join(dest,module),{recursive:true,filter:p=>!p.split('/').some(s=>['build','.gradle','xcuserdata'].includes(s))});
}
if (!releaseVersion) await cp('build/template-maven',join(dest,'vendor/maven'),{recursive:true,filter:p=>!p.includes('-sources.jar')});
// The template ships binaries only; do not advertise omitted sources to Maven consumers.
async function stripSourceMetadata(directory) {
  for (const e of await readdir(directory, {withFileTypes:true})) {
    const p = join(directory, e.name);
    if (e.isDirectory()) { await stripSourceMetadata(p); continue; }
    let content;
    if (e.name.endsWith('.module')) {
      const metadata = JSON.parse(await readFile(p, 'utf8'));
      metadata.variants = metadata.variants.filter(v => v.attributes?.['org.gradle.category'] !== 'documentation');
      content = JSON.stringify(metadata, null, 2) + '\n';
    } else if (e.name === 'maven-metadata.xml') {
      content = (await readFile(p, 'utf8')).replace(/<snapshotVersion>[\s\S]*?<\/snapshotVersion>/g,
        value => value.includes('<classifier>sources</classifier>') ? '' : value);
    }
    if (content !== undefined) {
      await writeFile(p, content);
      for (const algorithm of ['md5', 'sha1', 'sha256', 'sha512']) {
        await writeFile(`${p}.${algorithm}`, createHash(algorithm).update(content).digest('hex'));
      }
    }
  }
}
if (!releaseVersion) await stripSourceMetadata(join(dest, 'vendor/maven'));
const tooling=JSON.parse(await readFile('package.json','utf8'));
const lintScripts=Object.fromEntries(Object.entries(tooling.scripts).filter(([name])=>name.startsWith('lint')));
const lintDependencies=Object.fromEntries(Object.entries(tooling.devDependencies).filter(([name])=>name!=='toss-sdk-min'));
const pkg={name:'ait-cmp-template',private:true,packageManager:'yarn@4.18.1',type:'module',engines:{node:'>=24'},scripts:{dev:'node scripts/web.mjs dev','dev:toss':'node scripts/web.mjs dev:toss',build:'node scripts/web.mjs build','dev:web':'node scripts/web.mjs dev:web','build:web':'node scripts/web.mjs build:web',...lintScripts},dependencies:{'@apps-in-toss/web-framework':tooling.dependencies['@apps-in-toss/web-framework']},devDependencies:lintDependencies};
await writeFile(join(dest,'package.json'),JSON.stringify(pkg,null,2)+'\n');
await writeFile(join(dest,'settings.gradle.kts'),`pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
${releaseVersion ? '' : '        maven { url = uri("vendor/maven") }\n'}        google()
        mavenCentral()
    }
}
rootProject.name = "__PROJECT_NAME__"
include(":shared-core", ":shared-ui", ":appsInTossApp")
if (file("webApp").isDirectory) include(":webApp")
if (file("androidApp").isDirectory) include(":androidApp")
`);
await writeFile(join(dest,'apps-in-toss.config.ts'),`import { defineConfig } from '@apps-in-toss/web-framework/config';
export default defineConfig({appName:"__APP_NAME__",brand:{primaryColor:"#3469D9"},permissions:[],navigationBar:{withBackButton:true,withHomeButton:true,theme:"light"},webBundleDir:"dist/apps-in-toss"});
`);
await writeFile(join(dest,'README.md'),`# __DISPLAY_NAME__

앱인토스에서 사용하는 Compose Multiplatform 메모 서비스입니다. 메모 목록을 보고 내용을 수정·저장할 수 있으며, 공통 화면과 서비스 로직을 다른 앱·웹에 재사용할 수 있습니다.

## 기능

- 메모 목록·상세·편집 화면
- 기기에 초안 저장과 텍스트 공유
- 앱인토스 전용 로그인·호스트 이벤트·안전 영역 연결

## 구성

| 모듈 | 역할 |
| --- | --- |
| [shared-core](shared-core/README.md) | 데이터 모델·조회 API·저장 계약 |
| [shared-ui](shared-ui/README.md) | 재사용 가능한 Compose 화면 |
| [appsInTossApp](appsInTossApp/README.md) | 토스 연동과 앱인토스 진입점 |

Android·iOS·독립 웹 호스트는 프로젝트 생성 시 선택한 경우에 포함됩니다. 공통 UI는 Compose Material 3을 사용하며 TDS는 포함하지 않습니다.

## 현재 제공 범위

조회는 데모 데이터를 사용하고 초안은 기기에 저장합니다. 운영 서버·계정 연동·서버 인증 교환은 별도로 연결해야 합니다. 개발용 mock의 로그인 결과는 인증에 사용할 수 없습니다.

[개발 가이드](docs/development.md)에서 실행·검사·GitHub Actions CI와 플랫폼 확장 방법을 확인할 수 있습니다.
`);
await mkdir(join(dest,'docs'),{recursive:true});
await writeFile(join(dest,'docs/development.md'),`# 개발 가이드

## 환경 준비

Node 24 이상·Corepack·JDK 17이 필요합니다. \`packageManager\`에 지정한 Yarn 4를 사용합니다. 프로젝트 루트에서 실행합니다.

\`\`\`sh
corepack enable
yarn install --immutable
\`\`\`

Android에는 Android SDK 37이, iOS에는 macOS와 Xcode가 추가로 필요합니다. 앱인토스 전용 프로젝트에는 네이티브 개발 도구가 필요하지 않습니다.

## 앱인토스 실행과 배포물 생성

\`\`\`sh
yarn dev
yarn dev:toss
yarn build
\`\`\`

\`dev\`는 명시적인 mock, \`dev:toss\`는 실제 브리지를 로컬 네트워크에 제공합니다. 자동 갱신은 없어 소스를 바꾸면 서버를 다시 시작해야 합니다. mock 인가 코드는 서버 인증에 사용하지 않습니다.

\`build\`는 실제 브리지를 포함한 \`__APP_NAME__.ait\`를 만듭니다. 업로드 전에 \`apps-in-toss.config.ts\`의 appName·브랜드·권한 등 필수 정보를 콘솔과 맞춥니다. 콘솔 등록·업로드는 자동으로 수행하지 않습니다.

생성 앱의 직접 npm 런타임 의존성은 \`@apps-in-toss/web-framework\` 하나입니다. JS 브리지는 Kotlin SDK와 같은 버전의 Maven 리소스에 포함되며 웹 빌드가 자동으로 추출합니다. 별도 npm 브리지 설치는 필요하지 않습니다. 정식 CLI 배포물은 같은 버전의 SDK를 Maven Central에서 가져오고, 로컬 개발용 CLI는 \`vendor/maven\`에 SDK를 포함합니다. TDS는 기본 포함하지 않으며, 사용하려면 앱인토스 호스트에 React/DOM 화면과 관련 의존성을 연결해야 합니다.

## 선택한 플랫폼 실행

선택한 타깃의 디렉터리가 있을 때만 다음 명령을 사용합니다.

\`\`\`sh
yarn dev:web
yarn build:web
./gradlew :androidApp:assembleDebug
\`\`\`

iOS는 Xcode에서 \`iosApp/ServiceSample.xcodeproj\`를 열고 서명 팀을 설정합니다. 독립 웹에는 토스 브리지·mock을 포함하지 않습니다. 초안은 기기·브라우저 출처별로 저장하며 계정별 분리·동기화·암호화는 포함하지 않습니다.

## 검사와 GitHub Actions

\`\`\`sh
yarn lint
yarn lint:fix
./gradlew :shared-core:jvmTest
\`\`\`

Kotlin 검사에는 JDK 17이 필요합니다. 기본 CI는 \`.github/workflows/ci.yml\`에 포함됩니다. GitHub 저장소에 커밋하고 main에 푸시하면 실행되며, main 대상 PR에서도 실행됩니다. 별도의 secret 설정은 필요하지 않습니다.

JS·TS 린트, Kotlin·Gradle 린트, 공통 core JVM 테스트를 실행합니다. 서비스별 JS 테스트·Android·iOS 빌드·기기 실행 검사는 기본 CI에 포함하지 않으며 필요한 시점에 추가합니다.

## 플랫폼 확장

공통 화면과 API 계약은 \`shared-ui\`·\`shared-core\`에 유지하고 토스 전용 기능은 \`appsInTossApp\`에 둡니다. 나중에 플랫폼을 추가하려면 같은 CLI 버전의 템플릿에서 해당 호스트 디렉터리를 가져와 식별자를 바꾸고 Gradle 모듈을 등록합니다. 공통 모듈은 호스트 디렉터리에 따라 Android KMP 플러그인과 iOS 타깃을 활성화합니다.

CLI는 기존 프로젝트를 덮어쓰거나 호스트 추가를 자동으로 처리하지 않습니다. 호스트를 추가한 뒤 해당 플랫폼에서 빌드·실행을 확인해야 합니다.
`);
async function transform(directory) {
  for(const e of await readdir(directory,{withFileTypes:true})) {
    const p=join(directory,e.name);
    if(e.isDirectory()){await transform(p);continue;}
    if(!/\.(kt|kts|swift|pbxproj|xml|html|yml|md)$/.test(p))continue;
    let s=await readFile(p,'utf8');
    s=s.replaceAll(':sample:',':').replaceAll('io.github.l2hyunwoo.ait.sample','__PACKAGE_NAME__');
    s=s.replaceAll('implementation(project(":sdk"))',`implementation("io.github.l2hyunwoo:apps-in-toss-sdk:${sdkVersion}")`);
    s=s.replaceAll('project(mapOf("path" to ":sdk", "configuration" to "webBridgeResources"))', `"io.github.l2hyunwoo:apps-in-toss-sdk:${sdkVersion}:web-resources@jar"`);
    s=s.replaceAll('rootProject.file("sample/', 'rootProject.file("');
    if(p.endsWith('.pbxproj')) {
      s=s.replaceAll('PRODUCT_BUNDLE_IDENTIFIER = __PACKAGE_NAME__;','PRODUCT_BUNDLE_IDENTIFIER = "__PACKAGE_NAME__";');
      s=s.replaceAll('$SRCROOT/../..','$SRCROOT/..');
      s=s.replaceAll('INFOPLIST_KEY_CFBundleDisplayName = "Service sample";','INFOPLIST_KEY_CFBundleDisplayName = "__DISPLAY_NAME__";');
    }
    if(p.endsWith('project.yml')) { await rm(p);continue; } // Generated Xcode project is sufficient.
    if(p.endsWith('AndroidManifest.xml')) s=s.replace('android:label="Service sample"','android:label="__DISPLAY_NAME__"');
    if(p.endsWith('ServiceApp.kt')) s=s.replace('나의 작은 서비스','__DISPLAY_NAME__');
    if(p.endsWith('index.html'))s=s.replace('<title>CMP 서비스</title>','<title>__DISPLAY_NAME__</title>');
    await writeFile(p,s);
  }
}
await transform(dest);
await chmod(join(dest,'gradlew'),0o755);
// A nested template needs its own project boundary before Yarn resolves it.
await writeFile(join(dest,'yarn.lock'),'');
// Generate the lock without linking packages into the bundled template.
const locked=spawnSync(process.platform === 'win32' ? 'corepack.cmd' : 'corepack', ['yarn','install','--mode=update-lockfile'], {cwd:dest,stdio:'inherit',shell:process.platform==='win32'});
if(locked.error)throw locked.error;
if(locked.status!==0)process.exit(locked.status??1);
// Avoid making the bundled template a nested Yarn project during pack.
await rename(join(dest,'yarn.lock'),join(dest,'_yarn.lock'));
await rename(join(dest,'.yarnrc.yml'),join(dest,'_yarnrc.yml'));
await rm(join(dest,'.yarn'),{recursive:true,force:true});
console.log(`Prepared ${dest}`);
