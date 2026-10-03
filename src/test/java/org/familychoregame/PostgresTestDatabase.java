package org.familychoregame;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

final class PostgresTestDatabase {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("family_chore_game_test")
            .withUsername("family_chore_game")
            .withPassword("family_chore_game");

    private PostgresTestDatabase() {}

    static String configure(DynamicPropertyRegistry registry, String testName) {
        start();
        var schema = (testName + "_" + UUID.randomUUID()).replace('-', '_').toLowerCase(Locale.ROOT);
        provisionSchema(schema);
        registry.add("family-chore-game.database-url",
                () -> withSchema(schema));
        registry.add("family-chore-game.database-username", POSTGRES::getUsername);
        registry.add("family-chore-game.database-password", POSTGRES::getPassword);
        return schema;
    }

    static synchronized void start() {
        if (!POSTGRES.isRunning()) POSTGRES.start();
    }

    static String createSchema(String testName) {
        start();
        var schema = (testName + "_" + UUID.randomUUID()).replace('-', '_').toLowerCase(Locale.ROOT);
        provisionSchema(schema);
        return schema;
    }

    static String jdbcUrl(String schema) {
        return withSchema(schema);
    }

    static String username() {
        return POSTGRES.getUsername();
    }

    static String password() {
        return POSTGRES.getPassword();
    }

    private static void provisionSchema(String schema) {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        } catch (SQLException exception) {
            throw new IllegalStateException("Cannot create PostgreSQL test schema", exception);
        }
    }

    private static String withSchema(String schema) {
        var separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        return POSTGRES.getJdbcUrl() + separator + "currentSchema=" + schema;
    }
}
