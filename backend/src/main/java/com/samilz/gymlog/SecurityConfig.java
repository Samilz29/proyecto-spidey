package com.samilz.gymlog;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@Configuration
public class SecurityConfig {
 @Bean org.springframework.security.core.userdetails.UserDetailsService noDefaultUser(){return username->{throw new org.springframework.security.core.userdetails.UsernameNotFoundException(username);};}
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception {
  return http.authorizeHttpRequests(a->a.requestMatchers("/api/auth/**").permitAll().requestMatchers("/api/**").authenticated().anyRequest().permitAll())
   .csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
   .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->s.sendError(401)))
   .formLogin(f->f.disable()).httpBasic(b->b.disable()).logout(l->l.disable())
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; worker-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'")))
   .build();
 }
}
