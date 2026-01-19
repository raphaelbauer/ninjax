package org.ninja.test.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.ninja.db.jdbi.NinjaJdbi;
import org.ninja.db.jdbi.NinjaJdbiImpl;
import org.ninja.db.jdbc.NinjaDatasource;
import org.ninja.db.jdbc.NinjaDatasources;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

/**
 * Helper for creating test JDBI and NinjaJdbi instances.
 *
 * Example usage:
 * <pre>
 * H2TestDatabase db = H2TestDatabase.createInMemory();
 * db.start();
 *
 * NinjaJdbi ninjaJdbi = JdbiTestHelper.createNinjaJdbi(db.getDataSource());
 * Jdbi jdbi = JdbiTestHelper.createJdbi(db.getDataSource());
 * </pre>
 */
public class JdbiTestHelper {

    private JdbiTestHelper() {
        // Utility class
    }

    /**
     * Create a NinjaJdbi instance from a test database.
     *
     * @param testDatabase The test database
     * @return A NinjaJdbi instance
     */
    public static NinjaJdbi createNinjaJdbi(H2TestDatabase testDatabase) {
        Objects.requireNonNull(testDatabase, "testDatabase");
        return createNinjaJdbi(testDatabase.getDataSource());
    }

    /**
     * Create a NinjaJdbi instance from a TestContainers database.
     *
     * @param testDatabase The test database
     * @return A NinjaJdbi instance
     */
    public static NinjaJdbi createNinjaJdbi(TestContainersDatabase testDatabase) {
        Objects.requireNonNull(testDatabase, "testDatabase");
        return createNinjaJdbi(testDatabase.getDataSource());
    }

    /**
     * Create a NinjaJdbi instance from a data source.
     *
     * @param dataSource The data source
     * @return A NinjaJdbi instance
     */
    public static NinjaJdbi createNinjaJdbi(DataSource dataSource) {
        return createNinjaJdbi(dataSource, "default");
    }

    /**
     * Create a NinjaJdbi instance from a data source with a specific name.
     *
     * @param dataSource The data source
     * @param datasourceName The datasource name
     * @return A NinjaJdbi instance
     */
    public static NinjaJdbi createNinjaJdbi(DataSource dataSource, String datasourceName) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(datasourceName, "datasourceName");

        NinjaDatasource ninjaDatasource = new NinjaDatasource(datasourceName, dataSource);
        NinjaDatasources ninjaDatasources = new NinjaDatasources(List.of(ninjaDatasource));

        return new NinjaJdbiImpl(ninjaDatasources);
    }

    /**
     * Create a Jdbi instance from a test database.
     *
     * @param testDatabase The test database
     * @return A Jdbi instance
     */
    public static Jdbi createJdbi(H2TestDatabase testDatabase) {
        Objects.requireNonNull(testDatabase, "testDatabase");
        return createJdbi(testDatabase.getDataSource());
    }

    /**
     * Create a Jdbi instance from a TestContainers database.
     *
     * @param testDatabase The test database
     * @return A Jdbi instance
     */
    public static Jdbi createJdbi(TestContainersDatabase testDatabase) {
        Objects.requireNonNull(testDatabase, "testDatabase");
        return createJdbi(testDatabase.getDataSource());
    }

    /**
     * Create a Jdbi instance from a data source.
     *
     * @param dataSource The data source
     * @return A Jdbi instance with SqlObjectPlugin installed
     */
    public static Jdbi createJdbi(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");

        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }
}
