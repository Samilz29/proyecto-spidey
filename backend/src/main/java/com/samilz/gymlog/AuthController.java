package com.samilz.gymlog;
import static com.samilz.gymlog.Models.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DuplicateKeyException;
import java.util.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final JdbcTemplate db;private final PasswordEncoder encoder;
 private final Map<String,Deque<Long>> attempts=new java.util.concurrent.ConcurrentHashMap<>();
 public AuthController(JdbcTemplate db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"header",token.getHeaderName());}
 @GetMapping("/me") public Map<String,String> me(Authentication auth){if(auth==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return Map.of("id",auth.getName(),"username",db.queryForObject("SELECT username FROM gym_user WHERE id=?",String.class,auth.getName()));}
 private synchronized void limit(HttpServletRequest request){long now=System.currentTimeMillis();String ip=request.getRemoteAddr();if(attempts.size()>10000)attempts.clear();var q=attempts.computeIfAbsent(ip,k->new ArrayDeque<>());while(!q.isEmpty()&&q.peekFirst()<now-900000)q.removeFirst();if(q.size()>=20)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);q.addLast(now);}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,String> register(@Valid @RequestBody Credentials c,HttpServletRequest r){limit(r);if(c.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);String id=UUID.randomUUID().toString();String username=c.username().toLowerCase(Locale.ROOT);try{db.update("INSERT INTO gym_user VALUES (?,?,?)",id,username,encoder.encode(c.password()));}catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT);}session(r,id);return Map.of("id",id,"username",username);}
 @PostMapping("/login") public Map<String,String> login(@Valid @RequestBody Credentials c,HttpServletRequest r){limit(r);var users=db.queryForList("SELECT id AS \"ID\", password_hash AS \"PASSWORD_HASH\" FROM gym_user WHERE username=?",c.username().toLowerCase(Locale.ROOT));String dummy="$2a$12$z09N1sx9OWUKc4YXQeqWWOcGwpGUUwPgVmXNnOyAUxe/J50YnwmIO";String hash=users.isEmpty()?dummy:(String)users.getFirst().get("PASSWORD_HASH");boolean ok=encoder.matches(c.password(),hash);if(users.isEmpty()||!ok)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);String id=(String)users.getFirst().get("ID");session(r,id);return Map.of("id",id,"username",c.username().toLowerCase(Locale.ROOT));}
 private void session(HttpServletRequest request,String id){request.getSession();request.changeSessionId();var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(new UsernamePasswordAuthenticationToken(id,null,List.of()));SecurityContextHolder.setContext(context);request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);}
 @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(HttpServletRequest r){var session=r.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();}
}
