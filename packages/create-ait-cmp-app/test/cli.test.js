import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, writeFile, readFile, readdir, symlink, lstat, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { run, parseArgs, validate } from '../cli.js';

const args = ['my-app', '--yes', '--package', 'io.example.myapp', '--app-name', 'my-app', '--display-name', '내 앱', '--skip-install'];
async function fixture(t) {
  const temp = await mkdtemp(path.join(os.tmpdir(), 'ait-cli-'));
  // 명시적 symlink 보호를 테스트하기 전에 플랫폼 임시 디렉터리 경로를 해석한다.
  const { realpath } = await import('node:fs/promises');
  const cwd = await realpath(temp);
  t.after(() => rm(cwd, { recursive: true, force: true }));
  const template = path.join(cwd, 'template');
  await mkdir(template);
  await writeFile(path.join(template, 'package.json'), JSON.stringify({ name: '__PROJECT_NAME__', scripts: { dev: 'vite', 'dev:web': 'web', 'build:web': 'web' }, title: '__DISPLAY_NAME__' }));
  await writeFile(path.join(template, '_yarn.lock'), '"ait-cmp-template@workspace:.":\n  resolution: "ait-cmp-template@workspace:."\n  locator: "ait-cmp-template%40workspace%3A."\n\n"ajv@npm:6":\n  version: 6.0.0\n\n"zod@npm:4":\n  version: 4.0.0\n');
  await writeFile(path.join(template, 'App.kt'), 'package __PACKAGE_NAME__\n\nimport io.github.sdk.Client\nimport __PACKAGE_NAME__.Model\n\nval title = "__DISPLAY_NAME__"\n');
  await writeFile(path.join(template, 'Info.plist'), '<string>__DISPLAY_NAME__</string>');
  await writeFile(path.join(template, 'Title.swift'), 'let title = "__DISPLAY_NAME__"');
  await writeFile(path.join(template, 'project.pbxproj'), 'title = "__DISPLAY_NAME__";');
  await writeFile(path.join(template, 'title.properties'), 'title=__DISPLAY_NAME__');
  await writeFile(path.join(template, '_yarnrc.yml'), 'nodeLinker: node-modules\n');
  await mkdir(path.join(template, '_github/workflows'), { recursive: true });
  await writeFile(path.join(template, '_github/workflows/ci.yml'), 'name: CI\n');
  await writeFile(path.join(template, '_gitignore'), 'node_modules/\n');
  await writeFile(path.join(template, 'gradlew'), '#!/bin/sh\n', { mode: 0o755 });
  const binary = Buffer.from([0, 255, 32, 128]);
  await writeFile(path.join(template, 'wrapper.jar'), binary);
  for (const dir of ['androidApp', 'iosApp', 'webApp']) {
    await mkdir(path.join(template, dir));
    await writeFile(path.join(template, dir, 'README.md'), dir);
  }
  let output = '';
  const context = { cwd, template, input: { isTTY: false }, output: { write: text => { output += text; } } };
  return { cwd, template, context, output: () => output, binary };
}

test('기본 생성은 바이너리와 실행 권한을 보존하고 npm 이름을 갱신하며 선택 타깃을 제외한다', async t => {
  const f = await fixture(t);
  assert.equal(await run(args, f.context), 0);
  const root = path.join(f.cwd, 'my-app');
  const json = JSON.parse(await readFile(path.join(root, 'package.json')));
  assert.equal(json.name, 'my-app');
  assert.equal(await readFile(path.join(root, '.github/workflows/ci.yml'), 'utf8'), 'name: CI\n');
  assert.equal((await readdir(root)).includes('_github'), false);
  assert.match(await readFile(path.join(root, 'App.kt'), 'utf8'), /import io\.example\.myapp\.Model\nimport io\.github\.sdk\.Client/);
  assert.equal(await readFile(path.join(root, '.gitignore'), 'utf8'), 'node_modules/\n');
  assert.equal(json.scripts['dev:web'], undefined);
  const lock = await readFile(path.join(root, 'yarn.lock'), 'utf8');
  assert.equal(await readFile(path.join(root, '.yarnrc.yml'), 'utf8'), 'nodeLinker: node-modules\n');
  assert.match(lock, /my-app@workspace:\./);
  assert.doesNotMatch(lock, /ait-cmp-template/);
  assert.ok(lock.indexOf('"ajv@npm:6"') < lock.indexOf('"my-app@workspace:."'));
  assert.ok(lock.indexOf('"my-app@workspace:."') < lock.indexOf('"zod@npm:4"'));
  assert.match(lock, /my-app%40workspace%3A\./);
  assert.deepEqual(await readFile(path.join(root, 'wrapper.jar')), f.binary);
  assert.equal((await lstat(path.join(root, 'gradlew'))).mode & 0o111, 0o111);
  assert.equal((await readdir(root)).includes('androidApp'), false);
  assert.match(f.output(), /corepack yarn install --immutable/);
  assert.doesNotMatch(f.output(), /assembleDebug/);
});

test('모든 타깃과 공백이 든 상위 경로로 생성한다', async t => {
  const f = await fixture(t);
  await mkdir(path.join(f.cwd, 'space here'));
  assert.equal(await run(['space here/my-app', ...args.slice(1), '--targets=apps-in-toss,android,ios,web'], f.context), 0);
  const root = path.join(f.cwd, 'space here/my-app');
  for (const dir of ['androidApp', 'iosApp', 'webApp']) assert.ok((await lstat(path.join(root, dir))).isDirectory());
  assert.equal(JSON.parse(await readFile(path.join(root, 'package.json'))).scripts['dev:web'], 'web');
  assert.match(f.output(), /assembleDebug/);
  assert.match(f.output(), /Xcode/);
});

test('메타데이터에 JSON, Kotlin, XML 이스케이프를 적용한다', async t => {
  const f = await fixture(t);
  const title = '내 "앱" & <title> \\ $value';
  const argv = [...args]; argv[argv.indexOf('--display-name') + 1] = title;
  await run(argv, f.context);
  const root = path.join(f.cwd, 'my-app');
  assert.equal(JSON.parse(await readFile(path.join(root, 'package.json'))).title, title);
  assert.match(await readFile(path.join(root, 'App.kt'), 'utf8'), /\\\$value/);
  assert.match(await readFile(path.join(root, 'Info.plist'), 'utf8'), /&quot;앱&quot; &amp; &lt;title&gt;/);
  assert.equal(await readFile(path.join(root, 'project.pbxproj'), 'utf8'), `title = ${JSON.stringify(title)};`);
  assert.equal(await readFile(path.join(root, 'Title.swift'), 'utf8'), `let title = ${JSON.stringify(title)}`);
  assert.match(await readFile(path.join(root, 'title.properties'), 'utf8'), /\\ /);
});

test('비어 있지 않은 디렉터리와 파일을 보존한다', async t => {
  const f = await fixture(t);
  const destination = path.join(f.cwd, 'my-app');
  await mkdir(destination);
  await writeFile(path.join(destination, 'precious.txt'), 'keep');
  await assert.rejects(run(args, f.context), /not empty/);
  assert.equal(await readFile(path.join(destination, 'precious.txt'), 'utf8'), 'keep');
  assert.deepEqual(await readdir(destination), ['precious.txt']);
});

test('.git만 있는 디렉터리와 빈 디렉터리를 허용한다', async t => {
  for (const git of [false, true]) {
    const f = await fixture(t);
    await mkdir(path.join(f.cwd, 'my-app'));
    if (git) await mkdir(path.join(f.cwd, 'my-app/.git'));
    await run(args, f.context);
    assert.ok((await lstat(path.join(f.cwd, 'my-app/package.json'))).isFile());
  }
});

test('대상과 상위 경로의 symlink를 거부한다', async t => {
  const f = await fixture(t);
  await mkdir(path.join(f.cwd, 'actual'));
  await symlink(path.join(f.cwd, 'actual'), path.join(f.cwd, 'my-app'));
  await assert.rejects(run(args, f.context), /symlink/);
  await assert.rejects(run(['my-app/child-app', ...args.slice(1)], f.context), /symlink/);
  assert.deepEqual(await readdir(path.join(f.cwd, 'actual')), []);
});

test('잘못되거나 빠진 입력은 출력 파일 없이 실패한다', async t => {
  const f = await fixture(t);
  for (const argv of [[], ['my-app'], ['my-app', '--yes'], ['my-app', '--force'], [...args, '--targets', 'android'], [...args, '--targets', 'apps-in-toss,web,web'], [...args, '--package', 'bad']]) {
    await assert.rejects(run(argv, f.context));
  }
  assert.deepEqual(await readdir(f.cwd), ['template']);
  for (const value of ['io.bad_name.app', 'io.class.app', 'one', 'io.4bad', 'io.good;touch']) assert.throws(() => validate('package', value));
  assert.throws(() => validate('display-name', 'bad\nname'));
  assert.throws(() => validate('project', '../escape'));
  assert.throws(() => parseArgs(['--app-name']));
  assert.throws(() => parseArgs(['--yes=false']));
});

test('대화형 프롬프트는 주어진 값을 건너뛰고 검증을 재시도하고 확인을 받는다', async t => {
  const f = await fixture(t);
  const questions = [];
  const answers = ['bad_package', 'io.example.app', 'yes'];
  f.context.ask = async question => { questions.push(question); return answers.shift(); };
  await run(['my-app', '--targets', 'apps-in-toss', '--app-name', 'my-app', '--display-name', 'App', '--skip-install'], f.context);
  assert.equal(questions.length, 3);
  assert.match(questions[0], /Kotlin package/);
  assert.match(questions[2], /Create project/);
  assert.match(f.output(), /Package must/);
});

test('대화형 기본값과 취소는 대상 디렉터리를 남기지 않는다', async t => {
  const f = await fixture(t);
  let count = 0;
  f.context.ask = async () => { count++; return ''; };
  assert.equal(await run(['--skip-install'], f.context), 0);
  assert.equal(count, 6);
  assert.deepEqual(await readdir(f.cwd), ['template']);
  assert.match(f.output(), /Cancelled/);
});

test('생성 전에 Ctrl+C로 취소하면 대상 디렉터리를 남기지 않는다', async t => {
  const f = await fixture(t);
  f.context.ask = async () => { throw Object.assign(new Error('aborted'), { code: 'ABORT_ERR' }); };
  assert.equal(await run([], f.context), 130);
  assert.deepEqual(await readdir(f.cwd), ['template']);
});

test('설치가 실패하면 생성한 파일을 보존하고 실패를 반환한다', async t => {
  const f = await fixture(t);
  f.context.install = async directory => {
    assert.ok((await lstat(path.join(directory, 'package.json'))).isFile());
    throw new Error('offline');
  };
  assert.equal(await run(args.filter(arg => arg !== '--skip-install'), f.context), 1);
  assert.match(f.output(), /preserved/);
  assert.match(f.output(), /Retry corepack yarn install --immutable/);
});

test('help, version과 실제 비TTY 진입점을 확인한다', async t => {
  const f = await fixture(t);
  await run(['--help'], f.context);
  await run(['--version'], f.context);
  assert.match(f.output(), /Usage:/);
  assert.ok(f.output().includes(JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8')).version));
  const executable = fileURLToPath(new URL('../cli.js', import.meta.url));
  const child = spawnSync(process.execPath, [executable, 'my-app'], { cwd: f.cwd, encoding: 'utf8' });
  assert.equal(child.status, 1);
  assert.match(child.stderr, /No interactive terminal/);
});


test('npm 방식 symlink 실행 파일로 CLI를 실행한다', async t => {
  const f = await fixture(t);
  const executable = fileURLToPath(new URL('../cli.js', import.meta.url));
  const link = path.join(f.cwd, 'create-ait-cmp-app');
  await symlink(executable, link);
  const child = spawnSync(process.execPath, [link, '--version'], { encoding: 'utf8' });
  assert.equal(child.status, 0);
  assert.equal(child.stdout.trim(), JSON.parse(await readFile(new URL('../package.json', import.meta.url), 'utf8')).version);
});


test('Git worktree의 .git 파일을 허용하고 보존한다', async t => {
  const f = await fixture(t);
  const destination = path.join(f.cwd, 'my-app');
  await mkdir(destination);
  await writeFile(path.join(destination, '.git'), 'gitdir: ../existing-worktree\n');
  assert.equal(await run(args, f.context), 0);
  assert.equal(await readFile(path.join(destination, '.git'), 'utf8'), 'gitdir: ../existing-worktree\n');
});

test('디렉터리가 아닌 대상과 symlink .git을 거부한다', async t => {
  const f = await fixture(t);
  const destination = path.join(f.cwd, 'my-app');
  await writeFile(destination, 'precious');
  await assert.rejects(run(args, f.context), /not a directory/);
  assert.equal(await readFile(destination, 'utf8'), 'precious');
  await rm(destination);
  await mkdir(destination);
  await mkdir(path.join(f.cwd, 'git-data'));
  await symlink(path.join(f.cwd, 'git-data'), path.join(destination, '.git'));
  await assert.rejects(run(args, f.context), /symlink/);
  assert.deepEqual(await readdir(path.join(f.cwd, 'git-data')), []);
});

test('Unicode와 markdown 구두점은 표시 이름으로 허용하고 줄바꿈은 거부한다', async t => {
  const f = await fixture(t);
  const title = '앱 👋 "quoted" $value `code` [link](url) <>&';
  const argv = [...args]; argv[argv.indexOf('--display-name') + 1] = title;
  assert.equal(await run(argv, f.context), 0);
  assert.equal(JSON.parse(await readFile(path.join(f.cwd, 'my-app/package.json'))).title, title);
  for (const value of ['title\nnext', 'title\rnext', 'title\tbad', 'title\0bad']) {
    assert.throws(() => validate('display-name', value), /control characters/);
  }
});
