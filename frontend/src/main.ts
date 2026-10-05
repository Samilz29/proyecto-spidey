import {volume,maximum,validSet} from './metrics.mjs';
import 'zone.js';
import {bootstrapApplication} from '@angular/platform-browser';
import {Component,OnInit,provideZoneChangeDetection,ChangeDetectorRef,inject,ChangeDetectionStrategy} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {CommonModule} from '@angular/common';
interface Exercise {id?:string;name:string;sets:number;reps:number}
interface Routine {id?:string;name:string;exercises:Exercise[]}
interface SetLog {exercise:string;weight:number;reps:number;done?:boolean}
interface Workout {id?:string;routine:string;date:string;notes:string;sets:SetLog[]}
interface State {routines:Routine[];workouts:Workout[]}
const today=()=>{const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
@Component({changeDetection:ChangeDetectionStrategy.Default,selector:'gym-app',standalone:true,imports:[FormsModule,CommonModule],templateUrl:'./app.html'})
class App implements OnInit {
 cdr=inject(ChangeDetectorRef);
 state:State={routines:[],workouts:[]};tab='train';loading=true;busy=false;message='';error='';editor:Routine|null=null;active:Workout|null=null;selected='';demo=new URLSearchParams(location.search).has('demo');
 async ngOnInit(){await this.refresh();this.restoreDraft();}
 async request(path:string,method='GET',body?:unknown){const r=await fetch('/api'+path,{method,headers:{'Content-Type':'application/json'},body:body?JSON.stringify(body):undefined});if(!r.ok)throw Error(r.status===400?'Revisa los campos: nombres, series, pesos y repeticiones.':'No se ha guardado. Revisa la conexión y vuelve a intentarlo.');return r.status===204?null:r.json();}
 async refresh(){try{this.state=this.demo?JSON.parse(localStorage.getItem('gymlog-demo')||'{"routines":[],"workouts":[]}'):await this.request('/state');}catch{this.error='No puedo conectar con Spring Boot. Inicia el servidor. Tus cambios no se enviarán hasta que vuelva la conexión.';}finally{this.loading=false;this.selected ||=this.state.routines[0]?.id||'';this.cdr.detectChanges();}}
 persistDemo(){localStorage.setItem('gymlog-demo',JSON.stringify(this.state));}
 uid(){return crypto.randomUUID();}
 draft(){try{if(this.active)localStorage.setItem(this.demo?'gymlog-demo-draft':'gymlog-draft',JSON.stringify(this.active));else localStorage.removeItem(this.demo?'gymlog-demo-draft':'gymlog-draft');}catch{this.error='No se pudo guardar el borrador en este dispositivo.';}this.cdr.detectChanges();}
 restoreDraft(){try{const value=localStorage.getItem(this.demo?'gymlog-demo-draft':'gymlog-draft');if(value)this.active=JSON.parse(value);}catch{this.error='El borrador no se puede leer.';}}
 openEditor(r?:Routine){this.editor=r?structuredClone(r):{name:'',exercises:[{name:'',sets:3,reps:10}]};this.error='';this.cdr.detectChanges();}
 async saveRoutine(){if(!this.editor||this.busy)return;const r=this.editor;if(!r.name.trim()||r.exercises.length===0||r.exercises.some(e=>!e.name.trim()||!Number.isInteger(e.sets)||e.sets<1||e.sets>20||!Number.isInteger(e.reps)||e.reps<1||e.reps>100)){this.error='Añade nombre y ejercicios. Series: 1–20; repeticiones: 1–100.';return;}this.busy=true;try{if(this.demo){r.id ||=this.uid();this.state.routines=this.state.routines.filter(x=>x.id!==r.id).concat(structuredClone(r));this.persistDemo();}else await this.request('/routines'+(r.id?'/'+r.id:''),r.id?'PUT':'POST',r);this.editor=null;await this.refresh();this.message='Rutina guardada.';this.error='';}catch(e){this.error=(e as Error).message;}finally{this.busy=false;this.cdr.detectChanges();}}
 async deleteRoutine(r:Routine){if(!confirm('¿Borrar '+r.name+'? Tu historial se conserva.'))return;try{if(this.demo){this.state.routines=this.state.routines.filter(x=>x.id!==r.id);this.persistDemo();}else await this.request('/routines/'+r.id,'DELETE');await this.refresh();}catch(e){this.error=(e as Error).message;}}
 start(r:Routine){if(this.active&&!confirm('Ya tienes una sesión abierta. ¿Descartar el borrador?'))return;this.active={routine:r.name,date:today(),notes:'',sets:r.exercises.flatMap(e=>Array.from({length:e.sets},()=>({exercise:e.name,weight:this.lastWeight(e.name),reps:e.reps,done:false})))};this.tab='train';this.draft();this.error='';this.cdr.detectChanges();}
 lastWeight(name:string){return this.state.workouts.flatMap(w=>w.sets).find(s=>s.exercise===name)?.weight||0;}
 groups(){return [...new Set(this.active?.sets.map(s=>s.exercise)||[])];}
 setsFor(name:string){return this.active?.sets.filter(s=>s.exercise===name)||[];}
 addSet(name:string){const last=this.setsFor(name).at(-1);this.active?.sets.push({exercise:name,weight:last?.weight||0,reps:last?.reps||10,done:false});this.draft();}
 removeSet(s:SetLog){if(this.active){this.active.sets=this.active.sets.filter(x=>x!==s);this.draft();}}
 completed(){return this.active?.sets.filter(s=>s.done).length||0;}
 async finish(){if(!this.active||this.busy)return;const sets=this.active.sets.filter(s=>s.done);if(!sets.length){this.error='Marca al menos una serie como completada.';return;}if(this.active.date>today()||!this.active.date||sets.some(s=>!validSet(s))){this.error='Revisa fecha, pesos (0–1000 kg, hasta 2 decimales) y repeticiones (1–100).';return;}if(sets.length<this.active.sets.length&&!confirm('Quedan series sin marcar. ¿Guardar solo las '+sets.length+' completadas?'))return;this.busy=true;try{const w={...this.active,sets:sets.map(({done,...s})=>s)};if(this.demo){w.id=this.uid();this.state.workouts.unshift(w);this.persistDemo();}else await this.request('/workouts','POST',w);this.active=null;this.draft();await this.refresh();this.message='Entrenamiento guardado. Un paso más.';this.error='';this.tab='history';}catch(e){this.error=(e as Error).message;}finally{this.busy=false;this.cdr.detectChanges();}}
 cancel(){if(confirm('¿Descartar este entrenamiento sin guardarlo?')){this.active=null;this.draft();}}
 volume(w:Workout){return volume(w.sets);}
 totalVolume(){return this.state.workouts.reduce((n,w)=>n+this.volume(w),0);}
 names(){return [...new Set(this.state.workouts.flatMap(w=>w.sets.map(s=>s.exercise)))];}
 points(){return this.state.workouts.filter(w=>w.sets.some(s=>s.exercise===this.selected)).slice().reverse().map(w=>({date:w.date,weight:Math.max(...w.sets.filter(s=>s.exercise===this.selected).map(s=>s.weight))}));}
 max(){return maximum(this.points());}
 async deleteWorkout(w:Workout){if(!confirm('¿Borrar este entrenamiento? No se puede deshacer.'))return;try{if(this.demo){this.state.workouts=this.state.workouts.filter(x=>x.id!==w.id);this.persistDemo();}else await this.request('/workouts/'+w.id,'DELETE');await this.refresh();}catch(e){this.error=(e as Error).message;}}
 export(){const url=URL.createObjectURL(new Blob([JSON.stringify(this.state,null,2)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download='gymlog-'+today()+'.json';a.click();URL.revokeObjectURL(url);}
 seed(){if(this.state.routines.length&&!confirm('¿Añadir otra rutina de ejemplo?'))return;this.editor={name:'Torso · Fuerza',exercises:[{name:'Press de banca',sets:3,reps:8},{name:'Remo con barra',sets:3,reps:10},{name:'Press militar',sets:3,reps:10}]};this.saveRoutine();}
}
bootstrapApplication(App,{providers:[provideZoneChangeDetection() ]}).catch(console.error);
