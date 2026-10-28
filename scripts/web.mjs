import { build } from 'esbuild';
import { spawnSync } from 'node:child_process';
import { cp, mkdir, readFile, writeFile, stat } from 'node:fs/promises';
import { resolve, join, extname } from 'node:path';
import { createServer } from 'node:http';

const command = process.argv[2];
if (!['build','dev','dev:toss','build:web','dev:web'].includes(command)) throw Error('Use build, dev, dev:toss, build:web or dev:web');
const standalone = command.endsWith(':web');
const development = command.startsWith('dev');
const app = standalone ? 'webApp' : 'appsInTossApp';
const sample = await stat('sample').then(s => s.isDirectory()).catch(() => false);
const project = `${sample ? ':sample' : ''}:${app}`;
const task = `${project}:composeCompatibilityBrowserDistribution`;
const root = process.cwd();
function run(binary, args) {
  const result = spawnSync(binary, args, {stdio:'inherit', cwd:root, shell:process.platform === 'win32'});
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status ?? 1);
}
run(process.platform === 'win32' ? 'gradlew.bat' : './gradlew', [task, ...(standalone ? [] : [`${project}:prepareAppsInTossBridge`]), '--console=plain']);
const source = join(root, sample ? 'sample' : '', app, 'build/dist/composeWebCompatibility/productionExecutable');
const out = join(root, 'dist', (standalone ? 'web' : 'apps-in-toss') + (development ? '-dev' : ''));
const useMock = command === 'dev';
await mkdir(out, {recursive:true});
// 이 스크립트의 출력 트리만 지우고 프로젝트 디렉터리는 지우지 않는다.
const { rm } = await import('node:fs/promises');
await rm(out, {recursive:true, force:true});
await cp(source, out, {recursive:true});
if (!standalone) {
  const entry = join(root, sample ? 'sample' : '', app, 'build/ait-cmp-sdk/ait-cmp-bridge', `${useMock ? 'mock' : 'index'}.mjs`);
  await build({entryPoints:[entry], outfile:join(out,'ait-bridge.js'), bundle:true, format:'iife', platform:'browser', minify:!development, define:{'process.env.NODE_ENV':JSON.stringify(development ? 'development' : 'production')}});
  const index = join(out, 'index.html');
  const html = await readFile(index,'utf8');
  await writeFile(index, html.replace('<head>', '<head><script src="./ait-bridge.js"></script>'));
}
if (!development) {
  if (!standalone) run(process.platform === 'win32' ? 'node_modules/.bin/ait.cmd' : 'node_modules/.bin/ait', ['build']);
  console.log(`Built ${out}`);
} else {
  const mime = {'.html':'text/html; charset=utf-8','.js':'text/javascript','.mjs':'text/javascript','.wasm':'application/wasm','.json':'application/json','.ttf':'font/ttf','.woff2':'font/woff2','.css':'text/css'};
  const server = createServer(async (request, response) => {
    try {
      const path = decodeURIComponent(new URL(request.url,'http://localhost').pathname);
      const file = resolve(out, `.${path === '/' ? '/index.html' : path}`);
      if (!file.startsWith(`${out}/`)) { response.writeHead(403); response.end(); return; }
      const bytes = await readFile(file);
      response.writeHead(200, {'Content-Type':mime[extname(file)] ?? 'application/octet-stream', 'Cache-Control':'no-store'});response.end(bytes);
    } catch { response.writeHead(404);response.end('Not found'); }
  });
  server.listen(8080, command === 'dev:toss' ? '0.0.0.0' : '127.0.0.1',()=>console.log(`http://127.0.0.1:8080 (${standalone ? 'standalone web' : useMock ? 'explicit mock bridge' : 'real Toss bridge; connect from sandbox'})`));
}
