package org.familychoregame;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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

@SpringBootTest
class FamilyOnboardingIntegrationTest {
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry, "onboarding");
    }

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).defaultRequest(get("/").with(csrf()))
                .apply(springSecurity()).build();
    }

    @Test
    void freshInstallRequiresAtomicFamilyAndMemberSetupBeforeBoard() throws Exception {
        mvc.perform(get("/api/v1/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true))
                .andExpect(jsonPath("$.authenticated").value(false));

        var request = """
                {"familyName":"משפחת ישראלי","defaultLanguage":"he",
                 "members":[{"displayName":"נועה","role":"PARENT","pin":"1234"},
                            {"displayName":"דני","role":"CHILD","pin":"5678"}]}
                """;
        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content("""
                        {"familyName":"Duplicate Test","defaultLanguage":"en",
                         "members":[{"displayName":"Dana","role":"PARENT","pin":"1234"},
                                    {"displayName":"dana","role":"CHILD","pin":"5678"}]}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("duplicate_member_name"));

        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setupRequired").value(false))
                .andExpect(jsonPath("$.familyName").value("משפחת ישראלי"))
                .andExpect(jsonPath("$.defaultLanguage").value("he"))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.categories.length()").value(10))
                .andExpect(jsonPath("$.chores.length()").value(35));

        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("family_already_configured"));
    }
}
