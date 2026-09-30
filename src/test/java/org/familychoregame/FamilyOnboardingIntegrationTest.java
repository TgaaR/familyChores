package org.familychoregame;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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
    private static final String TEST_DATABASE = System.getProperty("java.io.tmpdir")
            + "/family-onboarding-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("family-chore-game.database-path", () -> TEST_DATABASE);
    }

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void freshInstallRequiresAtomicFamilyAndMemberSetupBeforeBoard() throws Exception {
        mvc.perform(get("/api/v1/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true))
                .andExpect(jsonPath("$.members").isEmpty())
                .andExpect(jsonPath("$.chores").isEmpty());

        var request = """
                {"familyName":"משפחת ישראלי","defaultLanguage":"he",
                 "memberNames":["נועה","דני"]}
                """;
        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content("""
                        {"familyName":"Duplicate Test","defaultLanguage":"en",
                         "memberNames":["Dana","dana"]}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("duplicate_member_name"));

        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setupRequired").value(false))
                .andExpect(jsonPath("$.familyName").value("משפחת ישראלי"))
                .andExpect(jsonPath("$.defaultLanguage").value("he"))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.categories.length()").value(7))
                .andExpect(jsonPath("$.chores.length()").value(28));

        mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("family_already_configured"));
    }
}
