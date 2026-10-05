package com.samilz.gymlog;
import static com.samilz.gymlog.Models.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api")
public class GymController {
 private final GymService service;
 public GymController(GymService service) { this.service=service; }
 @GetMapping("/state") public State state(){return service.state();}
 @PostMapping("/routines") @ResponseStatus(HttpStatus.CREATED) public Routine create(@Valid @RequestBody Routine body){return service.save(null,body);}
 @PutMapping("/routines/{id}") public Routine update(@PathVariable String id,@Valid @RequestBody Routine body){return service.save(id,body);}
 @DeleteMapping("/routines/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id){service.deleteRoutine(id);}
 @PostMapping("/workouts") @ResponseStatus(HttpStatus.CREATED) public Workout log(@Valid @RequestBody Workout body){return service.log(body);}
 @DeleteMapping("/workouts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteWorkout(@PathVariable String id){service.deleteWorkout(id);}
}
