package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.familychoregame.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SwaggerIntegrationTest {
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry, "swagger");
    }

    @Autowired WebApplicationContext context;
    @Autowired GameRepository repository;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository.clearAllData();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void publicApiDocsDescribeRequestsSessionAuthenticationAndDestructiveReset() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Family Chores API"))
                .andExpect(jsonPath("$.paths['/api/v1/data'].delete.description").value(
                        org.hamcrest.Matchers.containsString("Preserves schema_migrations")))
                .andExpect(jsonPath("$.paths['/api/v1/data'].delete.security[0].memberSession").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/login'].post.requestBody").exists())
                .andExpect(jsonPath("$.paths['/api/v1/assignments'].post.requestBody").exists())
                .andExpect(jsonPath("$.paths['/api/v1/weeks/{weekStart}'].get.parameters[0].name").value("weekStart"))
                .andExpect(jsonPath("$.components.securitySchemes.memberSession.in").value("cookie"))
                .andExpect(jsonPath("$.paths['/actuator/health']").doesNotExist());
        mvc.perform(get("/api/v1/bootstrap")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerAssetsAndConfigLoadAndIssueACsrfCookieWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")))
                .andExpect(cookie().exists("XSRF-TOKEN"));
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
        mvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("requestInterceptor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("XSRF-TOKEN")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("X-XSRF-TOKEN")));
    }

    @Test
    void swaggerCookieAndHeaderFlowSupportsSetupLoginAndParentWritesWithoutDisablingCsrf() throws Exception {
        var csrfCookie = mvc.perform(get("/swagger-ui/index.html")).andReturn()
                .getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        mvc.perform(post("/api/v1/families").cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"familyName":"Swagger Test","defaultLanguage":"en",
                         "members":[{"displayName":"Parent","role":"PARENT","pin":"1234"}]}
                        """))
                .andExpect(status().isCreated());
        var parentId = repository.members().get(0).id();
        var login = mvc.perform(post("/api/v1/login").cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + parentId + "\",\"pin\":\"1234\"}"))
                .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var refreshedToken = mvc.perform(get("/api/v1/session").session(session)).andReturn()
                .getResponse().getCookie("XSRF-TOKEN");
        assertThat(refreshedToken).isNotNull();
        var memberJson = "{\"displayName\":\"Child\",\"role\":\"CHILD\",\"pin\":\"5678\"}";
        mvc.perform(post("/api/v1/members").session(session).contentType(MediaType.APPLICATION_JSON).content(memberJson))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/members").session(session).cookie(refreshedToken)
                        .header("X-XSRF-TOKEN", refreshedToken.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(memberJson))
                .andExpect(status().isCreated());
    }
}


