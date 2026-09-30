package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.familychoregame.api.ApiModels.CompletionRequest;
import static org.familychoregame.api.ApiModels.RedeemBenefitRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.familychoregame.api.ApiException;
import org.familychoregame.config.CatalogSeeder;
import org.familychoregame.repository.GameRepository;
import org.familychoregame.service.GameService;
import org.familychoregame.service.WeekPolicy;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class BenefitConcurrencyIntegrationTest {
    private static final String TEST_DATABASE = System.getProperty("java.io.tmpdir")
            + "/family-benefit-concurrency-" + UUID.randomUUID() + ".db";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("family-chore-game.database-path", () -> TEST_DATABASE);
    }

    @Autowired GameRepository repository;
    @Autowired GameService service;
    @Autowired WeekPolicy weekPolicy;
    @Autowired CatalogSeeder catalogSeeder;
    @Autowired Jdbi jdbi;

    @BeforeEach
    void configureFamily() {
        if (repository.family().isEmpty()) {
            repository.createFamily("Concurrency Family", "Asia/Jerusalem", "en", List.of(), Instant.now());
            catalogSeeder.synchronize();
        }
    }

    @Test
    void simultaneousRedemptionsCannotSpendTheSameFinalPoint() throws Exception {
        var memberId = completedMember("sofas", 0);
        insertBenefit("final-point", 1, 10);

        var results = race(
                () -> redeem("final-point-a", memberId, "final-point"),
                () -> redeem("final-point-b", memberId, "final-point"));

        assertThat(results).containsExactlyInAnyOrder("success", "insufficient_points");
        assertThat(repository.availablePoints(memberId)).isZero();
        assertThat(repository.benefitRedemptions()).filteredOn(item -> item.memberId().equals(memberId)).hasSize(1);
    }

    @Test
    void simultaneousRedemptionsCannotBothUseTheFinalDailyCapSlot() throws Exception {
        var memberId = completedMember("bathroom-counter", 1);
        insertBenefit("final-cap", 1, 1);

        var results = race(
                () -> redeem("final-cap-a", memberId, "final-cap"),
                () -> redeem("final-cap-b", memberId, "final-cap"));

        assertThat(results).containsExactlyInAnyOrder("success", "daily_cap_reached");
        assertThat(repository.availablePoints(memberId)).isEqualTo(1);
        assertThat(repository.benefitRedemptions()).filteredOn(item -> item.memberId().equals(memberId)).hasSize(1);
    }

    @Test
    void redemptionRacingWithUncompletionNeverCreatesANegativeBalance() throws Exception {
        var memberId = completedMember("floor", 2);
        var assignment = repository.assignments(weekPolicy.currentWeekStart().plusDays(2),
                weekPolicy.currentWeekStart().plusDays(2)).stream()
                .filter(item -> item.memberId().equals(memberId)).findFirst().orElseThrow();
        insertBenefit("race-uncompletion", 1, 10);

        var results = race(
                () -> redeem("race-uncompletion-request", memberId, "race-uncompletion"),
                () -> complete(assignment.id(), false, 1));

        assertThat(results).satisfiesExactlyInAnyOrder(
                result -> assertThat(result).isIn("success", "insufficient_points"),
                result -> assertThat(result).isIn("success", "points_already_spent"));
        assertThat(repository.availablePoints(memberId)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void archivingRacingWithRedemptionLeavesConsistentMemberHistory() throws Exception {
        var memberId = completedMember("coffee-table", 3);
        insertBenefit("race-archive", 1, 10);

        var results = race(
                () -> redeem("race-archive-request", memberId, "race-archive"),
                () -> archive(memberId));

        assertThat(results).contains("success");
        assertThat(repository.allMembers()).anyMatch(item -> item.id().equals(memberId) && !item.active());
        assertThat(repository.availablePoints(memberId)).isGreaterThanOrEqualTo(0);
        assertThat(repository.benefitRedemptions().stream().filter(item -> item.memberId().equals(memberId)).count())
                .isBetween(0L, 1L);
    }

    private String completedMember(String choreId, int dayOffset) {
        var suffix = UUID.randomUUID().toString();
        var member = repository.addMember(suffix, "Member " + suffix.substring(0, 6), "member " + suffix,
                "#1c756d", Instant.now());
        var assignment = repository.addAssignment(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                member.id(), repository.chore(choreId).orElseThrow(),
                weekPolicy.currentWeekStart().plusDays(dayOffset), Instant.now());
        repository.setCompletion(assignment.id(), true, 0, Instant.now()).orElseThrow();
        return member.id();
    }

    private void insertBenefit(String id, int cost, int cap) {
        var now = Instant.now().toString();
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO benefits(id, family_id, name_he, name_en, point_cost, availability_type, daily_cap,
                  active, created_at, updated_at)
                VALUES (:id, :familyId, :id, :id, :cost, 'ALL_WEEK', :cap, 1, :now, :now)
                """).bind("id", id).bind("familyId", GameRepository.FAMILY_ID).bind("cost", cost)
                .bind("cap", cap).bind("now", now).execute());
    }

    private String redeem(String requestId, String memberId, String benefitId) {
        try {
            service.redeemBenefit(new RedeemBenefitRequest(requestId, memberId, benefitId));
            return "success";
        } catch (ApiException exception) {
            return exception.code();
        }
    }

    private String complete(String assignmentId, boolean completed, long version) {
        try {
            service.setCompletion(assignmentId, new CompletionRequest(completed, version));
            return "success";
        } catch (ApiException exception) {
            return exception.code();
        }
    }

    private String archive(String memberId) {
        try {
            service.archiveMember(memberId);
            return "success";
        } catch (ApiException exception) {
            return exception.code();
        }
    }

    private List<String> race(java.util.concurrent.Callable<String> first,
                              java.util.concurrent.Callable<String> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> { ready.countDown(); start.await(); return first.call(); });
            var secondResult = executor.submit(() -> { ready.countDown(); start.await(); return second.call(); });
            ready.await();
            start.countDown();
            return List.of(firstResult.get(), secondResult.get());
        }
    }
}
