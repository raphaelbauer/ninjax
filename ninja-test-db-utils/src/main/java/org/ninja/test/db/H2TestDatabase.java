package org.ninja.test.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Fast in-memory H2 database for unit tests.
 * Provides quick startup and teardown for isolated testing.
 *
 * Example usage:
 * <pre>
 * H2TestDatabase db = H2TestDatabase.createInMemory();
 * db.start();
 * db.runMigrations("db/migration");
 * // ... use database ...
 * db.stop();
 * </pre>
 */
public class H2TestDatabase {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private HikariDataSource dataSource;

    private H2TestDatabase(String jdbcUrl, String username, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
    }

    /**
     * Create an in-memory H2 database with default settings.
     * The database name is generated to be unique.
     *
     * @return A new H2 test database instance
     */
    public static H2TestDatabase createInMemory() {
        return createInMemory("test_" + System.currentTimeMillis());
    }

    /**
     * Create an in-memory H2 database with the given name.
     *
     * @param databaseName The database name
     * @return A new H2 test database instance
     */
    public static H2TestDatabase createInMemory(String databaseName) {
        String jdbcUrl = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
        return new H2TestDatabase(jdbcUrl, "sa", "");
    }

    /**
     * Start the database by creating a connection pool.
     */
    public void start() {
        if (dataSource != null) {
            throw new IllegalStateException("Database already started");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setAutoCommit(true);

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Stop the database and close the connection pool.
     */
    public void stop() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
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
        return jdbcUrl;
    }

    /**
     * Get the username.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
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
}
