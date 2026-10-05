export function parseRoutine(text){
 const exercises=[];
 for(const raw of text.split(/\n/)){const line=raw.trim().replace(/[\[(L]?o[\])]?(?=\s*$)/i,'0').replace(/x\s*x/ig,'x');const match=line.match(/^(.+?)\s+(\d{1,2})\s*[x×]\s*(\d{1,3})(?:\s*[-–]\s*(\d{1,3}))?\s*(m|s)?(?:\s*\/\s*(lado))?(?:\s+(\d(?:-\d)?))?\s*$/i);if(!match)continue;
 const sets=Number(match[2]),reps=Number(match[3]),repsMax=Number(match[4]||match[3]);if(sets<1||sets>20||reps<1||repsMax<reps||repsMax>1000)continue;
 exercises.push({name:match[1].trim(),sets,reps,repsMax,unit:match[5]?.toLowerCase()||'reps',perSide:!!match[6],rir:match[7]||'2'});
 }
 return {name:'Rutina desde foto',exercises};
}
export function parseRoutines(text){const blocks=[];let name='Rutina desde foto',lines=[];for(const line of text.split('\n')){if(/^D[IÍ]A\s*\d+\s*:/i.test(line.trim())){if(lines.length){const r=parseRoutine(lines.join('\n'));if(r.exercises.length)blocks.push({...r,name});}name=line.trim();lines=[];}else lines.push(line);}const r=parseRoutine(lines.join('\n'));if(r.exercises.length)blocks.push({...r,name});return blocks;}
