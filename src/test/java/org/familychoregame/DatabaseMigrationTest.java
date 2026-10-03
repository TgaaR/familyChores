package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.familychoregame.config.DatabaseMigrator;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class DatabaseMigrationTest {
    private static final String BASELINE = "db/migration/V1__initial_schema.sql";

    @Test
    void freshDatabaseCreatesTheCompleteBaselineSchema() {
        var jdbi = database("fresh_baseline");

        new DatabaseMigrator(jdbi).run(null);

        assertThat(appliedMigrations(jdbi)).containsExactly(BASELINE);
        assertThat(tables(jdbi)).contains(
                "families", "family_members", "chore_categories", "chores", "assignments",
                "rights", "right_redemptions", "member_login_attempts", "schema_migrations");
        assertThat(columns(jdbi, "rights")).contains(
                "point_cost", "availability_type", "daily_cap", "active");
        assertThat(columns(jdbi, "family_members")).contains("role", "pin_hash");
        assertThat(indexes(jdbi)).contains(
                "assignments_family_date_idx",
                "right_redemptions_family_member_idx",
                "right_redemptions_daily_cap_idx");
    }

    @Test
    void restartingDoesNotReapplyTheBaseline() {
        var jdbi = database("repeat_baseline");
        var migrator = new DatabaseMigrator(jdbi);

        migrator.run(null);
        migrator.run(null);

        assertThat(appliedMigrations(jdbi)).containsExactly(BASELINE);
        assertThat(tables(jdbi)).contains("rights", "right_redemptions");
    }

    @Test
    void failedBaselineRollsBackMigrationTrackingAndLeavesExistingDataReadable() {
        var jdbi = database("failed_baseline");
        jdbi.useHandle(handle -> {
            handle.execute("CREATE TABLE families (id TEXT PRIMARY KEY)");
            handle.execute("INSERT INTO families(id) VALUES ('preexisting-row')");
        });

        assertThatThrownBy(() -> new DatabaseMigrator(jdbi).run(null)).isInstanceOf(RuntimeException.class);

        assertThat(tables(jdbi)).contains("families").doesNotContain("schema_migrations", "family_members");
        String existingId = jdbi.withHandle(handle -> handle.createQuery(
                "SELECT id FROM families").mapTo(String.class).one());
        assertThat(existingId).isEqualTo("preexisting-row");
    }

    private Jdbi database(String testName) {
        var schema = PostgresTestDatabase.createSchema(testName);
        return Jdbi.create(PostgresTestDatabase.jdbcUrl(schema),
                PostgresTestDatabase.username(), PostgresTestDatabase.password()).installPlugins();
    }

    private java.util.List<String> appliedMigrations(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery(
                "SELECT version FROM schema_migrations ORDER BY version").mapTo(String.class).list());
    }

    private java.util.List<String> tables(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = current_schema()
                """).mapTo(String.class).list());
    }

    private java.util.List<String> columns(Jdbi jdbi, String table) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = :table
                """).bind("table", table).mapTo(String.class).list());
    }

    private java.util.List<String> indexes(Jdbi jdbi) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                SELECT indexname FROM pg_indexes WHERE schemaname = current_schema()
                """).mapTo(String.class).list());
    }
}
