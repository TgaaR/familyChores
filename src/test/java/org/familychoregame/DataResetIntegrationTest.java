package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.familychoregame.config.DatabaseMigrator;
import org.familychoregame.repository.GameRepository;
import org.jdbi.v3.core.Jdbi;
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
class DataResetIntegrationTest {
    private static final java.util.List<String> DATA_TABLES = java.util.List.of(
            "assignments", "right_redemptions", "member_login_attempts", "chores",
            "chore_categories", "rights", "family_members", "families");
    private static final String SETUP = """
            {"familyName":"Reset Test","defaultLanguage":"en",
             "members":[{"displayName":"Parent","role":"PARENT","pin":"1234"},
                        {"displayName":"Child","role":"CHILD","pin":"5678"}]}
            """;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry, "data_reset");
    }

    @Autowired WebApplicationContext context;
    @Autowired GameRepository repository;
    @Autowired Jdbi jdbi;
    @Autowired DatabaseMigrator migrator;
    MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        repository.clearAllData();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(post("/api/v1/families").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SETUP))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousAndChildrenCannotResetData() throws Exception {
        mvc.perform(delete("/api/v1/data").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/data").with(csrf()).with(user("child").roles("CHILD")))
                .andExpect(status().isForbidden());
        assertThat(count("families")).isEqualTo(1);
        assertThat(count("chores")).isEqualTo(35);
    }

    @Test
    void parentMustSupplyAValidCsrfToken() throws Exception {
        mvc.perform(delete("/api/v1/data").with(user("parent").roles("PARENT")))
                .andExpect(status().isForbidden());
        assertThat(count("family_members")).isEqualTo(2);
    }

    @Test
    void resetClearsAllApplicationTablesPreservesMigrationsAndAllowsFreshSetup() throws Exception {
        var parentSession = login("PARENT", "1234");
        var otherParentSession = login("PARENT", "1234");
        var childSession = login("CHILD", "5678");
        seedActivity();
        DATA_TABLES.forEach(table -> assertThat(count(table)).as(table).isPositive());
        var migrationsBefore = migrationHistory();

        mvc.perform(delete("/api/v1/data").session(parentSession).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(parentSession.isInvalid()).isTrue();
        DATA_TABLES.forEach(table -> assertThat(count(table)).as(table).isZero());
        assertThat(migrationHistory()).isEqualTo(migrationsBefore);
        migrator.run(null);
        DATA_TABLES.forEach(table -> assertThat(count(table)).as(table).isZero());
        mvc.perform(get("/api/v1/session"))
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.setupRequired").value(true));
        mvc.perform(post("/api/v1/families").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(SETUP))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chores.length()").value(35));
        // Sessions from the deleted family cannot access or reset the newly created family.
        mvc.perform(get("/api/v1/bootstrap").session(otherParentSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/bootstrap").session(childSession)).andExpect(status().isUnauthorized());
        assertThat(otherParentSession.isInvalid()).isTrue();
        assertThat(childSession.isInvalid()).isTrue();
        assertThat(count("families")).isEqualTo(1);
    }

    @Test
    void failedResetRollsBackWithoutLoggingOutTheParent() throws Exception {
        var parentSession = login("PARENT", "1234");
        seedActivity();
        jdbi.useHandle(handle -> {
            handle.execute("""
                    CREATE FUNCTION reject_test_reset() RETURNS trigger LANGUAGE plpgsql AS $$
                    BEGIN RAISE EXCEPTION 'Reset blocked by test'; END $$
                    """);
            handle.execute("CREATE TRIGGER reject_test_reset BEFORE TRUNCATE ON rights FOR EACH STATEMENT EXECUTE FUNCTION reject_test_reset()");
        });
        try {
            mvc.perform(delete("/api/v1/data").session(parentSession).with(csrf()))
                    .andExpect(status().isInternalServerError());
            DATA_TABLES.forEach(table -> assertThat(count(table)).as(table).isPositive());
            assertThat(parentSession.isInvalid()).isFalse();
        } finally {
            jdbi.useHandle(handle -> {
                handle.execute("DROP TRIGGER reject_test_reset ON rights");
                handle.execute("DROP FUNCTION reject_test_reset()");
            });
        }
    }

    private MockHttpSession login(String role, String pin) throws Exception {
        var memberId = jdbi.withHandle(handle -> handle.createQuery(
                "SELECT id FROM family_members WHERE role = :role").bind("role", role).mapTo(String.class).one());
        return (MockHttpSession) mvc.perform(post("/api/v1/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"" + memberId + "\",\"pin\":\"" + pin + "\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private void seedActivity() {
        jdbi.useHandle(handle -> {
            handle.execute("""
                    INSERT INTO assignments(id,family_id,member_id,chore_id,assignment_date,points_snapshot,
                        status,completed_at,request_id,version,created_at,updated_at)
                    SELECT 'reset-assignment',m.family_id,m.id,c.id,CURRENT_DATE,100,'completed',NOW(),
                        'reset-assignment-request',0,NOW(),NOW()
                    FROM family_members m CROSS JOIN chores c WHERE m.role='PARENT' LIMIT 1
                    """);
            handle.execute("""
                    INSERT INTO right_redemptions(id,family_id,member_id,right_id,right_name_he_snapshot,
                        right_name_en_snapshot,point_cost_snapshot,request_id,created_at)
                    SELECT 'reset-redemption',m.family_id,m.id,r.id,r.name_he,r.name_en,r.point_cost,
                        'reset-redemption-request',NOW()
                    FROM family_members m CROSS JOIN rights r WHERE m.role='PARENT' LIMIT 1
                    """);
            handle.execute("""
                    INSERT INTO member_login_attempts(member_id,failed_attempts,updated_at)
                    SELECT id,1,NOW() FROM family_members WHERE role='CHILD'
                    """);
        });
    }

    private int count(String table) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM " + table).mapTo(int.class).one());
    }

    private java.util.List<String> migrationHistory() {
        return jdbi.withHandle(handle -> handle.createQuery(
                "SELECT version || ':' || applied_at::text FROM schema_migrations ORDER BY version")
                .mapTo(String.class).list());
    }
}
