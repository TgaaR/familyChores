package org.familychoregame.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class DatabaseMigrator implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrator.class);
    private static final List<String> MIGRATIONS = List.of("db/migration/V1__initial_schema.sql");
    private final Jdbi jdbi;

    public DatabaseMigrator(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void run(ApplicationArguments args) {
        var appliedMigrations = new ArrayList<String>();
        jdbi.useTransaction(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS schema_migrations (version TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL)");
            for (String migration : MIGRATIONS) {
                if (!handle.createQuery("SELECT EXISTS(SELECT 1 FROM schema_migrations WHERE version = :version)")
                        .bind("version", migration).mapTo(boolean.class).one()) {
                    LOGGER.atInfo().addKeyValue("event", "database_migration_started")
                            .addKeyValue("migration", migration).log("Applying database migration");
                    for (String statement : readStatements(migration)) {
                        handle.execute(statement);
                    }
                    handle.createUpdate("INSERT INTO schema_migrations(version, applied_at) VALUES (:version, :appliedAt)")
                            .bind("version", migration)
                            .bind("appliedAt", java.time.Instant.now())
                            .execute();
                    appliedMigrations.add(migration);
                }
            }
        });
        appliedMigrations.forEach(migration -> LOGGER.atInfo()
                .addKeyValue("event", "database_migration_applied")
                .addKeyValue("migration", migration)
                .log("Database migration applied"));
        LOGGER.atInfo().addKeyValue("event", "database_migrations_ready")
                .addKeyValue("migration_count", MIGRATIONS.size()).log("Database migrations ready");
    }

    private List<String> readStatements(String path) {
        try (var input = new ClassPathResource(path).getInputStream()) {
            var sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.stripLeading().startsWith("--"))
                    .reduce("", (left, right) -> left + "\n" + right);
            return Arrays.stream(sql.split(";"))
                    .map(String::strip)
                    .filter(statement -> !statement.isEmpty())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read database migration " + path, exception);
        }
    }
}
