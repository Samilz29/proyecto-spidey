export function parseRoutine(text:string):{name:string;exercises:{name:string;sets:number;reps:number;repsMax:number;unit:string;perSide:boolean;rir:string}[]};
export function parseRoutines(text:string):ReturnType<typeof parseRoutine>[];
