package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.familychoregame.repository.GameRepository;
import org.familychoregame.api.RequestLoggingFilter;
import org.familychoregame.config.CatalogSeeder;
import org.familychoregame.service.WeekPolicy;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@WithMockUser(roles = "PARENT")
class ApiFlowIntegrationTest {
    private static final AtomicInteger NAME_SEQUENCE = new AtomicInteger();
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry, "api_flow");
    }

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired GameRepository repository;
    @Autowired WeekPolicy weekPolicy;
    @Autowired CatalogSeeder catalogSeeder;
    @Autowired RequestLoggingFilter requestLoggingFilter;
    @Autowired Jdbi jdbi;
    MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        if (repository.family().isEmpty()) {
            repository.createFamily("API Test Family", "Asia/Jerusalem", "en", java.util.List.of(), Instant.now());
            catalogSeeder.synchronize();
        }
        mvc = MockMvcBuilders.webAppContextSetup(context).defaultRequest(get("/").with(csrf())).apply(springSecurity())
                .addFilters(requestLoggingFilter).build();
    }

    @Test
    void bootstrapAndPackagedPwaExposeTheSharedApplicationContract() throws Exception {
        mvc.perform(get("/api/v1/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyId").value("default-family"))
                .andExpect(jsonPath("$.familyName").value("API Test Family"))
                .andExpect(jsonPath("$.timezone").value("Asia/Jerusalem"))
                .andExpect(jsonPath("$.categories.length()").value(10))
                .andExpect(jsonPath("$.chores.length()").value(35))
                .andExpect(jsonPath("$.currentWeek.mode").value("execution"))
                .andExpect(jsonPath("$.comingWeek.mode").value("planning"));

        assertThat(classpathText("static/index.html")).contains("id=\"syncStatus\"");
        assertThat(classpathText("static/app.js")).contains(
                "api('/bootstrap')", "familyChoreGame.preferences.v2", "XSRF-TOKEN", "X-XSRF-TOKEN");
        assertThat(classpathText("static/sw.js")).contains("startsWith('/api/')");
        assertThat(classpathText("catalog/rights.txt"))
                .contains("id|Hebrew name|English name|point cost|availability|daily cap")
                .doesNotContain("RIGHT|");
    }

    @Test
    void requestCorrelationIdIsReturnedInHeadersAndStructuredErrors() throws Exception {
        var requestId = "browser-request-123";

        mvc.perform(post("/api/v1/members")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Invalid 123\",\"role\":\"CHILD\",\"pin\":\"1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestLoggingFilter.REQUEST_ID_HEADER, requestId))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.code").value("invalid_member_name"));

        var generated = mvc.perform(get("/api/v1/bootstrap")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, "unsafe request id"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
        assertThat(generated).matches("[0-9a-f-]{36}").isNotEqualTo("unsafe request id");
        assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
    }

    @Test
    void malformedJsonAndPathDatesRemainSafeClientErrors() throws Exception {
        mvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_request"))
                .andExpect(jsonPath("$.message").value("The request could not be parsed"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mvc.perform(get("/api/v1/weeks/not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("malformed_request"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void refreshDoesNotReplaceTheSelectedMemberWhileAddChoreDialogIsOpen() throws Exception {
        var app = classpathText("static/app.js");

        assertThat(app)
                .contains("if(!$('#taskDialog').open) $('#taskMember').innerHTML=")
                .contains("setInterval(()=>{if(document.visibilityState==='visible')refresh();},15000)");
    }

    @Test
    void weekScheduleControlsArePlacedNextToTheDayBoard() throws Exception {
        var html = classpathText("static/index.html");

        assertThat(html.indexOf("id=\"previousWeek\""))
                .isGreaterThan(html.indexOf("id=\"boardTitle\""))
                .isLessThan(html.indexOf("id=\"weekBoard\""));
        assertThat(classpathText("static/styles.css")).contains(".board-schedule-row");
    }

    @Test
    void choresAndRightsAreSeparateAccessibleTabsWithChoresSelectedByDefault() throws Exception {
        var html = classpathText("static/index.html");
        var app = classpathText("static/app.js");

        assertThat(html)
                .contains("role=\"tablist\"")
                .contains("id=\"choresTab\" class=\"main-tab active\"")
                .contains("aria-selected=\"true\" aria-controls=\"choresPanel\"")
                .contains("id=\"rightsPanel\" class=\"main-tab-panel\" role=\"tabpanel\"")
                .contains("aria-labelledby=\"rightsTab\" hidden");
        assertThat(app)
                .contains("activeTab: 'chores'")
                .contains("function selectMainTab(tab, focus=false)")
                .contains("event.key==='ArrowRight'", "event.key==='Home'")
                .contains("rights:'זכויות'")
                .doesNotContain("rights:'הטבות'");
    }

    @Test
    void schoolAndVacationDayClassesUseTheRequiredGreenAndBlueColors() throws Exception {
        var app = classpathText("static/app.js");
        var styles = classpathText("static/styles.css");

        assertThat(app)
                .contains("nonSchool?'non-school-day':'school-day'")
                .contains("dayInfo?.nonSchoolDay===true");
        assertThat(styles)
                .contains(".day-column.school-day { background:#eaf7ed; }")
                .contains(".day-column.non-school-day { background:#e7f5fb; }")
                .contains(".saturday-card", "background:#e7f5fb;");
    }

    @Test
    void memberValidationAndNormalizedUniquenessReturnStructuredErrors() throws Exception {
        mvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Invalid 123\",\"role\":\"CHILD\",\"pin\":\"1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_member_name"));

        var name = uniqueName();
        createMember(name);
        mvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberRequest(name.toLowerCase()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("constraint_conflict"));
    }

    @Test
    void assignmentCreationIsIdempotentForRetriedRequestIds() throws Exception {
        var memberId = createMember(uniqueName()).get("id").asText();
        var date = weekPolicy.comingWeekStart();
        var body = new AssignmentRequest("retry-once", memberId, "sofas", date);

        var first = createAssignment(body);
        var second = createAssignment(body);

        assertThat(second.get("id").asText()).isEqualTo(first.get("id").asText());
        var weekResult = mvc.perform(get("/api/v1/weeks/{weekStart}", date))
                .andExpect(status().isOk()).andReturn();
        var assignments = objectMapper.readTree(weekResult.getResponse().getContentAsString()).get("assignments");
        assertThat(assignments).filteredOn(item -> item.get("id").asText().equals(first.get("id").asText())).hasSize(1);
    }

    @Test
    void databaseConstraintsRejectSecondMemberChoreAndSecondUseOfChoreOnSameDate() throws Exception {
        var firstMember = createMember(uniqueName()).get("id").asText();
        var secondMember = createMember(uniqueName()).get("id").asText();
        var date = weekPolicy.comingWeekStart().plusDays(1);
        createAssignment(new AssignmentRequest("constraint-first", firstMember, "coffee-table", date));

        expectAssignmentConflict(new AssignmentRequest("constraint-member", firstMember, "floor", date));
        expectAssignmentConflict(new AssignmentRequest("constraint-chore", secondMember, "coffee-table", date));
    }

    @Test
    void serverAllowsCurrentWeekEditingAndRejectsOtherWeeksAndSaturday() throws Exception {
        var memberId = createMember(uniqueName()).get("id").asText();

        var current = createAssignment(new AssignmentRequest(
                "current-week", memberId, "play-area", weekPolicy.currentWeekStart()));
        mvc.perform(delete("/api/v1/assignments/{id}", current.get("id").asText()))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(
                                "saturday", memberId, "play-area", weekPolicy.comingWeekStart().plusDays(6)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("planning_closed"));

        mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(
                                "past-week", memberId, "play-area", weekPolicy.currentWeekStart().minusWeeks(1)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("planning_closed"));
    }

    @Test
    void completionLifecycleUpdatesScoreAndRejectsStaleVersions() throws Exception {
        var member = repository.addMember(UUID.randomUUID().toString(), uniqueName(), uniqueName().toLowerCase(),
                "#1c756d", Instant.now());
        var chore = repository.chore("bathroom-counter").orElseThrow();
        var assignment = repository.addAssignment(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                member.id(), chore, weekPolicy.currentWeekStart().plusDays(2), Instant.now());

        mvc.perform(patch("/api/v1/assignments/{id}/completion", assignment.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true,\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(get("/api/v1/weeks/{weekStart}", weekPolicy.currentWeekStart()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scores[?(@.memberId=='" + member.id() + "')].points").value(2))
                .andExpect(jsonPath("$.scores[?(@.memberId=='" + member.id() + "')].completedCount").value(1));

        mvc.perform(patch("/api/v1/assignments/{id}/completion", assignment.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":false,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("stale_version"));

        mvc.perform(patch("/api/v1/assignments/{id}/completion", assignment.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":false,\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("planned"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void deletingAndCompletingAssignmentsObeyWeekModes() throws Exception {
        var memberId = createMember(uniqueName()).get("id").asText();
        var planned = createAssignment(new AssignmentRequest("delete-planned", memberId, "dining-table",
                weekPolicy.comingWeekStart().plusDays(3)));

        mvc.perform(patch("/api/v1/assignments/{id}/completion", planned.get("id").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("completion_closed"));

        mvc.perform(delete("/api/v1/assignments/{id}", planned.get("id").asText()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/weeks/{weekStart}", weekPolicy.comingWeekStart()))
                .andExpect(jsonPath("$.assignments[?(@.id=='" + planned.get("id").asText() + "')]").isEmpty());
    }

    @Test
    void archivingMemberHidesSelectionButPreservesIdentityAndAssignments() throws Exception {
        var member = createMember(uniqueName());
        var memberId = member.get("id").asText();
        var date = weekPolicy.comingWeekStart().plusDays(4);
        var assignment = createAssignment(new AssignmentRequest("archive-member", memberId, "laundry", date));

        mvc.perform(delete("/api/v1/members/{id}", memberId)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.id=='" + memberId + "')].active").value(false))
                .andExpect(jsonPath("$.comingWeek.assignments[?(@.id=='" + assignment.get("id").asText() + "')]").exists());
        assertThat(repository.members()).noneMatch(item -> item.id().equals(memberId));
        assertThat(repository.allMembers()).anyMatch(item -> item.id().equals(memberId) && !item.active());
    }

    @Test
    void malformedAndMissingResourcesReturnExpectedHttpErrors() throws Exception {
        mvc.perform(get("/api/v1/weeks/2026-09-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_week_start"));
        mvc.perform(delete("/api/v1/assignments/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("assignment_not_found"));
        mvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"));
    }

    @Test
    void weekBoardIncludesServerClassifiedSchoolAndVacationDays() throws Exception {
        mvc.perform(get("/api/v1/weeks/2026-09-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].date").value("2026-09-20"))
                .andExpect(jsonPath("$.days[0].type").value("vacation"))
                .andExpect(jsonPath("$.days[2].date").value("2026-09-22"))
                .andExpect(jsonPath("$.days[2].type").value("school-day"))
                .andExpect(jsonPath("$.days[5].date").value("2026-09-25"))
                .andExpect(jsonPath("$.days[5].type").value("vacation"));
    }

    @Test
    void simultaneousDevicesCannotAssignTheSameChoreTwice() throws Exception {
        var first = repository.addMember(UUID.randomUUID().toString(), uniqueName(), uniqueName().toLowerCase(),
                "#1c756d", Instant.now());
        var second = repository.addMember(UUID.randomUUID().toString(), uniqueName(), uniqueName().toLowerCase(),
                "#e47765", Instant.now());
        var chore = repository.chore("craft-table").orElseThrow();
        var date = weekPolicy.comingWeekStart().plusDays(5);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstAttempt = executor.submit(() -> concurrentAssignment(ready, start, first.id(), chore, date));
            var secondAttempt = executor.submit(() -> concurrentAssignment(ready, start, second.id(), chore, date));
            ready.await();
            start.countDown();

            assertThat(java.util.List.of(firstAttempt.get(), secondAttempt.get()))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(repository.assignments(date, date))
                .filteredOn(item -> item.choreId().equals(chore.id()))
                .hasSize(1);
    }

    @Test
    void rightRedemptionDeductsPointsIsIdempotentAndProtectsSpentPoints() throws Exception {
        var member = repository.addMember(UUID.randomUUID().toString(), uniqueName(), uniqueName().toLowerCase(),
                "#1c756d", Instant.now());
        var chore = repository.chore("bathroom-counter").orElseThrow();
        var assignment = repository.addAssignment(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                member.id(), chore, weekPolicy.currentWeekStart().plusDays(1), Instant.now());
        repository.setCompletion(assignment.id(), true, 0, Instant.now()).orElseThrow();
        var now = Instant.now();
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO rights(id, family_id, name_he, name_en, point_cost, availability_type, daily_cap,
                  active, created_at, updated_at)
                VALUES ('test-right', :familyId, 'פרס', 'Test right', 2, 'ALL_WEEK', 10, TRUE, :now, :now)
                ON CONFLICT(id) DO UPDATE SET active = TRUE, point_cost = 2, availability_type = 'ALL_WEEK', daily_cap = 10
                """).bind("familyId", GameRepository.FAMILY_ID).bind("now", now).execute());

        var redemption = """
                {"requestId":"right-retry","memberId":"%s","rightId":"test-right"}
                """.formatted(member.id());
        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content(redemption))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.pointCostSnapshot").value(2));
        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content(redemption))
                .andExpect(status().isCreated());

        var otherMember = createMember(uniqueName()).get("id").asText();
        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"right-retry","memberId":"%s","rightId":"test-right"}
                        """.formatted(otherMember)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_mismatch"));
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO rights(id, family_id, name_he, name_en, point_cost, availability_type, daily_cap,
                  active, created_at, updated_at)
                VALUES ('other-right', :familyId, 'פרס אחר', 'Other right', 1, 'ALL_WEEK', 10, TRUE, :now, :now)
                ON CONFLICT(id) DO UPDATE SET active = TRUE
                """).bind("familyId", GameRepository.FAMILY_ID).bind("now", now).execute());
        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"right-retry","memberId":"%s","rightId":"other-right"}
                        """.formatted(member.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_mismatch"));

        mvc.perform(get("/api/v1/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[?(@.memberId=='" + member.id() + "')].earnedPoints").value(2))
                .andExpect(jsonPath("$.balances[?(@.memberId=='" + member.id() + "')].spentPoints").value(2))
                .andExpect(jsonPath("$.balances[?(@.memberId=='" + member.id() + "')].availablePoints").value(0));

        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"right-too-expensive","memberId":"%s","rightId":"test-right"}
                        """.formatted(member.id())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("insufficient_points"));

        mvc.perform(patch("/api/v1/assignments/{id}/completion", assignment.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":false,\"version\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("points_already_spent"));
    }

    @Test
    void rightDailyCapIsEnforcedByTheServer() throws Exception {
        var member = repository.addMember(UUID.randomUUID().toString(), uniqueName(), uniqueName().toLowerCase(),
                "#e47765", Instant.now());
        var chore = repository.chore("sofas").orElseThrow();
        var assignment = repository.addAssignment(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                member.id(), chore, weekPolicy.currentWeekStart().plusDays(2), Instant.now());
        repository.setCompletion(assignment.id(), true, 0, Instant.now()).orElseThrow();
        var now = Instant.now();
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO rights(id, family_id, name_he, name_en, point_cost, availability_type, daily_cap,
                  active, created_at, updated_at)
                VALUES ('daily-cap-right', :familyId, 'פרס יומי', 'Daily right', 1, 'ALL_WEEK', 1, TRUE, :now, :now)
                ON CONFLICT(id) DO UPDATE SET active = TRUE, point_cost = 1,
                  availability_type = 'ALL_WEEK', daily_cap = 1
                """).bind("familyId", GameRepository.FAMILY_ID).bind("now", now).execute());

        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"daily-cap-first","memberId":"%s","rightId":"daily-cap-right"}
                        """.formatted(member.id())))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"daily-cap-first","memberId":"%s","rightId":"daily-cap-right"}
                        """.formatted(member.id())))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/right-redemptions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"requestId":"daily-cap-second","memberId":"%s","rightId":"daily-cap-right"}
                        """.formatted(member.id())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("daily_cap_reached"));
    }

    private JsonNode createMember(String name) throws Exception {
        var result = mvc.perform(post("/api/v1/members").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MemberRequest(name))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createAssignment(AssignmentRequest request) throws Exception {
        var result = mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void expectAssignmentConflict(AssignmentRequest request) throws Exception {
        mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("constraint_conflict"));
    }

    private static String uniqueName() {
        var sequence = NAME_SEQUENCE.incrementAndGet();
        return "Test Member " + (char) ('A' + (sequence / 26)) + (char) ('A' + (sequence % 26));
    }

    private static String classpathText(String path) throws Exception {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private boolean concurrentAssignment(CountDownLatch ready, CountDownLatch start, String memberId,
                                         org.familychoregame.domain.Chore chore, LocalDate date) throws Exception {
        ready.countDown();
        start.await();
        try {
            repository.addAssignment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), memberId,
                    chore, date, Instant.now());
            return true;
        } catch (RuntimeException expectedConstraintConflict) {
            return false;
        }
    }

    private record MemberRequest(String displayName, String role, String pin) {
        MemberRequest(String displayName) { this(displayName, "CHILD", "1234"); }
    }
    private record AssignmentRequest(String requestId, String memberId, String choreId, LocalDate assignmentDate) {}
}
