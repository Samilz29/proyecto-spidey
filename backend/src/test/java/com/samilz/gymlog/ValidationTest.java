package com.samilz.gymlog;
import static com.samilz.gymlog.Models.*;
import static org.junit.jupiter.api.Assertions.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
class ValidationTest {
 private final jakarta.validation.Validator validator=Validation.buildDefaultValidatorFactory().getValidator();
 @Test void rejectsNegativeWeights(){assertFalse(validator.validate(new LoggedSet("Banca",new BigDecimal("-1"),8,"reps",false,2)).isEmpty());}
 @Test void rejectsMissingExercises(){assertFalse(validator.validate(new Routine(null,"Rutina",List.of())).isEmpty());}
 @Test void rejectsFutureSessions(){assertFalse(validator.validate(new Workout(null,"Torso",LocalDate.now().plusDays(1),"",List.of(new LoggedSet("Banca",BigDecimal.ZERO,8,"reps",false,2)))).isEmpty());}
 @Test void acceptsBodyweightAndDecimals(){assertTrue(validator.validate(new LoggedSet("Dominadas",BigDecimal.ZERO,8,"reps",false,2)).isEmpty());assertTrue(validator.validate(new LoggedSet("Banca",new BigDecimal("42.25"),8,"reps",false,2)).isEmpty());}
 @Test void nestedValidationRejectsBadSets(){assertFalse(validator.validate(new Routine(null,"Torso",List.of(new Exercise(null,"Banca",0,8,10,"reps",false,"2")))).isEmpty());}
 @Test void rejectsTooManyDecimals(){assertFalse(validator.validate(new LoggedSet("Banca",new BigDecimal("42.251"),8,"reps",false,2)).isEmpty());}
}
