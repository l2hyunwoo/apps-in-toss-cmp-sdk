#!/usr/bin/env node
import { lstat, readdir, readFile, mkdir, writeFile, chmod, realpath } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createInterface } from 'node:readline/promises';
import { spawn } from 'node:child_process';

const {version: VERSION} = JSON.parse(await readFile(new URL('./package.json', import.meta.url), 'utf8'));
const TARGETS = ['apps-in-toss', 'android', 'ios', 'web'];
const TEMPLATE = fileURLToPath(new URL('./templates/app/', import.meta.url));
const HELP = `Usage: create-ait-cmp-app [directory] [options]

Create a Compose Multiplatform app. Interactive prompts are the default.
  --package <id>         Kotlin package, Android application ID and iOS bundle ID
  --app-name <name>      Apps in Toss console appName (lowercase letters, digits, hyphens)
  --display-name <name>  App title
  --targets <list>       apps-in-toss,android,ios,web (default: apps-in-toss)
  --yes                 Skip prompts; directory and all three metadata options required
  --skip-install        Generate files without running corepack yarn install --immutable
  --help                Show help
  --version             Show version

Node.js 24 or newer is required. Existing files and symlink paths are protected.
`;
const KEYWORDS = new Set('as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while'.split(' '));

export function validate(field, value) {
  if (typeof value !== 'string' || !value.trim()) throw new Error(`${field} is required.`);
  if (field === 'project' && !/^[a-z][a-z0-9-]{0,79}$/.test(value)) {
    throw new Error('Project name must start with a lowercase letter and contain only lowercase letters, digits and hyphens (max 80 characters).');
  }
  if (field === 'package' && (!/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/.test(value)
      || value.split('.').some(part => KEYWORDS.has(part) || part.includes('_')))) {
    throw new Error('Package must have at least two dot-separated segments starting with letters, using letters and digits only, without Kotlin keywords.');
  }
  if (field === 'app-name' && !/^[a-z][a-z0-9-]{0,79}$/.test(value)) {
    throw new Error('appName must start with a lowercase letter and contain lowercase letters, digits and hyphens (max 80 characters).');
  }
  // 파일을 쓰기 전에 표시 이름의 제어 문자를 거부한다.
  // eslint-disable-next-line no-control-regex
  if (field === 'display-name' && (/[\u0000-\u001f\u007f]/.test(value) || value.length > 100)) {
    throw new Error('Display name must contain 1–100 characters without control characters.');
  }
  if (field === 'targets') {
    const selected = value.split(',');
    if (!selected.includes('apps-in-toss') || selected.some(t => !TARGETS.includes(t)) || new Set(selected).size !== selected.length) {
      throw new Error(`Targets must include apps-in-toss and use each of ${TARGETS.join(',')} at most once.`);
    }
  }
  return value;
}

export function parseArgs(argv) {
  const options = {};
  const flags = new Set(['yes', 'skip-install', 'help', 'version']);
  const values = new Set(['package', 'app-name', 'display-name', 'targets']);
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg.startsWith('--')) {
      const [name, ...rest] = arg.slice(2).split('=');
      if (Object.hasOwn(options, name)) throw new Error(`Duplicate option --${name}.`);
      if (flags.has(name) && !rest.length) options[name] = true;
      else if (values.has(name)) {
        const value = rest.length ? rest.join('=') : argv[++i];
        if (value === undefined || value.startsWith('--')) throw new Error(`Missing value for --${name}.`);
        options[name] = validate(name, value);
      } else throw new Error(`Unknown option ${arg}. Use --help.`);
    } else {
      if (arg.startsWith('-')) throw new Error(`Unknown option ${arg}. Use --help.`);
      if (options.directory !== undefined) throw new Error('Provide only one target directory.');
      options.directory = arg;
    }
  }
  return options;
}

export async function checkDirectory(directory) {
  // 안전해 보이는 대상 경로를 symlink가 다른 곳으로 돌리지 못하도록 모든 상위 경로를 검사한다.
  let current = path.parse(directory).root;
  for (const part of directory.slice(current.length).split(path.sep).filter(Boolean)) {
    current = path.join(current, part);
    try {
      const stat = await lstat(current);
      if (stat.isSymbolicLink()) throw new Error(`Refusing symlink path: ${current}`);
      if (!stat.isDirectory()) throw new Error(`Path is not a directory: ${current}`);
    } catch (error) {
      if (error.code === 'ENOENT') break;
      throw error;
    }
  }
  try {
    const entries = await readdir(directory);
    if (entries.some(entry => entry !== '.git')) throw new Error(`Directory is not empty: ${directory}`);
    if (entries.includes('.git') && (await lstat(path.join(directory, '.git'))).isSymbolicLink()) {
      throw new Error('Refusing symlink .git entry.');
    }
  } catch (error) {
    if (error.code !== 'ENOENT') throw error;
  }
}

function escapeValue(value, filename) {
  const ext = path.extname(filename);
  if (['.xml', '.plist', '.html'].includes(ext)) {
    return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&apos;');
  }
  if (['.json', '.js', '.mjs', '.cjs', '.ts', '.tsx', '.kt', '.kts', '.swift', '.pbxproj'].includes(ext)) {
    const escaped = JSON.stringify(value).slice(1, -1).replaceAll('\u2028', '\\u2028').replaceAll('\u2029', '\\u2029');
    if (ext === '.swift') return escaped.replaceAll('\\u2028', '\\u{2028}').replaceAll('\\u2029', '\\u{2029}');
    return ['.kt', '.kts'].includes(ext) ? escaped.replaceAll('$', '\\$') : escaped;
  }
  if (ext === '.properties') return value.replace(/[\\:=#! ]/g, char => `\\${char}`);
  if (['.md', '.txt'].includes(ext)) return value;
  throw new Error(`Template metadata has no escaping contract for ${filename}.`);
}

async function templateFiles(root, options, prefix = '') {
  const files = [];
  const omitted = new Set([!options.targets.includes('android') && 'androidApp', !options.targets.includes('ios') && 'iosApp', !options.targets.includes('web') && 'webApp']);
  for (const entry of await readdir(path.join(root, prefix), { withFileTypes: true })) {
    if (!prefix && omitted.has(entry.name)) continue;
    const relative = path.join(prefix, entry.name);
    if (entry.isSymbolicLink()) throw new Error(`Template symlink is not supported: ${relative}`);
    if (entry.isDirectory()) files.push(...await templateFiles(root, options, relative));
    else if (entry.isFile()) {
      const source = await readFile(path.join(root, relative));
      let content = source;
      const text = source.toString('utf8');
      if (/__(PROJECT_NAME|PACKAGE_NAME|APP_NAME|DISPLAY_NAME)__/.test(text)) {
        const metadata = { PROJECT_NAME: options.project, PACKAGE_NAME: options.package, APP_NAME: options['app-name'], DISPLAY_NAME: options['display-name'] };
        content = Buffer.from(text.replace(/__(PROJECT_NAME|PACKAGE_NAME|APP_NAME|DISPLAY_NAME)__/g,
          (_, key) => escapeValue(metadata[key], relative)));
      }
      // 패키지 치환으로 .editorconfig가 요구하는 import 순서가 바뀔 수 있다.
      if (relative.endsWith('.kt')) {
        content = Buffer.from(content.toString('utf8').replace(/^(?:import [^\n]+\n)+/gm,
          block => `${block.trimEnd().split('\n').sort().join('\n')}\n`));
      }
      if (relative === 'package.json') {
        const json = JSON.parse(content.toString('utf8'));
        json.name = options.project;
        if (relative === 'package.json' && !options.targets.includes('web')) {
          delete json.scripts?.['dev:web'];
          delete json.scripts?.['build:web'];
        }
        content = Buffer.from(`${JSON.stringify(json, null, 2)}\n`);
      }
      if (relative === '_yarn.lock') {
        // workspace 이름만 바뀌어도 Yarn은 정규 항목 순서를 요구한다.
        const blocks = content.toString('utf8').replaceAll('ait-cmp-template', options.project).trimEnd().split('\n\n');
        const firstEntry = blocks.findIndex(block => block.startsWith('"'));
        if (firstEntry < 0) throw new Error('Bundled Yarn lockfile has no entries.');
        content = Buffer.from([...blocks.slice(0, firstEntry), ...blocks.slice(firstEntry).sort()].join('\n\n') + '\n');
      }
      const destination = relative.startsWith(`_github${path.sep}`)
        ? `.github${relative.slice('_github'.length)}`
        : ({'_gitignore':'.gitignore','_yarn.lock':'yarn.lock','_yarnrc.yml':'.yarnrc.yml'})[relative] ?? relative;
      files.push({ relative: destination, content, mode: (await lstat(path.join(root, relative))).mode & 0o777 });
    }
  }
  return files;
}

function install(directory) {
  return new Promise((resolve, reject) => {
    const command = process.platform === 'win32' ? 'corepack.cmd' : 'corepack';
    const child = spawn(command, ['yarn', 'install', '--immutable'], {
      cwd: directory, stdio: 'inherit', shell: process.platform === 'win32'
    });
    child.once('error', reject);
    child.once('exit', code => code === 0 ? resolve() : reject(new Error(`corepack yarn install --immutable exited with ${code ?? 'a signal'}.`)));
  });
}

export async function run(argv, context = {}) {
  const output = context.output ?? process.stdout;
  const input = context.input ?? process.stdin;
  const log = message => output.write(`${message}\n`);
  const options = parseArgs(argv);
  if (options.help) { output.write(HELP); return 0; }
  if (options.version) { log(VERSION); return 0; }
  if (Number(process.versions.node.split('.')[0]) < 24) throw new Error('Node.js 24 or newer is required.');
  if (!options.yes && !input.isTTY && !context.ask) {
    throw new Error('No interactive terminal. Use <directory> --yes --package <id> --app-name <name> --display-name <name>.');
  }
  if (options.yes) {
    const missing = ['directory', 'package', 'app-name', 'display-name'].filter(key => options[key] === undefined);
    if (missing.length) throw new Error(`--yes requires ${missing.map(key => key === 'directory' ? '<directory>' : `--${key}`).join(', ')}.`);
  }
  let rl;
  let cancelled = false;
  let abortQuestion;
  const ask = context.ask ?? (question => {
    if (!rl) {
      rl = createInterface({ input, output });
      rl.on('SIGINT', () => { cancelled = true; abortQuestion?.abort(); rl.close(); });
    }
    abortQuestion = new AbortController();
    return rl.question(question, { signal: abortQuestion.signal });
  });
  async function prompt(field, label, fallback) {
    if (options[field] !== undefined) return;
    for (;;) {
      const answer = (await ask(`${label} [${fallback}]: `)).trim() || fallback;
      try {
        if (field === 'directory') validate('project', path.basename(path.resolve(context.cwd ?? process.cwd(), answer)));
        options[field] = field === 'directory' ? answer : validate(field, answer);
        return;
      }
      catch (error) { log(error.message); }
    }
  }
  try {
    if (!options.yes) await prompt('directory', 'Project directory', 'my-app');
    validate('directory', options.directory);
    const directory = path.resolve(context.cwd ?? process.cwd(), options.directory);
    options.project = validate('project', path.basename(directory));
    await checkDirectory(directory);
    if (!options.yes) {
      await prompt('targets', 'Targets (comma-separated)', 'apps-in-toss');
      await prompt('package', 'Kotlin package / application ID', `io.example.${options.project.replaceAll('-', '')}`);
      await prompt('app-name', 'Apps in Toss appName (must match your console registration)', options.project);
      await prompt('display-name', 'Display name', options.project);
    }
    options.targets = (options.targets ?? 'apps-in-toss').split(',');
    log(`\nDirectory: ${directory}\nProject: ${options.project}\nTargets: ${options.targets.join(', ')}\nPackage: ${options.package}\nappName: ${options['app-name']}\nDisplay name: ${options['display-name']}\nInstall dependencies: ${!options['skip-install']}`);
    if (!options.yes && !/^(y|yes)$/i.test((await ask('Create project? [y/N]: ')).trim())) {
      log('Cancelled.'); return 0;
    }
    rl?.close();
    // 디렉터리를 만들기 전에 모두 렌더링한다. 그래서 잘못된 템플릿은 출력을 남기지 않는다.
    const files = await templateFiles(context.template ?? TEMPLATE, options);
    if (!files.length) throw new Error('Bundled template is empty.');
    await checkDirectory(directory);
    await mkdir(directory, { recursive: true });
    for (const file of files) {
      const destination = path.join(directory, file.relative);
      await mkdir(path.dirname(destination), { recursive: true });
      await writeFile(destination, file.content, { flag: 'wx', mode: file.mode });
      await chmod(destination, file.mode);
    }
    log(`\nCreated ${options.project}.`);
    if (!options['skip-install']) {
      try { await (context.install ?? install)(directory); }
      catch (error) {
        log(`Dependency installation failed: ${error.message}\nGenerated files are preserved. Retry corepack yarn install --immutable in ${directory}.`);
        return 1;
      }
    }
    log(`\nNext steps in ${directory}:`);
    if (options['skip-install']) log('  corepack yarn install --immutable');
    log('  yarn lint\n  yarn dev\n  yarn build');
    if (options.targets.includes('android')) log('  ./gradlew :androidApp:assembleDebug (requires JDK 17 and Android SDK)');
    if (options.targets.includes('ios')) log('  Open iosApp in Xcode on macOS and configure your signing team.');
    if (options.targets.includes('web')) log('  yarn dev:web\n  yarn build:web');
    log('Configure the Apps in Toss console values and logo before a production build.');
    return 0;
  } catch (error) {
    if (cancelled || error.code === 'ABORT_ERR' || error.code === 'ERR_USE_AFTER_CLOSE') { log('Cancelled.'); return 130; }
    throw error;
  } finally { rl?.close(); }
}

if (process.argv[1] && await realpath(process.argv[1]).catch(() => '') === fileURLToPath(import.meta.url)) {
  run(process.argv.slice(2)).then(code => { process.exitCode = code; }).catch(error => {
    process.stderr.write(`Error: ${error.message}\n`); process.exitCode = 1;
  });
}
