package com.samilz.gymlog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
public final class Models {
 private Models() {}
 public record Exercise(String id, @NotBlank @Size(max=100) String name, @Min(1) @Max(20) int sets, @Min(1) @Max(100) int reps) {}
 public record Routine(String id, @NotBlank @Size(max=80) String name, @NotEmpty @Size(max=30) List<@Valid Exercise> exercises) {}
 public record LoggedSet(@NotBlank @Size(max=100) String exercise, @NotNull @DecimalMin("0") @DecimalMax("1000") @Digits(integer=4,fraction=2) BigDecimal weight, @Min(1) @Max(100) int reps) {}
 public record Workout(String id, @NotBlank @Size(max=80) String routine, @NotNull @PastOrPresent LocalDate date, @NotNull @Size(max=1000) String notes, @NotEmpty @Size(max=300) List<@Valid LoggedSet> sets) {}
 public record State(List<Routine> routines,List<Workout> workouts) {}
}
