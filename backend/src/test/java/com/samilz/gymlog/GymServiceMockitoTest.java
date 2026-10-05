package com.samilz.gymlog;

import static com.samilz.gymlog.Models.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GymServiceMockitoTest {
  @Mock JdbcTemplate db;
  @InjectMocks GymService service;

  private static Exercise exercise(int reps, int repsMax, String unit) {
    return new Exercise(null, "Banca", 3, reps, repsMax, unit, false, "2");
  }

  @Test
  void saveRejectsRepsMaxBelowReps() {
    var routine = new Routine(null, "Torso", List.of(exercise(10, 8, "reps")));
    var ex = assertThrows(ResponseStatusException.class, () -> service.save("a", null, routine));
    assertEquals(400, ex.getStatusCode().value());
    verifyNoInteractions(db);
  }

  @Test
  void saveRejectsMoreThan100Reps() {
    var routine = new Routine(null, "Torso", List.of(exercise(8, 101, "reps")));
    var ex = assertThrows(ResponseStatusException.class, () -> service.save("a", null, routine));
    assertEquals(400, ex.getStatusCode().value());
    verifyNoInteractions(db);
  }

  @Test
  void saveNewRoutineTrimsNameAndInsertsIt() {
    var saved = service.save("a", null, new Routine(null, "  Torso  ", List.of(exercise(8, 8, "reps"))));
    assertNotNull(saved.id());
    assertEquals("Torso", saved.name());
    assertEquals(1, saved.exercises().size());
    verify(db).update(eq("INSERT INTO user_routine VALUES (?,?,?)"), eq(saved.id()), eq("a"), eq("Torso"));
  }

  @Test
  void saveUnknownRoutineIs404() {
    when(db.update(eq("UPDATE user_routine SET name=? WHERE id=? AND user_id=?"), eq("Torso"), eq("x"), eq("a")))
        .thenReturn(0);
    var routine = new Routine(null, "Torso", List.of(exercise(8, 8, "reps")));
    var ex = assertThrows(ResponseStatusException.class, () -> service.save("a", "x", routine));
    assertEquals(404, ex.getStatusCode().value());
  }

  @Test
  void logRejectsMoreThan100Reps() {
    var workout = new Workout(null, "Torso", LocalDate.now(), "",
        List.of(new LoggedSet("Banca", new BigDecimal("40"), 101, "reps", false, 2)));
    var ex = assertThrows(ResponseStatusException.class, () -> service.log("a", workout));
    assertEquals(400, ex.getStatusCode().value());
    verifyNoInteractions(db);
  }

  @Test
  void logStoresWorkoutAndSets() {
    var workout = new Workout(null, " Torso ", LocalDate.now(), " Bien ",
        List.of(new LoggedSet("Banca", new BigDecimal("42.5"), 8, "reps", false, 2)));
    var saved = service.log("a", workout);
    assertEquals("Torso", saved.routine());
    assertEquals("Bien", saved.notes());
    verify(db).update(eq("INSERT INTO user_workout (id,user_id,routine_name,trained_on,notes) VALUES (?,?,?,?,?)"),
        eq(saved.id()), eq("a"), eq("Torso"), eq(workout.date()), eq("Bien"));
    verify(db).update(eq("INSERT INTO user_logged_set VALUES (?,?,?,?,?,?,?,?)"),
        eq(saved.id()), eq("Banca"), eq(0), eq(new BigDecimal("42.5")), eq(8), eq("reps"), eq(false), eq(2));
  }

  @Test
  void deleteRoutineOfAnotherUserIs404() {
    when(db.update(eq("DELETE FROM user_routine WHERE id=? AND user_id=?"), eq("r1"), eq("b"))).thenReturn(0);
    var ex = assertThrows(ResponseStatusException.class, () -> service.deleteRoutine("b", "r1"));
    assertEquals(404, ex.getStatusCode().value());
  }

  @Test
  void deleteRoutineOfOwnerSucceeds() {
    when(db.update(eq("DELETE FROM user_routine WHERE id=? AND user_id=?"), eq("r1"), eq("a"))).thenReturn(1);
    assertDoesNotThrow(() -> service.deleteRoutine("a", "r1"));
  }

  @Test
  void deleteMissingWorkoutIs404() {
    when(db.update(eq("DELETE FROM user_workout WHERE id=? AND user_id=?"), eq("w1"), eq("a"))).thenReturn(0);
    var ex = assertThrows(ResponseStatusException.class, () -> service.deleteWorkout("a", "w1"));
    assertEquals(404, ex.getStatusCode().value());
  }

  @Test
  void planWithRoutineOfAnotherUserIs404() {
    when(db.queryForObject(eq("SELECT count(*) FROM user_routine WHERE id=? AND user_id=?"), eq(Integer.class),
        eq("r1"), eq("b"))).thenReturn(0);
    var plan = new Plan(LocalDate.now(), "r1");
    var ex = assertThrows(ResponseStatusException.class, () -> service.plan("b", plan));
    assertEquals(404, ex.getStatusCode().value());
    verify(db, never()).update(eq("INSERT INTO schedule(user_id,planned_on,routine_id) VALUES (?,?,?)"), any(), any(), any());
  }

  @Test
  void planUpdatesExistingDayWithoutInserting() {
    var date = LocalDate.now();
    when(db.queryForObject(eq("SELECT count(*) FROM user_routine WHERE id=? AND user_id=?"), eq(Integer.class),
        eq("r1"), eq("a"))).thenReturn(1);
    when(db.update(eq("UPDATE schedule SET routine_id=? WHERE user_id=? AND planned_on=?"), eq("r1"), eq("a"),
        eq(date))).thenReturn(1);
    service.plan("a", new Plan(date, "r1"));
    verify(db, never()).update(eq("INSERT INTO schedule(user_id,planned_on,routine_id) VALUES (?,?,?)"), any(), any(), any());
  }

  @Test
  void planInsertsWhenDayHasNoEntry() {
    var date = LocalDate.now();
    when(db.update(eq("UPDATE schedule SET routine_id=? WHERE user_id=? AND planned_on=?"), eq(null), eq("a"),
        eq(date))).thenReturn(0);
    service.plan("a", new Plan(date, null));
    verify(db).update(eq("INSERT INTO schedule(user_id,planned_on,routine_id) VALUES (?,?,?)"), eq("a"), eq(date), eq(null));
  }

  @Test
  void weeklyRequiresSevenDistinctDays() {
    var entries = List.of(new Weekly(0, null), new Weekly(0, null), new Weekly(1, null), new Weekly(2, null),
        new Weekly(3, null), new Weekly(4, null), new Weekly(5, null));
    var ex = assertThrows(ResponseStatusException.class, () -> service.weekly("a", entries));
    assertEquals(400, ex.getStatusCode().value());
    verifyNoInteractions(db);
  }
}
