import {chromium} from '@playwright/test';
import {spawn} from 'node:child_process';
import {setTimeout as delay} from 'node:timers/promises';
import assert from 'node:assert/strict';
const backend=spawn((process.env.JAVA_HOME?process.env.JAVA_HOME+'/bin/java':'java'),['-jar','../backend/target/gymlog-1.0.0.jar'],{cwd:process.cwd(),env:{...process.env,PORT:'18081',DB_URL:'jdbc:h2:mem:e2e;DB_CLOSE_DELAY=-1'},stdio:'ignore'});
const frontend=spawn('./node_modules/.bin/ng',['serve','--host','127.0.0.1','--proxy-config','proxy.e2e.json','--port','14200'],{stdio:'ignore',detached:true});
let browser;
try{
for(let i=0;i<60;i++){try{const r=await fetch('http://127.0.0.1:14200/api/state');if(r.ok)break;}catch{}await delay(500);}
browser=await chromium.launch({headless:true});const page=await browser.newPage({viewport:{width:390,height:844},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));
await page.goto('http://127.0.0.1:14200');await page.getByText('Tu primera rutina empieza aquí').waitFor();
await page.getByRole('button',{name:'Añadir ejemplo',exact:true}).click();await page.getByRole('button',{name:'Empezar sesión'}).waitFor();
await page.screenshot({path:'../docs/mobile-routines.png',fullPage:true});
await page.getByRole('button',{name:'Empezar sesión'}).click();
await page.getByLabel('Peso Press de banca serie 1',{exact:true}).fill('42.5');await page.getByRole('button',{name:'Completar Press de banca serie 1',exact:true}).click();
await page.reload();await page.getByRole('button',{name:'Terminar y guardar sesión'}).waitFor();assert.equal(await page.getByLabel('Peso Press de banca serie 1',{exact:true}).inputValue(),'42.5');
await page.screenshot({path:'../docs/mobile-session.png',fullPage:true});
page.once('dialog',d=>d.accept());await page.getByRole('button',{name:'Terminar y guardar sesión'}).click();await page.getByText('1 series · Ver entrenamiento').waitFor();
await page.getByRole('button',{name:'Progreso',exact:true}).click();await page.getByText('TU RÉCORD REGISTRADO').waitFor();
await page.screenshot({path:'../docs/mobile-progress.png',fullPage:true});
await page.setViewportSize({width:1440,height:1000});await page.screenshot({path:'../docs/desktop-progress.png',fullPage:true});
await page.getByRole('button',{name:'Rutinas',exact:true}).click();await page.getByRole('button',{name:'Editar',exact:true}).click();await page.getByLabel('Nombre de la rutina').fill('Torso editado');await page.getByRole('button',{name:'Guardar rutina',exact:true}).click();await page.getByRole('heading',{name:'Torso editado'}).waitFor();
const state=await(await fetch('http://127.0.0.1:18081/api/state')).json();assert.equal(state.workouts[0].sets[0].weight,42.5);assert.equal(state.workouts[0].sets.length,1);assert.equal(state.routines[0].name,'Torso editado');
for(const width of [320,390,768,1440]){await page.setViewportSize({width,height:900});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'no horizontal overflow at '+width);}
const bad=await fetch('http://127.0.0.1:18081/api/workouts',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({routine:'A',date:'2020-01-01',notes:'',sets:[{exercise:'B',weight:-1,reps:8}]})});assert.equal(bad.status,400);
assert.deepEqual(errors,[]);console.log('PASS: create, workout, draft recovery, save only completed sets, history, chart, edit, API validation, 4 responsive sizes, no console errors');
}finally{await browser?.close();backend.kill();try{process.kill(-frontend.pid,'SIGTERM')}catch{}}
