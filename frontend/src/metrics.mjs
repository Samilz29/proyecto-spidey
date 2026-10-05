export function volume(sets){return Math.round(sets.reduce((sum,set)=>sum+set.weight*set.reps,0));}
export function maximum(sets){return Math.max(0,...sets.map(set=>set.weight));}
export function validSet(set){return Number.isFinite(set.weight)&&set.weight>=0&&set.weight<=1000&&Math.abs(Math.round(set.weight*100)-set.weight*100)<0.00001&&Number.isInteger(set.reps)&&set.reps>=1&&set.reps<=100;}
