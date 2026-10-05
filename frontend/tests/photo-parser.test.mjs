import {parseRoutine} from '../src/photo-parser.mjs';
import {test} from 'node:test';import assert from 'node:assert/strict';
test('reads range and RIR',()=>{const e=parseRoutine('Press banca 3 x 8-10 2').exercises[0];assert.equal(e.repsMax,10);assert.equal(e.rir,'2');});
test('reads meters per side without inventing repetitions',()=>{const e=parseRoutine('Paseo granjero 3 x 30m/lado 0').exercises[0];assert.equal(e.unit,'m');assert.equal(e.perSide,true);assert.equal(e.rir,'0');});
test('ignores headings and unknown content',()=>assert.equal(parseRoutine('DIA 1\nNo parece una serie').exercises.length,0));
test('rejects invalid bounds',()=>assert.equal(parseRoutine('Banca 99 x 8 2').exercises.length,0));
import {parseRoutines} from '../src/photo-parser.mjs';
test('separates photo days without merging repeated exercises',()=>assert.equal(parseRoutines('DÍA 1: TORSO\nPress banca 3 x 8-10 2\nDÍA 2: PIERNA\nPrensa 3 x 10-12 2').length,2));
test('normalizes OCR zeros',()=>assert.equal(parseRoutine('Triceps 3x12 [o]').exercises[0].rir,'0'));
