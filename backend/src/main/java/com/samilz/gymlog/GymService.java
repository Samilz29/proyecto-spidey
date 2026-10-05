package com.samilz.gymlog;
import static com.samilz.gymlog.Models.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class GymService {
 private final JdbcTemplate db;
 public GymService(JdbcTemplate db){this.db=db;}
 @Transactional(readOnly=true) public State state(String user){
 var routines=db.query("SELECT * FROM user_routine WHERE user_id=? ORDER BY name",(rs,n)->new Routine(rs.getString("id"),rs.getString("name"),db.query("SELECT * FROM user_exercise WHERE routine_id=? ORDER BY position",(e,k)->new Exercise(e.getString("id"),e.getString("name"),e.getInt("target_sets"),e.getInt("target_reps"),e.getInt("reps_max"),e.getString("unit"),e.getBoolean("per_side"),e.getString("rir")),rs.getString("id"))),user);
 var workouts=db.query("SELECT * FROM user_workout WHERE user_id=? ORDER BY trained_on DESC,created_at DESC,id",(rs,n)->new Workout(rs.getString("id"),rs.getString("routine_name"),rs.getDate("trained_on").toLocalDate(),rs.getString("notes"),db.query("SELECT * FROM user_logged_set WHERE workout_id=? ORDER BY position",(s,k)->new LoggedSet(s.getString("exercise_name"),s.getBigDecimal("weight"),s.getInt("reps"),s.getString("unit"),s.getBoolean("per_side"),s.getInt("rir")),rs.getString("id"))),user);
 var plans=db.query("SELECT * FROM schedule WHERE user_id=? ORDER BY planned_on",(rs,n)->new Plan(rs.getDate("planned_on").toLocalDate(),rs.getString("routine_id")),user);
 var weekly=db.query("SELECT * FROM weekly_plan WHERE user_id=? ORDER BY weekday",(rs,n)->new Weekly(rs.getInt("weekday"),rs.getString("routine_id")),user);return new State(routines,workouts,plans,weekly);
 }
 @Transactional public Routine save(String user,String id,Routine input){String key=id==null?UUID.randomUUID().toString():id;
 for(var e:input.exercises())if(e.repsMax()<e.reps()||(e.unit().equals("reps")&&e.repsMax()>100))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
 if(id!=null&&db.update("UPDATE user_routine SET name=? WHERE id=? AND user_id=?",input.name().trim(),key,user)==0)throw missing();
 if(id==null)db.update("INSERT INTO user_routine VALUES (?,?,?)",key,user,input.name().trim());
 db.update("DELETE FROM user_exercise WHERE routine_id=?",key);List<Exercise> exercises=new ArrayList<>();int pos=0;
 for(var e:input.exercises()){var ex=new Exercise(UUID.randomUUID().toString(),e.name().trim(),e.sets(),e.reps(),e.repsMax(),e.unit(),e.perSide(),e.rir());exercises.add(ex);db.update("INSERT INTO user_exercise VALUES (?,?,?,?,?,?,?,?,?,?)",ex.id(),key,ex.name(),ex.sets(),ex.reps(),ex.repsMax(),ex.unit(),ex.perSide(),ex.rir(),pos++);}return new Routine(key,input.name().trim(),exercises);
 }
 @Transactional public Workout log(String user,Workout input){for(var s:input.sets())if(s.unit().equals("reps")&&s.reps()>100)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);String key=UUID.randomUUID().toString();db.update("INSERT INTO user_workout (id,user_id,routine_name,trained_on,notes) VALUES (?,?,?,?,?)",key,user,input.routine().trim(),input.date(),input.notes().trim());int pos=0;for(var s:input.sets())db.update("INSERT INTO user_logged_set VALUES (?,?,?,?,?,?,?,?)",key,s.exercise().trim(),pos++,s.weight(),s.reps(),s.unit(),s.perSide(),s.rir());return new Workout(key,input.routine().trim(),input.date(),input.notes().trim(),input.sets());}
 @Transactional public void deleteRoutine(String user,String id){if(db.update("DELETE FROM user_routine WHERE id=? AND user_id=?",id,user)==0)throw missing();}
 @Transactional public void deleteWorkout(String user,String id){if(db.update("DELETE FROM user_workout WHERE id=? AND user_id=?",id,user)==0)throw missing();}
 @Transactional public void plan(String user,Plan plan){if(plan.routineId()!=null&&db.queryForObject("SELECT count(*) FROM user_routine WHERE id=? AND user_id=?",Integer.class,plan.routineId(),user)==0)throw missing();db.update("MERGE INTO schedule (user_id,planned_on,routine_id) KEY(user_id,planned_on) VALUES (?,?,?)",user,plan.date(),plan.routineId());}
 @Transactional public void unplan(String user,java.time.LocalDate date){db.update("DELETE FROM schedule WHERE user_id=? AND planned_on=?",user,date);}
 @Transactional public void weekly(String user,List<Weekly> entries){if(entries.size()!=7||entries.stream().map(Weekly::weekday).distinct().count()!=7)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);for(var e:entries){if(e.routineId()!=null&&db.queryForObject("SELECT count(*) FROM user_routine WHERE id=? AND user_id=?",Integer.class,e.routineId(),user)==0)throw missing();db.update("MERGE INTO weekly_plan(user_id,weekday,routine_id) KEY(user_id,weekday) VALUES (?,?,?)",user,e.weekday(),e.routineId());}}
 private ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
