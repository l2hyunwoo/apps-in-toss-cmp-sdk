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
    for (const [namespace,method] of [['TossAuth','login'],['User','getAnonymousKey'],['Share','sendMessage'],['Screen','close'],['SafeArea','get'],['SafeArea','subscribe'],['graniteEvent','addEventListener'],['Storage','getItem'],['Storage','setItem'],['Storage','removeItem'],['Game','getUserProfile'],['Game','setLeaderboardScore'],['Game','openLeaderboard']]) {
      assert.equal(typeof sdk[namespace]?.[method],'function',`${namespace}.${method}`);
    }
    assert.equal(typeof sdk.generateHapticFeedback,'function');
    for (const method of ['getUserProfile','setLeaderboardScore','openLeaderboard']) assert.equal(typeof sdk.Game[method].isSupported,'function',method);
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

function gameFixture() {
  const {sdk}=fixture(); const store=new Map(); const haptics=[]; const scores=[];
  Object.assign(sdk,{
    generateHapticFeedback:async o=>{haptics.push(o.type)},
    Storage:{getItem:async k=>store.get(k)??null,setItem:async(k,v)=>{store.set(k,v)},removeItem:async k=>{store.delete(k)}},
    Game:{
      getUserProfile:Object.assign(async()=>({statusCode:'SUCCESS',nickname:'n',profileImageUri:''}),{isSupported:()=>true}),
      setLeaderboardScore:Object.assign(async({score})=>{scores.push(score);return {statusCode:'SUCCESS'}},{isSupported:()=>true}),
      openLeaderboard:Object.assign(async()=>{},{isSupported:()=>true})
    }
  });
  return {sdk,store,haptics,scores};
}
const callBridge=(b,method,payload)=>new Promise((resolve,reject)=>b.call(method,JSON.stringify(payload),s=>resolve(JSON.parse(s)),e=>reject(JSON.parse(e))));

test('game APIs dispatch to the official SDK',async()=>{
  const {sdk,store,haptics,scores}=gameFixture(); const b=createBridge(sdk);
  for (const cap of ['HAPTIC','STORAGE','GAME_PROFILE','LEADERBOARD']) assert.equal(b.supports(cap),true,cap);
  await callBridge(b,'haptic',{type:'confetti'}); assert.deepEqual(haptics,['confetti']);
  assert.equal(await callBridge(b,'storageGet',{key:'k'}),null);
  await callBridge(b,'storageSet',{key:'k',value:'v'}); assert.equal(store.get('k'),'v');
  assert.equal(await callBridge(b,'storageGet',{key:'k'}),'v');
  await callBridge(b,'storageRemove',{key:'k'}); assert.equal(store.has('k'),false);
  assert.equal((await callBridge(b,'gameProfile',{})).nickname,'n');
  assert.equal((await callBridge(b,'leaderboardSubmit',{score:'7'})).statusCode,'SUCCESS'); assert.deepEqual(scores,['7']);
  await callBridge(b,'leaderboardOpen',{});
});

test('game APIs validate input and report unsupported hosts',async()=>{
  const {sdk}=gameFixture(); const b=createBridge(sdk);
  assert.throws(()=>b.call('haptic',JSON.stringify({type:'boom'}),()=>{},()=>{}),TypeError);
  assert.throws(()=>b.call('storageSet',JSON.stringify({key:' ',value:'v'}),()=>{},()=>{}),TypeError);
  assert.throws(()=>b.call('leaderboardSubmit',JSON.stringify({score:'abc'}),()=>{},()=>{}),TypeError);
  assert.throws(()=>b.call('constructor','{}',()=>{},()=>{}),TypeError);
  sdk.Game.setLeaderboardScore.isSupported=()=>false;
  assert.equal(b.supports('LEADERBOARD'),false);
  await assert.rejects(callBridge(b,'leaderboardOpen',{}),{code:'UNSUPPORTED'});
  sdk.Game.getUserProfile=Object.assign(async()=>undefined,{isSupported:()=>true});
  await assert.rejects(callBridge(b,'gameProfile',{}),{code:'UNSUPPORTED'});
  delete sdk.Storage;
  assert.equal(b.supports('STORAGE'),false);
  await assert.rejects(callBridge(b,'storageGet',{key:'k'}),{code:'UNSUPPORTED'});
  const browser=createBridge({Environment:{environment:'web'}});
  assert.equal(browser.supports('HAPTIC'),false);
  await assert.rejects(callBridge(browser,'haptic',{type:'tap'}),{code:'HOST_UNAVAILABLE'});
});
