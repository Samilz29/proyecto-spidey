package com.samilz.gymlog;
import static com.samilz.gymlog.Models.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:tests;DB_CLOSE_DELAY=-1")
@Transactional
class GymServiceTest {
 @Autowired GymService service;
 @Autowired org.springframework.jdbc.core.JdbcTemplate db;
 @org.junit.jupiter.api.BeforeEach void setup(){db.update("INSERT INTO gym_user VALUES (?,?,?)","a","alice","hash");db.update("INSERT INTO gym_user VALUES (?,?,?)","b","bob","hash");}
 @Test void routineRoundTrip(){var r=service.save("a",null,new Routine(null,"Torso",List.of(new Exercise(null,"Press banca",3,8,8,"reps",false,"2"))));assertNotNull(r.id());assertEquals("Press banca",service.state("a").routines().getFirst().exercises().getFirst().name());}
 @Test void editReplacesExercises(){var r=service.save("a",null,new Routine(null,"Torso",List.of(new Exercise(null,"Banca",3,8,8,"reps",false,"2"))));service.save("a",r.id(),new Routine(null,"Pierna",List.of(new Exercise(null,"Sentadilla",4,6,6,"reps",false,"2"))));assertEquals(1,service.state("a").routines().getFirst().exercises().size());assertEquals("Sentadilla",service.state("a").routines().getFirst().exercises().getFirst().name());}
 @Test void deletingRoutinePreservesHistory(){var r=service.save("a",null,new Routine(null,"Torso",List.of(new Exercise(null,"Banca",3,8,8,"reps",false,"2"))));service.log("a",new Workout(null,"Torso",LocalDate.now(),"Bien",List.of(new LoggedSet("Banca",new BigDecimal("42.5"),8,"reps",false,2))));service.deleteRoutine("a",r.id());assertTrue(service.state("a").routines().isEmpty());assertEquals(1,service.state("a").workouts().size());}
 @Test void workoutPrecisionAndDeletion(){var w=service.log("a",new Workout(null,"Torso",LocalDate.now(),"",List.of(new LoggedSet("Banca",new BigDecimal("42.25"),8,"reps",false,2))));assertEquals(0,new BigDecimal("42.25").compareTo(service.state("a").workouts().getFirst().sets().getFirst().weight()));service.deleteWorkout("a",w.id());assertTrue(service.state("a").workouts().isEmpty());}
 @Test void missingRoutineIs404(){assertEquals(404,assertThrows(ResponseStatusException.class,()->service.deleteRoutine("a","missing")).getStatusCode().value());}
 @Test void dataIsIsolated(){var r=service.save("a",null,new Routine(null,"Private",List.of(new Exercise(null,"Banca",3,8,10,"reps",false,"2"))));assertTrue(service.state("b").routines().isEmpty());assertThrows(ResponseStatusException.class,()->service.deleteRoutine("b",r.id()));assertThrows(ResponseStatusException.class,()->service.plan("b",new Plan(LocalDate.now(),r.id())));}
 @Test void calendarRoundTrip(){var r=service.save("a",null,new Routine(null,"Plan",List.of(new Exercise(null,"Banca",3,8,10,"reps",false,"2"))));service.plan("a",new Plan(LocalDate.now(),r.id()));assertEquals(r.id(),service.state("a").plans().getFirst().routineId());assertTrue(service.state("b").plans().isEmpty());}
}
