package com.samilz.gymlog;
import static com.samilz.gymlog.Models.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.security.Principal;
import java.time.LocalDate;
@RestController @RequestMapping("/api")
public class GymController {
 private final GymService service;
 public GymController(GymService service){this.service=service;}
 @PutMapping("/weekly") @ResponseStatus(HttpStatus.NO_CONTENT) public void weekly(Principal p,@Valid @RequestBody java.util.List<@Valid Weekly> entries){service.weekly(p.getName(),entries);}
 @GetMapping("/state") public State state(Principal p){return service.state(p.getName());}
 @PostMapping("/routines") @ResponseStatus(HttpStatus.CREATED) public Routine create(Principal p,@Valid @RequestBody Routine body){return service.save(p.getName(),null,body);}
 @PutMapping("/routines/{id}") public Routine update(Principal p,@PathVariable String id,@Valid @RequestBody Routine body){return service.save(p.getName(),id,body);}
 @DeleteMapping("/routines/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Principal p,@PathVariable String id){service.deleteRoutine(p.getName(),id);}
 @PostMapping("/workouts") @ResponseStatus(HttpStatus.CREATED) public Workout log(Principal p,@Valid @RequestBody Workout body){return service.log(p.getName(),body);}
 @DeleteMapping("/workouts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteWorkout(Principal p,@PathVariable String id){service.deleteWorkout(p.getName(),id);}
 @PutMapping("/plans") @ResponseStatus(HttpStatus.NO_CONTENT) public void plan(Principal p,@Valid @RequestBody Plan body){service.plan(p.getName(),body);}
 @DeleteMapping("/plans/{date}") @ResponseStatus(HttpStatus.NO_CONTENT) public void unplan(Principal p,@PathVariable LocalDate date){service.unplan(p.getName(),date);}
}
