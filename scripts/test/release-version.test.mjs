import {test} from 'node:test';
import assert from 'node:assert/strict';
import { getNpmTag } from '../release-version.mjs';

test('정식 릴리스는 latest를 쓰고 사전 릴리스는 자기 npm 채널에 남는다',()=>{
  for (const [version,channel] of [['1.0.0','latest'],['1.0.0-alpha01','alpha'],['1.0.0-beta02','beta'],['1.0.0-rc01','rc']]) assert.equal(getNpmTag(version),channel);
});
test('잘못된 버전은 릴리스 경로나 셸 명령에 도달하지 못한다',()=>{
  for (const version of [undefined,'','v1.0.0','01.0.0','1.0.0-SNAPSHOT','1.0.0-alpha','1.0.0-alpha01/other','1.0.0;echo invalid','1.0.0\n']) assert.throws(()=>getNpmTag(version));
});
