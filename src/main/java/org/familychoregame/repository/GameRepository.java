package org.familychoregame.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.familychoregame.api.ApiModels.MemberBalance;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.Right;
import org.familychoregame.domain.RightRedemption;
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
            id, member_id AS memberId, right_id AS rightId,
            right_name_he_snapshot AS rightNameHeSnapshot,
            right_name_en_snapshot AS rightNameEnSnapshot,
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
                    .bind("language", defaultLanguage).bind("now", now).execute();
            for (var member : members) {
                handle.createUpdate("""
                        INSERT INTO family_members(id, family_id, display_name, normalized_name, color,
                          role, pin_hash, active, created_at)
                        VALUES (:id, :familyId, :displayName, :normalizedName, :color,
                          :role, :pinHash, TRUE, :createdAt)
                        """).bind("id", member.id()).bind("familyId", FAMILY_ID)
                        .bind("displayName", member.displayName()).bind("normalizedName", member.normalizedName())
                        .bind("color", member.color()).bind("role", member.role())
                        .bind("pinHash", member.pinHash()).bind("createdAt", now).execute();
            }
        });
    }

    public List<FamilyMember> members() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, display_name AS displayName, color, role, active, created_at AS createdAt
                FROM family_members WHERE family_id = :familyId AND active = TRUE ORDER BY created_at, id
                """).bind("familyId", FAMILY_ID).mapTo(FamilyMember.class).list());
    }

    public List<FamilyMember> allMembers() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, display_name AS displayName, color, role, active, created_at AS createdAt
                FROM family_members WHERE family_id = :familyId ORDER BY created_at, id
                """).bind("familyId", FAMILY_ID).mapTo(FamilyMember.class).list());
    }

    public List<ChoreCategory> categories() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name_he AS nameHe, name_en AS nameEn, sort_order AS sortOrder, active
                FROM chore_categories WHERE family_id = :familyId AND active = TRUE
                ORDER BY LOWER(name_en), id
                """).bind("familyId", FAMILY_ID).mapTo(ChoreCategory.class).list());
    }

    public List<Chore> chores() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, category_id AS categoryId, name_he AS nameHe, name_en AS nameEn,
                       points, sort_order AS sortOrder, active
                FROM chores WHERE family_id = :familyId AND active = TRUE
                ORDER BY LOWER(name_en), id
                """).bind("familyId", FAMILY_ID).mapTo(Chore.class).list());
    }

    public Optional<Chore> chore(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, category_id AS categoryId, name_he AS nameHe, name_en AS nameEn,
                       points, sort_order AS sortOrder, active
                FROM chores WHERE family_id = :familyId AND id = :id AND active = TRUE
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(Chore.class).findOne());
    }

    public List<Right> rights() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name_he AS nameHe, name_en AS nameEn, point_cost AS pointCost,
                       availability_type AS availabilityType, daily_cap AS dailyCap, active
                FROM rights WHERE family_id = :familyId AND active = TRUE ORDER BY LOWER(name_en), id
                """).bind("familyId", FAMILY_ID).mapTo(Right.class).list());
    }

    public Optional<Right> right(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, name_he AS nameHe, name_en AS nameEn, point_cost AS pointCost,
                       availability_type AS availabilityType, daily_cap AS dailyCap, active
                FROM rights WHERE family_id = :familyId AND id = :id AND active = TRUE
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(Right.class).findOne());
    }

    public List<MemberBalance> balances() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT m.id AS memberId,
                  COALESCE((SELECT SUM(a.points_snapshot) FROM assignments a
                    WHERE a.family_id = m.family_id AND a.member_id = m.id AND a.status = 'completed'), 0) AS earnedPoints,
                  COALESCE((SELECT SUM(r.point_cost_snapshot) FROM right_redemptions r
                    WHERE r.family_id = m.family_id AND r.member_id = m.id), 0) AS spentPoints,
                  COALESCE((SELECT SUM(a.points_snapshot) FROM assignments a
                    WHERE a.family_id = m.family_id AND a.member_id = m.id AND a.status = 'completed'), 0)
                  - COALESCE((SELECT SUM(r.point_cost_snapshot) FROM right_redemptions r
                    WHERE r.family_id = m.family_id AND r.member_id = m.id), 0) AS availablePoints
                FROM family_members m WHERE m.family_id = :familyId ORDER BY m.created_at, m.id
                """).bind("familyId", FAMILY_ID).mapTo(MemberBalance.class).list());
    }

    public int availablePoints(String memberId) {
        return jdbi.withHandle(handle -> availablePoints(handle, memberId));
    }

    public List<RightRedemption> rightRedemptions() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                FROM right_redemptions WHERE family_id = :familyId ORDER BY created_at DESC
                """).bind("familyId", FAMILY_ID).mapTo(RightRedemption.class).list());
    }

    public Optional<RightRedemption> rightRedemptionByRequestId(String requestId) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                FROM right_redemptions WHERE family_id = :familyId AND request_id = :requestId
                """).bind("familyId", FAMILY_ID).bind("requestId", requestId)
                .mapTo(RightRedemption.class).findOne());
    }

    public int redemptionsOnDate(String memberId, String rightId, LocalDate date, ZoneId zone) {
        var from = date.atStartOfDay(zone).toInstant();
        var through = date.plusDays(1).atStartOfDay(zone).toInstant();
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT COUNT(*) FROM right_redemptions
                WHERE family_id = :familyId AND member_id = :memberId AND right_id = :rightId
                  AND created_at >= :from AND created_at < :through
                """).bind("familyId", FAMILY_ID).bind("memberId", memberId).bind("rightId", rightId)
                .bind("from", from).bind("through", through).mapTo(int.class).one());
    }

    public Optional<RightRedemption> redeemRight(String id, String requestId, String memberId,
                                                       Right right, Instant now, LocalDate date, ZoneId zone) {
        var dayStart = date.atStartOfDay(zone).toInstant();
        var nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant();
        return jdbi.inTransaction(handle -> {
            lockMember(handle, memberId);
            var existing = handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                    FROM right_redemptions WHERE family_id = :familyId AND request_id = :requestId
                    """).bind("familyId", FAMILY_ID).bind("requestId", requestId)
                    .mapTo(RightRedemption.class).findOne();
            if (existing.isPresent()) return existing;

            var inserted = handle.createUpdate("""
                    INSERT INTO right_redemptions(id, family_id, member_id, right_id,
                      right_name_he_snapshot, right_name_en_snapshot, point_cost_snapshot,
                      request_id, created_at)
                    SELECT :id, :familyId, :memberId, :rightId, :nameHe, :nameEn, :cost, :requestId, :now
                    WHERE (COALESCE((SELECT SUM(points_snapshot) FROM assignments
                              WHERE family_id = :familyId AND member_id = :memberId AND status = 'completed'), 0)
                           - COALESCE((SELECT SUM(point_cost_snapshot) FROM right_redemptions
                              WHERE family_id = :familyId AND member_id = :memberId), 0)) >= :cost
                      AND (SELECT COUNT(*) FROM right_redemptions
                           WHERE family_id = :familyId AND member_id = :memberId AND right_id = :rightId
                             AND created_at >= :dayStart AND created_at < :nextDayStart) < :dailyCap
                      AND EXISTS (SELECT 1 FROM family_members
                           WHERE family_id = :familyId AND id = :memberId AND active = TRUE)
                    ON CONFLICT(family_id, request_id) DO NOTHING
                    """).bind("id", id).bind("familyId", FAMILY_ID).bind("memberId", memberId)
                    .bind("rightId", right.id()).bind("nameHe", right.nameHe())
                    .bind("nameEn", right.nameEn()).bind("cost", right.pointCost())
                    .bind("requestId", requestId).bind("now", now)
                    .bind("dayStart", dayStart).bind("nextDayStart", nextDayStart)
                    .bind("dailyCap", right.dailyCap()).execute();
            if (inserted == 1) {
                return handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                        FROM right_redemptions WHERE family_id = :familyId AND id = :id
                        """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(RightRedemption.class).findOne();
            }
            return handle.createQuery("SELECT " + REDEMPTION_COLUMNS + """
                    FROM right_redemptions WHERE family_id = :familyId AND request_id = :requestId
                    """).bind("familyId", FAMILY_ID).bind("requestId", requestId)
                    .mapTo(RightRedemption.class).findOne();
        });
    }

    public boolean activeMemberExists(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT EXISTS(SELECT 1 FROM family_members WHERE family_id = :familyId AND id = :id AND active = TRUE)
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(boolean.class).one());
    }

    public FamilyMember addMember(String id, String displayName, String normalizedName, String color,
                                  String role, String pinHash, Instant now) {
        jdbi.useHandle(handle -> handle.createUpdate("""
                INSERT INTO family_members(id, family_id, display_name, normalized_name, color,
                  role, pin_hash, active, created_at)
                VALUES (:id, :familyId, :displayName, :normalizedName, :color,
                  :role, :pinHash, TRUE, :createdAt)
                """).bind("id", id).bind("familyId", FAMILY_ID).bind("displayName", displayName)
                .bind("normalizedName", normalizedName).bind("color", color).bind("role", role)
                .bind("pinHash", pinHash).bind("createdAt", now).execute());
        return new FamilyMember(id, displayName, color, role, true, now);
    }

    public FamilyMember addMember(String id, String displayName, String normalizedName, String color, Instant now) {
        return addMember(id, displayName, normalizedName, color, "CHILD", "test-only-no-login", now);
    }

    public boolean archiveMember(String id, Instant now) {
        return jdbi.inTransaction(handle -> {
            lockMember(handle, id);
            return handle.createUpdate("""
                    UPDATE family_members SET active = FALSE, archived_at = :archivedAt
                    WHERE family_id = :familyId AND id = :id AND active = TRUE
                    """).bind("archivedAt", now).bind("familyId", FAMILY_ID).bind("id", id).execute() == 1;
        });
    }

    public List<Assignment> assignments(LocalDate from, LocalDate through) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT " + ASSIGNMENT_COLUMNS + """
                FROM assignments WHERE family_id = :familyId
                AND assignment_date BETWEEN :from AND :through ORDER BY assignment_date, created_at
                """).bind("familyId", FAMILY_ID).bind("from", from).bind("through", through)
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
                    .bind("choreId", chore.id()).bind("date", date).bind("points", chore.points())
                    .bind("requestId", requestId).bind("now", now).execute();
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
            var memberId = handle.createQuery("""
                    SELECT member_id FROM assignments WHERE family_id = :familyId AND id = :id
                    """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(String.class).findOne();
            if (memberId.isEmpty()) return Optional.empty();
            lockMember(handle, memberId.get());
            var changed = handle.createUpdate("""
                    UPDATE assignments SET status = :status, completed_at = :completedAt,
                      version = version + 1, updated_at = :updatedAt
                    WHERE family_id = :familyId AND id = :id AND version = :version
                      AND (:completed OR status <> 'completed' OR
                        (COALESCE((SELECT SUM(earned.points_snapshot) FROM assignments earned
                           WHERE earned.family_id = :familyId AND earned.member_id = assignments.member_id
                             AND earned.status = 'completed'), 0)
                         - COALESCE((SELECT SUM(spent.point_cost_snapshot) FROM right_redemptions spent
                           WHERE spent.family_id = :familyId AND spent.member_id = assignments.member_id), 0))
                        >= points_snapshot)
                    """).bind("status", completed ? "completed" : "planned")
                    .bind("completed", completed)
                    .bind("completedAt", completed ? now : null)
                    .bind("updatedAt", now).bind("familyId", FAMILY_ID)
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
                  - COALESCE((SELECT SUM(point_cost_snapshot) FROM right_redemptions
                    WHERE family_id = :familyId AND member_id = :memberId), 0)
                """).bind("familyId", FAMILY_ID).bind("memberId", memberId).mapTo(int.class).one();
    }

    private void lockMember(Handle handle, String memberId) {
        handle.createQuery("""
                SELECT id FROM family_members WHERE family_id = :familyId AND id = :memberId FOR UPDATE
                """).bind("familyId", FAMILY_ID).bind("memberId", memberId).mapTo(String.class).findOne();
    }

    public List<org.familychoregame.api.ApiModels.LoginMember> loginMembers() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, display_name AS displayName FROM family_members
                WHERE family_id = :familyId AND active = TRUE ORDER BY created_at, id
                """).bind("familyId", FAMILY_ID)
                .mapTo(org.familychoregame.api.ApiModels.LoginMember.class).list());
    }

    public Optional<AuthMember> authMember(String id) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT id, display_name AS displayName, role, pin_hash AS pinHash
                FROM family_members WHERE family_id = :familyId AND id = :id AND active = TRUE
                """).bind("familyId", FAMILY_ID).bind("id", id).mapTo(AuthMember.class).findOne());
    }

    public boolean loginLocked(String memberId, Instant now) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT EXISTS(SELECT 1 FROM member_login_attempts
                  WHERE member_id = :memberId AND locked_until > :now)
                """).bind("memberId", memberId).bind("now", now).mapTo(boolean.class).one());
    }

    public boolean recordFailedLogin(String memberId, Instant now, int maximumAttempts, Instant lockedUntil) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                INSERT INTO member_login_attempts(member_id, failed_attempts, locked_until, updated_at)
                VALUES (:memberId, 1, NULL, :now)
                ON CONFLICT (member_id) DO UPDATE SET
                  failed_attempts = CASE
                    WHEN member_login_attempts.locked_until IS NOT NULL
                         AND member_login_attempts.locked_until <= :now THEN 1
                    ELSE member_login_attempts.failed_attempts + 1
                  END,
                  locked_until = CASE
                    WHEN member_login_attempts.locked_until > :now THEN member_login_attempts.locked_until
                    WHEN (CASE
                      WHEN member_login_attempts.locked_until IS NOT NULL
                           AND member_login_attempts.locked_until <= :now THEN 1
                      ELSE member_login_attempts.failed_attempts + 1
                    END) >= :maximumAttempts THEN :lockedUntil
                    ELSE NULL
                  END,
                  updated_at = :now
                RETURNING locked_until IS NOT NULL AND locked_until > :now
                """).bind("memberId", memberId).bind("now", now)
                .bind("maximumAttempts", maximumAttempts).bind("lockedUntil", lockedUntil)
                .mapTo(boolean.class).one());
    }

    public void clearFailedLogins(String memberId) {
        jdbi.useHandle(handle -> handle.createUpdate(
                "DELETE FROM member_login_attempts WHERE member_id = :memberId")
                .bind("memberId", memberId).execute());
    }

    public record NewMember(String id, String displayName, String normalizedName, String color,
                            String role, String pinHash) {}
    public record AuthMember(String id, String displayName, String role, String pinHash) {}
}
