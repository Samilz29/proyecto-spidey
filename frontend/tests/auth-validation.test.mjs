import test from 'node:test';
import assert from 'node:assert/strict';
import {validateCredentials} from '../src/auth-validation.mjs';
test('email username returns a username error, not workout validation',()=>{const errors=validateCredentials({username:'sami2game@example.com',password:'123456789012345'});assert.match(errors.username,/no un email/);assert.equal(errors.password,undefined);});
test('accepts supported username and password',()=>assert.deepEqual(validateCredentials({username:'sami2game',password:'123456789012345'}),{}));
test('reports both missing fields',()=>{const errors=validateCredentials({username:'',password:''});assert.ok(errors.username);assert.ok(errors.password);});
test('rejects short passwords and UTF8 passwords over BCrypt byte limit',()=>{assert.match(validateCredentials({username:'sam',password:'short'}).password,/12/);assert.match(validateCredentials({username:'sam',password:'😀'.repeat(19)}).password,/bytes/);});
import {readFileSync} from 'node:fs';
const html=readFileSync(new URL('../src/app.html',import.meta.url),'utf8');
const patterns=[...html.matchAll(/\bpattern="([^"]*)"/g)].map(match=>match[1]);
test('all HTML patterns compile with the browser Unicode sets flag',()=>{
 assert.ok(patterns.length>0);
 for(const pattern of patterns) assert.doesNotThrow(()=>new RegExp(pattern,'v'));
});
test('username pattern and app validation agree on supported characters and limits',()=>{
 const pattern=new RegExp('^(?:'+patterns[0]+')$','v');
 for(const username of ['sam','sami2game','sam-lozano','sam.lozano','sam_lozano','a'.repeat(40)]){
  assert.ok(pattern.test(username),username);
  assert.equal(validateCredentials({username,password:'123456789012345'}).username,undefined);
 }
 for(const username of ['ab','a'.repeat(41),'sam@example.com','sam lozano','sam/lozano','sam|lozano','sam(lozano)','sám']){
  assert.equal(pattern.test(username),false,username);
  assert.ok(validateCredentials({username,password:'123456789012345'}).username);
 }
});
