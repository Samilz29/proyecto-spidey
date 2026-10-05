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
 public GymService(JdbcTemplate db) { this.db=db; }
 @Transactional(readOnly=true)
 public State state() {
  var routines=db.query("SELECT * FROM routine ORDER BY name",(rs,n)->new Routine(rs.getString("id"),rs.getString("name"),db.query("SELECT * FROM exercise WHERE routine_id=? ORDER BY position",(e,k)->new Exercise(e.getString("id"),e.getString("name"),e.getInt("target_sets"),e.getInt("target_reps")),rs.getString("id"))));
  var workouts=db.query("SELECT * FROM workout ORDER BY trained_on DESC,created_at DESC,id",(rs,n)->new Workout(rs.getString("id"),rs.getString("routine_name"),rs.getDate("trained_on").toLocalDate(),rs.getString("notes"),db.query("SELECT * FROM logged_set WHERE workout_id=? ORDER BY position",(s,k)->new LoggedSet(s.getString("exercise_name"),s.getBigDecimal("weight"),s.getInt("reps")),rs.getString("id"))));
  return new State(routines,workouts);
 }
 @Transactional
 public Routine save(String id,Routine input) {
  String key=id==null?UUID.randomUUID().toString():id;
  if(id!=null && db.update("UPDATE routine SET name=? WHERE id=?",input.name().trim(),key)==0) throw missing();
  if(id==null) db.update("INSERT INTO routine VALUES (?,?)",key,input.name().trim());
  db.update("DELETE FROM exercise WHERE routine_id=?",key);
  List<Exercise> exercises=new ArrayList<>(); int position=0;
  for(var e:input.exercises()) { var ex=new Exercise(UUID.randomUUID().toString(),e.name().trim(),e.sets(),e.reps()); exercises.add(ex); db.update("INSERT INTO exercise VALUES (?,?,?,?,?,?)",ex.id(),key,ex.name(),ex.sets(),ex.reps(),position++); }
  return new Routine(key,input.name().trim(),exercises);
 }
 @Transactional
 public Workout log(Workout input) {
  String key=UUID.randomUUID().toString();
  db.update("INSERT INTO workout (id,routine_name,trained_on,notes) VALUES (?,?,?,?)",key,input.routine().trim(),input.date(),input.notes().trim()); int pos=0;
  for(var s:input.sets()) db.update("INSERT INTO logged_set VALUES (?,?,?,?,?)",key,s.exercise().trim(),pos++,s.weight(),s.reps());
  return new Workout(key,input.routine().trim(),input.date(),input.notes().trim(),input.sets());
 }
 @Transactional public void deleteRoutine(String id) { if(db.update("DELETE FROM routine WHERE id=?",id)==0) throw missing(); }
 @Transactional public void deleteWorkout(String id) { if(db.update("DELETE FROM workout WHERE id=?",id)==0) throw missing(); }
 private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"No encontrado"); }
}
