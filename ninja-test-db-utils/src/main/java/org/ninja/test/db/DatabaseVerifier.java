package org.ninja.test.db;

import static com.google.common.truth.Truth.assertThat;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Utility for verifying database state in tests.
 *
 * Example usage:
 * <pre>
 * DatabaseVerifier verifier = new DatabaseVerifier(dataSource);
 * verifier.assertTableCount("tasks", 3);
 * verifier.assertColumnValue("tasks", "id", 1L, "completed", true);
 * verifier.assertRecordExists("tasks", "title", "Test Task");
 * </pre>
 */
public class DatabaseVerifier {

    private final DataSource dataSource;

    /**
     * Create a new database verifier.
     *
     * @param dataSource The data source
     */
    public DatabaseVerifier(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    /**
     * Assert that a table has the expected number of rows.
     *
     * @param tableName The table name
     * @param expectedCount The expected row count
     */
    public void assertTableCount(String tableName, int expectedCount) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int actualCount = rs.getInt(1);
                assertThat(actualCount).isEqualTo(expectedCount);
            } else {
                throw new AssertionError("Failed to get count from table: " + tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count rows in table: " + tableName, e);
        }
    }

    /**
     * Assert that a table is empty.
     *
     * @param tableName The table name
     */
    public void assertTableEmpty(String tableName) {
        assertTableCount(tableName, 0);
    }

    /**
     * Assert that a table is not empty.
     *
     * @param tableName The table name
     */
    public void assertTableNotEmpty(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int count = rs.getInt(1);
                assertThat(count).isGreaterThan(0);
            } else {
                throw new AssertionError("Failed to get count from table: " + tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count rows in table: " + tableName, e);
        }
    }

    /**
     * Assert that a specific column has the expected value for a given row.
     *
     * @param tableName The table name
     * @param idColumn The ID column name
     * @param idValue The ID value
     * @param column The column to check
     * @param expectedValue The expected value
     */
    public void assertColumnValue(String tableName, String idColumn, Object idValue, String column, Object expectedValue) {
        String sql = "SELECT " + column + " FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idValue);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object actualValue = rs.getObject(1);
                    assertThat(actualValue).isEqualTo(expectedValue);
                } else {
                    throw new AssertionError("No row found in " + tableName + " where " + idColumn + " = " + idValue);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify column value", e);
        }
    }

    /**
     * Assert that a record exists with the given column value.
     *
     * @param tableName The table name
     * @param column The column name
     * @param value The value to search for
     */
    public void assertRecordExists(String tableName, String column, Object value) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + column + " = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, value);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    assertThat(count).isGreaterThan(0);
                } else {
                    throw new AssertionError("Failed to check for record existence");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify record existence", e);
        }
    }

    /**
     * Assert that no record exists with the given column value.
     *
     * @param tableName The table name
     * @param column The column name
     * @param value The value to search for
     */
    public void assertRecordDoesNotExist(String tableName, String column, Object value) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + column + " = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, value);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    assertThat(count).isEqualTo(0);
                } else {
                    throw new AssertionError("Failed to check for record non-existence");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to verify record non-existence", e);
        }
    }

    /**
     * Get a column value from a specific row.
     *
     * @param tableName The table name
     * @param idColumn The ID column name
     * @param idValue The ID value
     * @param column The column to retrieve
     * @param <T> The expected return type
     * @return The column value
     */
    @SuppressWarnings("unchecked")
    public <T> T getColumnValue(String tableName, String idColumn, Object idValue, String column) {
        String sql = "SELECT " + column + " FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idValue);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return (T) rs.getObject(1);
                } else {
                    throw new AssertionError("No row found in " + tableName + " where " + idColumn + " = " + idValue);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get column value", e);
        }
    }

    /**
     * Get the count of rows in a table.
     *
     * @param tableName The table name
     * @return The row count
     */
    public int getTableCount(String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new AssertionError("Failed to get count from table: " + tableName);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count rows in table: " + tableName, e);
        }
    }
}
