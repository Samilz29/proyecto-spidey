package com.samilz.gymlog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SecurityTest {
 @Autowired MockMvc mvc;
 @Test void anonymousStateDenied()throws Exception{mvc.perform(get("/api/state")).andExpect(status().isUnauthorized());}
 @Test void registrationRequiresCsrf()throws Exception{mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"username\":\"abc\",\"password\":\"this-is-a-test-password\"}")).andExpect(status().isForbidden());}
 @Test void rejectsWeakPassword()throws Exception{mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"username\":\"abc\",\"password\":\"short\"}")).andExpect(status().isBadRequest());}
 @Test void noBasicAuthBypass()throws Exception{mvc.perform(get("/api/state").with(httpBasic("user","password"))).andExpect(status().isUnauthorized());}
 @Test void signedInRequestStillNeedsCsrf()throws Exception{mvc.perform(post("/api/routines").with(user("a")).contentType("application/json").content("{}")).andExpect(status().isForbidden());}
}
