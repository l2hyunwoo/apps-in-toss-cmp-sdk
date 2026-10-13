import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createBridge} from '../src/webMain/resources/ait-cmp-bridge/core.mjs';
function fixture() {
  const removed=[]; const listeners={};
  const sdk={Environment:{environment:'sandbox'}, TossAuth:{login:async()=>({authorizationCode:'secret',referrer:'SANDBOX'})}, Share:{sendMessage:async()=>{}}, Screen:{close:async()=>{}}, SafeArea:{get:()=>({top:10,left:0,right:0,bottom:20}),subscribe:({onEvent})=>{listeners.safe=onEvent;return()=>removed.push('safe');}},graniteEvent:{addEventListener:(name,{onEvent})=>{listeners[name]=onEvent;return()=>removed.push(name);}}};
  return {sdk,removed,listeners};
}
test('login uses official namespace and callbacks; cancellation ignores late result',async()=>{
  const {sdk}=fixture(); const b=createBridge(sdk); let result;
  b.login(s=>{result=JSON.parse(s)},()=>assert.fail()); await new Promise(setImmediate);
  assert.equal(result.referrer,'SANDBOX');
  const cancel=b.login(()=>assert.fail(),()=>assert.fail());cancel();await new Promise(setImmediate);
});
test('plain browser is unavailable, never silently mocked',()=>{
  const b=createBridge({Environment:{get environment(){throw Error('no host')}}});
  assert.equal(b.host(),'browser'); b.login(()=>assert.fail(),e=>assert.equal(JSON.parse(e).code,'HOST_UNAVAILABLE'));
});
test('events have initial safe area and unsubscribe exactly once',()=>{
  const {sdk,removed,listeners}=fixture();const events=[];const stop=createBridge(sdk).subscribe(e=>events.push(JSON.parse(e)),()=>assert.fail());
  assert.equal(events[0].top,10);listeners.backEvent();assert.equal(events[1].type,'back');stop();stop();listeners.backEvent();
  assert.equal(events.length,2);assert.deepEqual(removed,['backEvent','homeEvent','safe']);
});
test('partial subscription failure cleans previous listeners',()=>{
  const {sdk,removed}=fixture();sdk.SafeArea.subscribe=()=>{throw Error('private')};
  createBridge(sdk).subscribe(()=>assert.fail(),e=>assert.deepEqual(JSON.parse(e),{code:'SDK_FAILURE'}));assert.equal(removed.length,2);
});
test('invalid sharing is rejected before host call',()=>assert.throws(()=>createBridge(fixture().sdk).share(' ',()=>{},()=>{}),TypeError));

test('anonymous user key checks the official version capability', async()=>{
  const {sdk}=fixture(); sdk.User={getAnonymousKey:Object.assign(async()=>({type:'HASH',hash:'id'}),{isSupported:()=>false})};
  let error;createBridge(sdk).anonymousKey(()=>assert.fail(),e=>{error=JSON.parse(e)});
  assert.equal(error.code,'UNSUPPORTED');
  sdk.User.getAnonymousKey.isSupported=()=>true;
  let result;createBridge(sdk).anonymousKey(s=>{result=JSON.parse(s)},()=>assert.fail());await new Promise(setImmediate);
  assert.equal(result.hash,'id');
});

test('injected adapter reports missing and explicitly unsupported APIs',()=>{
  const {sdk}=fixture();delete sdk.TossAuth;
  const b=createBridge(sdk);
  assert.equal(b.supports('TOSS_LOGIN'),false);
  b.login(()=>assert.fail(),e=>assert.equal(JSON.parse(e).code,'UNSUPPORTED'));
  sdk.Share.sendMessage.isSupported=()=>false;
  assert.equal(b.supports('SHARE'),false);
  b.share('hello',()=>assert.fail(),e=>assert.equal(JSON.parse(e).code,'UNSUPPORTED'));
  delete sdk.SafeArea;
  b.subscribe(()=>assert.fail(),e=>assert.equal(JSON.parse(e).code,'UNSUPPORTED'));
});

test('SDK resource accepts an injected SDK without installing a global bridge',async()=>{
  const {createBridge: factory}=await import('../src/webMain/resources/ait-cmp-bridge/core.mjs');
  assert.equal(factory(fixture().sdk).host(),'sandbox');
});

for (const name of ['toss-sdk-min','@apps-in-toss/web-framework']) {
  test(`official SDK public API shape: ${name}`,async()=>{
    const sdk=await import(name);
    for (const [namespace,method] of [['TossAuth','login'],['User','getAnonymousKey'],['Share','sendMessage'],['Screen','close'],['SafeArea','get'],['SafeArea','subscribe'],['graniteEvent','addEventListener']]) {
      assert.equal(typeof sdk[namespace]?.[method],'function',`${namespace}.${method}`);
    }
    assert.equal(typeof sdk.User.getAnonymousKey.isSupported,'function');
  });
}

test('bundled SDK resources initialize their host and keep mock out of production',async()=>{
  const {build}=await import('esbuild');
  const {runInNewContext}=await import('node:vm');
  for (const entry of ['mock','index']) {
    const bundled=await build({entryPoints:[`sdk/src/webMain/resources/ait-cmp-bridge/${entry}.mjs`],bundle:true,write:false,format:'iife',platform:'browser',metafile:true,define:{'process.env.NODE_ENV':'"production"'}});
    const context={console:{info(){}},setTimeout,clearTimeout,URL,TextEncoder,TextDecoder};
    runInNewContext(bundled.outputFiles[0].text,context);
    assert.equal(context.aitCmpBridge.host(),entry==='mock'?'mock':'browser');
    if (entry==='index') assert.ok(Object.keys(bundled.metafile.inputs).every(p=>!p.endsWith('/mock.mjs')));
    else {
      let result;
      context.aitCmpBridge.login(s=>{result=JSON.parse(s)},()=>assert.fail());
      await new Promise(setImmediate);
      assert.equal(result.authorizationCode,'mock-not-for-server');
    }
  }
});
