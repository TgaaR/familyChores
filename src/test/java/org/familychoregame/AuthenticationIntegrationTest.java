package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class AuthenticationIntegrationTest {
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry, "authentication");
    }

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired Jdbi jdbi;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).defaultRequest(get("/").with(csrf()))
                .apply(springSecurity()).build();
    }

    @Test
    void pinLoginCreatesASessionAndRolesProtectParentActions() throws Exception {
        mvc.perform(get("/api/v1/bootstrap"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content("""
                {"familyName":"Login Family","defaultLanguage":"en",
                 "members":[{"displayName":"Parent","role":"PARENT","pin":"1234"},
                            {"displayName":"Child","role":"CHILD","pin":"5678"}]}
                """))
                .andExpect(status().isCreated());

        var hashes = jdbi.withHandle(handle -> handle.createQuery(
                "SELECT pin_hash FROM family_members ORDER BY display_name").mapTo(String.class).list());
        assertThat(hashes).allSatisfy(hash -> {
            assertThat(hash).startsWith("$2");
            assertThat(hash).doesNotContain("1234", "5678");
        });

        JsonNode members = objectMapper.readTree(mvc.perform(get("/api/v1/login-members"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var parentId = idFor(members, "Parent");
        var childId = idFor(members, "Child");

        mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(childId, "9999")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_login"));

        var childLogin = mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(childId, "5678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.role").value("CHILD"))
                .andReturn();
        HttpSession childSession = childLogin.getRequest().getSession(false);

        mvc.perform(get("/api/v1/bootstrap").session((org.springframework.mock.web.MockHttpSession) childSession))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/members").session((org.springframework.mock.web.MockHttpSession) childSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Blocked\",\"role\":\"CHILD\",\"pin\":\"1111\"}"))
                .andExpect(status().isForbidden());

        var parentLogin = mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(parentId, "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PARENT"))
                .andReturn();
        var parentSession = (org.springframework.mock.web.MockHttpSession) parentLogin.getRequest().getSession(false);
        mvc.perform(post("/api/v1/members").session(parentSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"New child\",\"role\":\"CHILD\",\"pin\":\"1111\"}"))
                .andExpect(status().isCreated());

        JsonNode bootstrap = objectMapper.readTree(mvc.perform(get("/api/v1/bootstrap").session(parentSession))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var choreId = bootstrap.get("chores").get(0).get("id").asText();
        var rightId = bootstrap.get("rights").get(0).get("id").asText();
        var date = bootstrap.get("currentWeek").get("weekStart").asText();
        var assignmentRequest = objectMapper.writeValueAsString(java.util.Map.of(
                "requestId", java.util.UUID.randomUUID().toString(), "memberId", parentId,
                "choreId", choreId, "assignmentDate", date));
        JsonNode assignment = objectMapper.readTree(mvc.perform(post("/api/v1/assignments")
                        .session(parentSession).contentType(MediaType.APPLICATION_JSON).content(assignmentRequest))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mvc.perform(patch("/api/v1/assignments/{id}/completion", assignment.get("id").asText())
                        .session((org.springframework.mock.web.MockHttpSession) childSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true,\"version\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/right-redemptions")
                        .session((org.springframework.mock.web.MockHttpSession) childSession)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(java.util.Map.of(
                                "requestId", java.util.UUID.randomUUID().toString(), "memberId", parentId,
                                "rightId", rightId))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/logout").session(parentSession))
                .andExpect(status().isNoContent());

        JsonNode updatedMembers = objectMapper.readTree(mvc.perform(get("/api/v1/login-members"))
                .andReturn().getResponse().getContentAsString());
        var newChildId = idFor(updatedMembers, "New child");
        for (int attempt = 1; attempt < 5; attempt++) {
            mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(newChildId, "9999")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(newChildId, "9999")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("too_many_login_attempts"));
        mvc.perform(post("/api/v1/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(newChildId, "1111")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void invalidCsrfTokensAreRejected() throws Exception {
        var browserMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        browserMvc.perform(get("/api/v1/session"))
                .andExpect(status().isOk());

        browserMvc.perform(post("/api/v1/login").with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"unknown\",\"pin\":\"1234\"}"))
                .andExpect(status().isForbidden());
    }

    private String idFor(JsonNode members, String displayName) {
        for (var member : members) if (displayName.equals(member.get("displayName").asText())) {
            return member.get("id").asText();
        }
        throw new AssertionError("Missing login member " + displayName);
    }

    private String loginJson(String memberId, String pin) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("memberId", memberId, "pin", pin));
    }
}
