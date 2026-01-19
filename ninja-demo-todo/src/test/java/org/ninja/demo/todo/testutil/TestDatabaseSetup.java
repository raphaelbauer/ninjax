package org.ninja.demo.todo.testutil;

import org.ninja.db.flyway.NinjaFlywayMigrator;
import org.ninja.test.db.DatabaseFixture;
import org.ninja.test.db.DatabaseVerifier;
import org.ninja.test.db.H2TestDatabase;
import org.ninja.test.db.JdbiTestHelper;
import org.ninja.db.jdbi.NinjaJdbi;

/**
 * Shared database setup logic for tests.
 * Provides convenient access to database utilities and configuration.
 *
 * Example usage:
 * <pre>
 * TestDatabaseSetup dbSetup = TestDatabaseSetup.create();
 * dbSetup.start();
 * dbSetup.runMigrations();
 *
 * // Use utilities
 * dbSetup.getFixture().clearTable("tasks");
 * dbSetup.getVerifier().assertTableCount("tasks", 0);
 *
 * dbSetup.stop();
 * </pre>
 */
public class TestDatabaseSetup {

    private final H2TestDatabase database;
    private DatabaseFixture fixture;
    private DatabaseVerifier verifier;
    private NinjaJdbi ninjaJdbi;

    private TestDatabaseSetup(H2TestDatabase database) {
        this.database = database;
    }

    /**
     * Create a new test database setup with default configuration.
     *
     * @return A new test database setup
     */
    public static TestDatabaseSetup create() {
        return new TestDatabaseSetup(H2TestDatabase.createInMemory());
    }

    /**
     * Start the database and initialize utilities.
     */
    public void start() {
        database.start();
        this.fixture = new DatabaseFixture(database.getDataSource());
        this.verifier = new DatabaseVerifier(database.getDataSource());
        this.ninjaJdbi = JdbiTestHelper.createNinjaJdbi(database.getDataSource());
    }

    /**
     * Stop the database.
     */
    public void stop() {
        database.stop();
    }

    /**
     * Run Flyway migrations for the demo todo app.
     */
    public void runMigrations() {
        //NinjaFlywayMigrator ninjaFlywayMigrator = new NinjaFlywayMigrator(ninjaDatasourceConfigProvider.get());

        database.runMigrations("migrations/default");
    }

    /**
     * Clean the database (drop all objects).
     */
    public void clean() {
        database.clean();
    }

    /**
     * Clear all tasks from the tasks table.
     */
    public void clearTasks() {
        fixture.clearTable("tasks");
    }

    /**
     * Get the underlying H2 test database.
     *
     * @return The database
     */
    public H2TestDatabase getDatabase() {
        return database;
    }

    /**
     * Get the database fixture for test data setup.
     *
     * @return The fixture
     */
    public DatabaseFixture getFixture() {
        return fixture;
    }

    /**
     * Get the database verifier for assertions.
     *
     * @return The verifier
     */
    public DatabaseVerifier getVerifier() {
        return verifier;
    }

    /**
     * Get the NinjaJdbi instance.
     *
     * @return The NinjaJdbi instance
     */
    public NinjaJdbi getNinjaJdbi() {
        return ninjaJdbi;
    }
}
