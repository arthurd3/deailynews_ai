package com.arthur.newsbrief;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that load the full application context, which needs a database.
 *
 * <p>One container for the whole JVM, started once and never stopped explicitly. Registering
 * it as a {@code @ServiceConnection} bean instead would hand its lifecycle to Spring, which
 * stops it when the first context closes — leaving later test classes pointing at a dead
 * container, because they use different properties and so get different contexts.
 *
 * <p>Testcontainers 2.x moved the container to {@code org.testcontainers.postgresql}; the 1.x
 * coordinate no longer produces Spring Boot connection details.
 *
 * <p>{@link #databaseIsReachable()} guards the whole hierarchy. Docker being installed is not
 * enough — a host whose published container ports do not forward traffic will start the
 * container happily and then fail every connection. Rather than reporting that as a broken
 * test suite, these tests skip with a reason and the rest of the suite still runs.
 */
@EnabledIf(value = "com.arthur.newsbrief.PostgresBackedTest#databaseIsReachable",
        disabledReason = "Docker is unavailable, or its published ports do not forward traffic on this host")
public abstract class PostgresBackedTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private static final boolean REACHABLE = startAndProbe();

    public static boolean databaseIsReachable() {
        return REACHABLE;
    }

    private static boolean startAndProbe() {
        try {
            POSTGRES.start();
            // Starting is not the same as being reachable: prove a connection actually works
            // before letting a whole context try and fail on it.
            try (Connection ignored = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
                return true;
            }
        }
        catch (Exception ex) {
            System.err.println("[tests] Postgres-backed tests skipped: " + ex);
            return false;
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
