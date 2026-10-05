import test from 'node:test';
import assert from 'node:assert/strict';
import {volume,maximum,validSet} from '../src/metrics.mjs';
test('volume counts every set',()=>assert.equal(volume([{weight:42.5,reps:8},{weight:40,reps:10}]),740));
test('maximum handles empty and bodyweight sessions',()=>{assert.equal(maximum([]),0);assert.equal(maximum([{weight:0}]),0);});
test('maximum chooses the heaviest set',()=>assert.equal(maximum([{weight:20},{weight:42.5}]),42.5));
test('validSet accepts fractional plates and bodyweight',()=>{assert.ok(validSet({weight:42.25,reps:8}));assert.ok(validSet({weight:0,reps:8}));});
test('validSet rejects negative weight and fractional reps',()=>{assert.equal(validSet({weight:-1,reps:8}),false);assert.equal(validSet({weight:10,reps:1.5}),false);});
test('validSet rejects overprecision and nonfinite input',()=>{assert.equal(validSet({weight:42.251,reps:8}),false);assert.equal(validSet({weight:NaN,reps:8}),false);});
