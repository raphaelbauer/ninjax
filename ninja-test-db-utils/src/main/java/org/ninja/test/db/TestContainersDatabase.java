package org.ninja.test.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Real PostgreSQL database using TestContainers for integration tests.
 * Provides a containerized PostgreSQL instance for more realistic testing.
 *
 * Example usage:
 * <pre>
 * TestContainersDatabase db = new TestContainersDatabase();
 * db.start();
 * db.runMigrations("db/migration");
 * // ... use database ...
 * db.stop();
 * </pre>
 */
public class TestContainersDatabase {

    private PostgreSQLContainer<?> postgresContainer;
    private HikariDataSource dataSource;

    /**
     * Create a new TestContainers database with PostgreSQL.
     */
    public TestContainersDatabase() {
        // Use default PostgreSQL version
        this("postgres:15-alpine");
    }

    /**
     * Create a new TestContainers database with a specific PostgreSQL image.
     *
     * @param dockerImageName The Docker image name (e.g., "postgres:15-alpine")
     */
    public TestContainersDatabase(String dockerImageName) {
        this.postgresContainer = new PostgreSQLContainer<>(dockerImageName)
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");
    }

    /**
     * Start the PostgreSQL container and create a connection pool.
     */
    public void start() {
        if (postgresContainer.isRunning()) {
            throw new IllegalStateException("Container already started");
        }

        postgresContainer.start();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgresContainer.getJdbcUrl());
        config.setUsername(postgresContainer.getUsername());
        config.setPassword(postgresContainer.getPassword());
        config.setMaximumPoolSize(5);
        config.setAutoCommit(true);

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Stop the PostgreSQL container and close the connection pool.
     */
    public void stop() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        if (postgresContainer != null) {
            postgresContainer.stop();
        }
    }

    /**
     * Get the DataSource for this database.
     *
     * @return The DataSource
     */
    public DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("Database not started. Call start() first.");
        }
        return dataSource;
    }

    /**
     * Get a connection to the database.
     *
     * @return A database connection
     * @throws SQLException If a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Get the JDBC URL.
     *
     * @return The JDBC URL
     */
    public String getJdbcUrl() {
        if (!postgresContainer.isRunning()) {
            throw new IllegalStateException("Container not started. Call start() first.");
        }
        return postgresContainer.getJdbcUrl();
    }

    /**
     * Get the username.
     *
     * @return The username
     */
    public String getUsername() {
        return postgresContainer.getUsername();
    }

    /**
     * Get the password.
     *
     * @return The password
     */
    public String getPassword() {
        return postgresContainer.getPassword();
    }

    /**
     * Run Flyway migrations from the given location.
     *
     * @param migrationLocation The classpath location of migrations (e.g., "db/migration")
     */
    public void runMigrations(String migrationLocation) {
        if (dataSource == null) {
            throw new IllegalStateException("Database not started. Call start() first.");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationLocation)
                .load();

        flyway.migrate();
    }

    /**
     * Clean the database by dropping all objects.
     * Useful for resetting state between tests.
     */
    public void clean() {
        if (dataSource == null) {
            throw new IllegalStateException("Database not started. Call start() first.");
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .load();

        flyway.clean();
    }

    /**
     * Get the PostgreSQL container for advanced operations.
     *
     * @return The PostgreSQL container
     */
    public PostgreSQLContainer<?> getContainer() {
        return postgresContainer;
    }
}
