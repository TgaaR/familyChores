package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import org.familychoregame.config.DatabaseMigrator;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

class DatabaseMigrationUpgradeTest {
    @TempDir Path temporaryDirectory;

    @Test
    void v1DatabaseUpgradesThroughBenefitsAndDailyCaps() throws Exception {
        var jdbi = database("v1-upgrade.db");
        initializeMigrationTracking(jdbi);
        applyAndRecord(jdbi, "db/migration/V1__initial_schema.sql");

        var migrator = new DatabaseMigrator(jdbi);
        migrator.run(null);

        assertThat(appliedMigrations(jdbi)).containsExactly(
                "db/migration/V1__initial_schema.sql",
                "db/migration/V2__benefits.sql",
                "db/migration/V3__benefit_availability_and_caps.sql");
        assertThat(benefitColumns(jdbi)).contains("availability_type", "daily_cap");
    }

    @Test
    void v2BenefitAndRedemptionDataSurviveV3AndRestart() throws Exception {
        var jdbi = database("v2-upgrade.db");
        initializeMigrationTracking(jdbi);
        applyAndRecord(jdbi, "db/migration/V1__initial_schema.sql");
        applyAndRecord(jdbi, "db/migration/V2__benefits.sql");
        insertExistingRedemption(jdbi);

        var migrator = new DatabaseMigrator(jdbi);
        migrator.run(null);
        migrator.run(null);

        assertThat(appliedMigrations(jdbi)).hasSize(3);
        String redemptionSnapshot = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT benefit_name_en_snapshot || ':' || point_cost_snapshot
                FROM benefit_redemptions WHERE id = 'redemption-before-v3'
                """).mapTo(String.class).one());
        String upgradedBenefit = jdbi.withHandle(handle -> handle.createQuery("""
                SELECT availability_type || ':' || daily_cap FROM benefits WHERE id = 'existing-benefit'
                """).mapTo(String.class).one());
        assertThat(redemptionSnapshot).isEqualTo("Existing benefit:2");
        assertThat(upgradedBenefit).isEqualTo("ALL_WEEK:1");
    }

    @Test
    void failedMigrationIsNotRecordedAndExistingDataRemainsReadable() throws Exception {
        var jdbi = database("failed-upgrade.db");
        initializeMigrationTracking(jdbi);
        applyAndRecord(jdbi, "db/migration/V1__initial_schema.sql");
        applyAndRecord(jdbi, "db/migration/V2__benefits.sql");
        insertExistingRedemption(jdbi);
        jdbi.useHandle(handle -> handle.execute(
                "ALTER TABLE benefits ADD COLUMN availability_type TEXT NOT NULL DEFAULT 'ALL_WEEK'"));

        assertThatThrownBy(() -> new DatabaseMigrator(jdbi).run(null)).isInstanceOf(RuntimeException.class);

        assertThat(appliedMigrations(jdbi))
                .doesNotContain("db/migration/V3__benefit_availability_and_caps.sql");
        assertThat(benefitColumns(jdbi)).doesNotContain("daily_cap");
        int redemptionCount = jdbi.withHandle(handle -> handle.createQuery(
                "SELECT COUNT(*) FROM benefit_redemptions WHERE id = 'redemption-before-v3'")
                .mapTo(int.class).one());
        assertThat(redemptionCount).isEqualTo(1);
    }

    private Jdbi database(String filename) {
        return Jdbi.create("jdbc:sqlite:" + temporaryDirectory.resolve(filename).toAbsolutePath());
    }

    private void initializeMigrationTracking(Jdbi jdbi) {
        jdbi.useHandle(handle -> handle.execute("""
                CREATE TABLE schema_migrations (version TEXT PRIMARY KEY, applied_at TEXT NOT NULL)
                """));
    }

    private void applyAndRecord(Jdbi jdbi, String migration) throws Exception {
        var sql = classpathText(migration).lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .reduce("", (left, right) -> left + "\n" + right);
        jdbi.useTransaction(handle -> {
            Arrays.stream(sql.split(";")).map(String::strip).filter(statement -> !statement.isEmpty())
                    .forEach(handle::execute);
            handle.createUpdate("INSERT INTO schema_migrations(version, applied_at) VALUES (:version, :now)")
                    .bind("version", migration).bind("now", Instant.now().toString()).execute();
        });
    }

    private void insertExistingRedemption(Jdbi jdbi) {
        var now = Instant.now().toString();
        jdbi.useTransaction(handle -> {
            handle.createUpdate("""
                    INSERT INTO families(id, name, timezone, default_language, created_at, updated_at)
                    VALUES ('default-family', 'Migration Family', 'Asia/Jerusalem', 'en', :now, :now)
                    """).bind("now", now).execute();
            handle.createUpdate("""
                    INSERT INTO family_members(id, family_id, display_name, normalized_name, color, active, created_at)
                    VALUES ('existing-member', 'default-family', 'Existing Member', 'existing member', '#1c756d', 1, :now)
                    """).bind("now", now).execute();
            handle.createUpdate("""
                    INSERT INTO benefits(id, family_id, name_he, name_en, point_cost, active, created_at, updated_at)
                    VALUES ('existing-benefit', 'default-family', 'הטבה קיימת', 'Existing benefit', 2, 1, :now, :now)
                    """).bind("now", now).execute();
            handle.createUpdate("""
                    INSERT INTO benefit_redemptions(id, family_id, member_id, benefit_id,
                      benefit_name_he_snapshot, benefit_name_en_snapshot, point_cost_snapshot, request_id, created_at)
                    VALUES ('redemption-before-v3', 'default-family', 'existing-member', 'existing-benefit',
                      'הטבה קיימת', 'Existing benefit', 2, 'existing-request', :now)
                    """).bind("now", now).execute();
        });
    }

    private java.util.List<String> appliedMigrations(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery(
                "SELECT version FROM schema_migrations ORDER BY version").mapTo(String.class).list());
    }

    private java.util.List<String> benefitColumns(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery(
                "SELECT name FROM pragma_table_info('benefits')").mapTo(String.class).list());
    }

    private String classpathText(String path) throws Exception {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
