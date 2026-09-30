package org.familychoregame.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.familychoregame.api.ApiModels.MemberBalance;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.Benefit;
import org.familychoregame.domain.BenefitRedemption;
import org.familychoregame.domain.Chore;
import org.familychoregame.domain.ChoreCategory;
import org.familychoregame.domain.FamilyMember;
import org.familychoregame.domain.FamilyProfile;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

@Repository
public class GameRepository {
    public static final String FAMILY_ID = "default-family";
    private static final String ASSIGNMENT_COLUMNS = """
            id, member_id AS memberId, chore_id AS choreId, assignment_date AS assignmentDate,
            points_snapshot AS pointsSnapshot, status, completed_at AS completedAt, version,
            created_at AS createdAt, updated_at AS updatedAt
            """;
    private static final String REDEMPTION_COLUMNS = """
            id, member_id AS memberId, benefit_id AS benefitId,
            benefit_name_he_snapshot AS benefitNameHeSnapshot,
            benefit_name_en_snapshot AS benefitNameEnSnapshot,
            point_cost_snapshot AS pointCostSnapshot, created_at AS createdAt
            """;
    private final Jdbi jdbi;

    public GameRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Optional<FamilyProfile> family() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name, timezone, default_language AS defaultLanguage
                FROM families WHERE id = :familyId
                """).bind("familyId", FAMILY_ID).mapTo(FamilyProfile.class).findOne());
    }

    public void createFamily(String name, String timezone, String defaultLanguage,
                             List<NewMember> members, Instant now) {
        jdbi.useTransaction(handle -> {
            handle.createUpdate("""
                    INSERT INTO families(id, name, timezone, default_language, created_at, updated_at)
                    VALUES (:id, :name, :timezone, :language, :now, :now)
                    """).bind("id", FAMILY_ID).bind("name", name).bind("timezone", timezone)
                    .bind("language", defaultLanguage).bind("now", now.toString()).execute();
            for (var member : members) {
                handle.createUpdate("""
                        INSERT INTO family_members(id, family_id, display_name, normalized_name, color, active, created_at)
                        VALUES (:id, :familyId, :displayName, :normalizedName, :color, 1, :createdAt)
                        """).bind("id", member.id()).bind("familyId", FAMILY_ID)
                        .bind("displayName", member.displayName()).bind("normalizedName", member.normalizedName())
                        .bind("color", member.color()).bind("createdAt", now.toString()).execute();
            }
        });
    }

    public List<FamilyMember> members() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, display_name AS displayName, color, active, created_at AS createdAt
                FROM family_members WHERE family_id = :familyId AND active = 1 ORDER BY created_at, id
                """).bind("familyId", FAMILY_ID).mapTo(FamilyMember.class).list());
    }

    public List<FamilyMember> allMembers() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, display_name AS displayName, color, active, created_at AS createdAt
                FROM family_members WHERE family_id = :familyId ORDER BY created_at, id
                """).bind("familyId", FAMILY_ID).mapTo(FamilyMember.class).list());
    }

    public List<ChoreCategory> categories() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name_he AS nameHe, name_en AS nameEn, sort_order AS sortOrder, active
                FROM chore_categories WHERE family_id = :familyId AND active = 1
                ORDER BY name_en COLLATE NOCASE, id
                """).bind("familyId", FAMILY_ID).mapTo(ChoreCategory.class).list());
    }

    public List<Chore> chores() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, category_id AS categoryId, name_he AS nameHe, name_en AS nameEn,
                       points, sort_order AS sortOrder, active
                FROM chores WHERE family_id = :familyId AND active = 1
                ORDER BY name_en COLLATE NOCASE, id
                """).bind("familyId", FAMILY_ID).mapTo(Chore.class).list());
    }

    public Optional<Chore> chore(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, category_id AS categoryId, name_he AS nameHe, name_en AS nameEn,
                       points, sort_order AS sortOrder, active
                FROM chores WHERE family_id = :familyId AND id = :id AND active = 1
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(Chore.class).findOne());
    }

    public List<Benefit> benefits() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name_he AS nameHe, name_en AS nameEn, point_cost AS pointCost,
                       availability_type AS availabilityType, daily_cap AS dailyCap, active
                FROM benefits WHERE family_id = :familyId AND active = 1 ORDER BY name_en COLLATE NOCASE, id
                """).bind("familyId", FAMILY_ID).mapTo(Benefit.class).list());
    }

    public Optional<Benefit> benefit(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name_he AS nameHe, name_en AS nameEn, point_cost AS pointCost,
                       availability_type AS availabilityType, daily_cap AS dailyCap, active
                FROM benefits WHERE family_id = :familyId AND id = :id AND active = 1
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(Benefit.class).findOne());
    }

    public List<MemberBalance> balances() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT m.id AS memberId,
                  COALESCE((SELECT SUM(a.points_snapshot) FROM assignments a
                    WHERE a.family_id = m.family_id AND a.member_id = m.id AND a.status = 'completed'), 0) AS earnedPoints,
                  COALESCE((SELECT SUM(r.point_cost_snapshot) FROM benefit_redemptions r
                    WHERE r.family_id = m.family_id AND r.member_id = m.id), 0) AS spentPoints,
                  COALESCE((SELECT SUM(a.points_snapshot) FROM assignments a
                    WHERE a.family_id = m.family_id AND a.member_id = m.id AND a.status = 'completed'), 0)
                  - COALESCE((SELECT SUM(r.point_cost_snapshot) FROM benefit_redemptions r
                    WHERE r.family_id = m.family_id AND r.member_id = m.id), 0) AS availablePoints
                FROM family_members m WHERE m.family_id = :familyId ORDER BY m.created_at, m.id
                """).bind("familyId", FAMILY_ID).mapTo(MemberBalance.class).list());
    }

    public int availablePoints(String memberId) {
        return jdbi.withHandle(handle -> availablePoints(handle, memberId));
    }

    public List<BenefitRedemption> benefitRedemptions() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                FROM benefit_redemptions WHERE family_id = :familyId ORDER BY created_at DESC
                """).bind("familyId", FAMILY_ID).mapTo(BenefitRedemption.class).list());
    }

    public Optional<BenefitRedemption> benefitRedemptionByRequestId(String requestId) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                FROM benefit_redemptions WHERE family_id = :familyId AND request_id = :requestId
                """).bind("familyId", FAMILY_ID).bind("requestId", requestId)
                .mapTo(BenefitRedemption.class).findOne());
    }

    public int redemptionsOnDate(String memberId, String benefitId, LocalDate date, ZoneId zone) {
        var from = date.atStartOfDay(zone).toInstant();
        var through = date.plusDays(1).atStartOfDay(zone).toInstant();
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT COUNT(*) FROM benefit_redemptions
                WHERE family_id = :familyId AND member_id = :memberId AND benefit_id = :benefitId
                  AND created_at >= :from AND created_at < :through
                """).bind("familyId", FAMILY_ID).bind("memberId", memberId).bind("benefitId", benefitId)
                .bind("from", from.toString()).bind("through", through.toString()).mapTo(int.class).one());
    }

    public Optional<BenefitRedemption> redeemBenefit(String id, String requestId, String memberId,
                                                       Benefit benefit, Instant now, LocalDate date, ZoneId zone) {
        var dayStart = date.atStartOfDay(zone).toInstant().toString();
        var nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant().toString();
        return jdbi.withHandle(handle -> {
            var existing = handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                    FROM benefit_redemptions WHERE family_id = :familyId AND request_id = :requestId
                    """).bind("familyId", FAMILY_ID).bind("requestId", requestId)
                    .mapTo(BenefitRedemption.class).findOne();
            if (existing.isPresent()) return existing;

            var inserted = handle.createUpdate("""
                    INSERT INTO benefit_redemptions(id, family_id, member_id, benefit_id,
                      benefit_name_he_snapshot, benefit_name_en_snapshot, point_cost_snapshot,
                      request_id, created_at)
                    SELECT :id, :familyId, :memberId, :benefitId, :nameHe, :nameEn, :cost, :requestId, :now
                    WHERE (COALESCE((SELECT SUM(points_snapshot) FROM assignments
                              WHERE family_id = :familyId AND member_id = :memberId AND status = 'completed'), 0)
                           - COALESCE((SELECT SUM(point_cost_snapshot) FROM benefit_redemptions
                              WHERE family_id = :familyId AND member_id = :memberId), 0)) >= :cost
                      AND (SELECT COUNT(*) FROM benefit_redemptions
                           WHERE family_id = :familyId AND member_id = :memberId AND benefit_id = :benefitId
                             AND created_at >= :dayStart AND created_at < :nextDayStart) < :dailyCap
                      AND EXISTS (SELECT 1 FROM family_members
                           WHERE family_id = :familyId AND id = :memberId AND active = 1)
                    ON CONFLICT(family_id, request_id) DO NOTHING
                    """).bind("id", id).bind("familyId", FAMILY_ID).bind("memberId", memberId)
                    .bind("benefitId", benefit.id()).bind("nameHe", benefit.nameHe())
                    .bind("nameEn", benefit.nameEn()).bind("cost", benefit.pointCost())
                    .bind("requestId", requestId).bind("now", now.toString())
                    .bind("dayStart", dayStart).bind("nextDayStart", nextDayStart)
                    .bind("dailyCap", benefit.dailyCap()).execute();
            if (inserted == 1) {
                return handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                        FROM benefit_redemptions WHERE family_id = :familyId AND id = :id
                        """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(BenefitRedemption.class).findOne();
            }
            return handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                    FROM benefit_redemptions WHERE family_id = :familyId AND request_id = :requestId
                    """).bind("familyId", FAMILY_ID).bind("requestId", requestId)
                    .mapTo(BenefitRedemption.class).findOne();
        });
    }

    public boolean activeMemberExists(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT EXISTS(SELECT 1 FROM family_members WHERE family_id = :familyId AND id = :id AND active = 1)
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(boolean.class).one());
    }

    public FamilyMember addMember(String id, String displayName, String normalizedName, String color, Instant now) {
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO family_members(id, family_id, display_name, normalized_name, color, active, created_at)
                VALUES (:id, :familyId, :displayName, :normalizedName, :color, 1, :createdAt)
                """).bind("id", id).bind("familyId", FAMILY_ID).bind("displayName", displayName)
                .bind("normalizedName", normalizedName).bind("color", color).bind("createdAt", now.toString()).execute());
        return new FamilyMember(id, displayName, color, true, now);
    }

    public boolean archiveMember(String id, Instant now) {
        return jdbi.withHandle(handle -> handle.createUpdate("""
                UPDATE family_members SET active = 0, archived_at = :archivedAt
                WHERE family_id = :familyId AND id = :id AND active = 1
                """).bind("archivedAt", now.toString()).bind("familyId", FAMILY_ID).bind("id", id).execute() == 1);
    }

    public List<Assignment> assignments(LocalDate from, LocalDate through) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " + ASSIGNMENT_COLUMNS + """
                FROM assignments WHERE family_id = :familyId
                AND assignment_date BETWEEN :from AND :through ORDER BY assignment_date, created_at
                """).bind("familyId", FAMILY_ID).bind("from", from.toString()).bind("through", through.toString())
                .mapTo(Assignment.class).list());
    }

    public Optional<Assignment> assignment(String id) {
        return jdbi.withHandle(handle -> findAssignment(handle, id));
    }

    public Assignment addAssignment(String id, String requestId, String memberId, Chore chore,
                                    LocalDate date, Instant now) {
        return jdbi.inTransaction(handle -> {
            var existing = handle.createQuery("SELECT " + ASSIGNMENT_COLUMNS + """
                    FROM assignments WHERE family_id = :familyId AND request_id = :requestId
                    """).bind("familyId", FAMILY_ID).bind("requestId", requestId).mapTo(Assignment.class).findOne();
            if (existing.isPresent()) {
                return existing.get();
            }
            handle.createUpdate("""
                    INSERT INTO assignments(id, family_id, member_id, chore_id, assignment_date,
                      points_snapshot, status, request_id, created_at, updated_at)
                    VALUES (:id, :familyId, :memberId, :choreId, :date, :points, 'planned', :requestId, :now, :now)
                    """).bind("id", id).bind("familyId", FAMILY_ID).bind("memberId", memberId)
                    .bind("choreId", chore.id()).bind("date", date.toString()).bind("points", chore.points())
                    .bind("requestId", requestId).bind("now", now.toString()).execute();
            return findAssignment(handle, id).orElseThrow();
        });
    }

    public boolean deleteAssignment(String id) {
        return jdbi.withHandle(handle -> handle.createUpdate(
                "DELETE FROM assignments WHERE family_id = :familyId AND id = :id")
                .bind("familyId", FAMILY_ID).bind("id", id).execute() == 1);
    }

    public Optional<Assignment> setCompletion(String id, boolean completed, long expectedVersion, Instant now) {
        return jdbi.inTransaction(handle -> {
            var changed = handle.createUpdate("""
                    UPDATE assignments SET status = :status, completed_at = :completedAt,
                      version = version + 1, updated_at = :updatedAt
                    WHERE family_id = :familyId AND id = :id AND version = :version
                      AND (:completed = 1 OR status <> 'completed' OR
                        (COALESCE((SELECT SUM(earned.points_snapshot) FROM assignments earned
                           WHERE earned.family_id = :familyId AND earned.member_id = assignments.member_id
                             AND earned.status = 'completed'), 0)
                         - COALESCE((SELECT SUM(spent.point_cost_snapshot) FROM benefit_redemptions spent
                           WHERE spent.family_id = :familyId AND spent.member_id = assignments.member_id), 0))
                        >= points_snapshot)
                    """).bind("status", completed ? "completed" : "planned")
                    .bind("completed", completed)
                    .bind("completedAt", completed ? now.toString() : null)
                    .bind("updatedAt", now.toString()).bind("familyId", FAMILY_ID)
                    .bind("id", id).bind("version", expectedVersion).execute();
            return changed == 1 ? findAssignment(handle, id) : Optional.empty();
        });
    }

    private Optional<Assignment> findAssignment(Handle handle, String id) {
        return handle.createQuery("SELECT " + ASSIGNMENT_COLUMNS + """
                FROM assignments WHERE family_id = :familyId AND id = :id
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(Assignment.class).findOne();
    }

    private int availablePoints(Handle handle, String memberId) {
        return handle.createQuery("""
                SELECT COALESCE((SELECT SUM(points_snapshot) FROM assignments
                    WHERE family_id = :familyId AND member_id = :memberId AND status = 'completed'), 0)
                  - COALESCE((SELECT SUM(point_cost_snapshot) FROM benefit_redemptions
                    WHERE family_id = :familyId AND member_id = :memberId), 0)
                """).bind("familyId", FAMILY_ID).bind("memberId", memberId).mapTo(int.class).one();
    }

    public record NewMember(String id, String displayName, String normalizedName, String color) {}
}
