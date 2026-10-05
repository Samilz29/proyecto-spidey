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
 @Test void routineRoundTrip(){var r=service.save(null,new Routine(null,"Torso",List.of(new Exercise(null,"Press banca",3,8))));assertNotNull(r.id());assertEquals("Press banca",service.state().routines().getFirst().exercises().getFirst().name());}
 @Test void editReplacesExercises(){var r=service.save(null,new Routine(null,"Torso",List.of(new Exercise(null,"Banca",3,8))));service.save(r.id(),new Routine(null,"Pierna",List.of(new Exercise(null,"Sentadilla",4,6))));assertEquals(1,service.state().routines().getFirst().exercises().size());assertEquals("Sentadilla",service.state().routines().getFirst().exercises().getFirst().name());}
 @Test void deletingRoutinePreservesHistory(){var r=service.save(null,new Routine(null,"Torso",List.of(new Exercise(null,"Banca",3,8))));service.log(new Workout(null,"Torso",LocalDate.now(),"Bien",List.of(new LoggedSet("Banca",new BigDecimal("42.5"),8))));service.deleteRoutine(r.id());assertTrue(service.state().routines().isEmpty());assertEquals(1,service.state().workouts().size());}
 @Test void workoutPrecisionAndDeletion(){var w=service.log(new Workout(null,"Torso",LocalDate.now(),"",List.of(new LoggedSet("Banca",new BigDecimal("42.25"),8))));assertEquals(0,new BigDecimal("42.25").compareTo(service.state().workouts().getFirst().sets().getFirst().weight()));service.deleteWorkout(w.id());assertTrue(service.state().workouts().isEmpty());}
 @Test void missingRoutineIs404(){assertEquals(404,assertThrows(ResponseStatusException.class,()->service.deleteRoutine("missing")).getStatusCode().value());}
}
