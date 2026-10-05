import test from 'node:test';
import assert from 'node:assert/strict';
import {validateCredentials} from '../src/auth-validation.mjs';
test('email username returns a username error, not workout validation',()=>{const errors=validateCredentials({username:'sami2game@example.com',password:'123456789012345'});assert.match(errors.username,/no un email/);assert.equal(errors.password,undefined);});
test('accepts supported username and password',()=>assert.deepEqual(validateCredentials({username:'sami2game',password:'123456789012345'}),{}));
test('reports both missing fields',()=>{const errors=validateCredentials({username:'',password:''});assert.ok(errors.username);assert.ok(errors.password);});
test('rejects short passwords and UTF8 passwords over BCrypt byte limit',()=>{assert.match(validateCredentials({username:'sam',password:'short'}).password,/12/);assert.match(validateCredentials({username:'sam',password:'😀'.repeat(19)}).password,/bytes/);});
