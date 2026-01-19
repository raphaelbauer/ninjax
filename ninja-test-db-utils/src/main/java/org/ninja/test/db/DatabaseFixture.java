package org.ninja.test.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility for setting up test data and executing SQL.
 *
 * Example usage:
 * <pre>
 * DatabaseFixture fixture = new DatabaseFixture(dataSource);
 * fixture.executeSql("IN0SERT INTO tasks (title, completed) VALUES (?, ?)", "Test", false);
 * fixture.clearTable("tasks");
 * fixture.insertRow("tasks", Map.of("title", "Test", "completed", false));
 * </pre>
 */
public class DatabaseFixture {

    private final DataSource dataSource;

    /**
     * Create a new database fixture.
     *
     * @param dataSource The data source
     */
    public DatabaseFixture(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    /**
     * Execute a SQL statement with parameters.
     *
     * @param sql The SQL statement
     * @param parameters The parameters
     * @throws RuntimeException If execution fails
     */
    public void executeSql(String sql, Object... parameters) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    /**
     * Execute a SQL statement without parameters.
     *
     * @param sql The SQL statement
     * @throws RuntimeException If execution fails
     */
    public void executeSqlUpdate(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    /**
     * Clear all rows from a table.
     *
     * @param tableName The table name
     * @throws RuntimeException If clearing fails
     */
    public void clearTable(String tableName) {
        executeSqlUpdate("DELETE FROM " + tableName);
    }

    /**
     * Insert a row into a table.
     *
     * @param tableName The table name
     * @param columnValues Map of column names to values
     * @throws RuntimeException If insertion fails
     */
    public void insertRow(String tableName, Map<String, Object> columnValues) {
        if (columnValues.isEmpty()) {
            throw new IllegalArgumentException("columnValues cannot be empty");
        }

        String columns = String.join(", ", columnValues.keySet());
        String placeholders = columnValues.keySet().stream()
                .map(k -> "?")
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";

        executeSql(sql, columnValues.values().toArray());
    }

    /**
     * Update a row in a table.
     *
     * @param tableName The table name
     * @param columnValues Map of column names to new values
     * @param whereColumn The column to use in WHERE clause
     * @param whereValue The value to match in WHERE clause
     * @throws RuntimeException If update fails
     */
    public void updateRow(String tableName, Map<String, Object> columnValues, String whereColumn, Object whereValue) {
        if (columnValues.isEmpty()) {
            throw new IllegalArgumentException("columnValues cannot be empty");
        }

        String setClause = columnValues.keySet().stream()
                .map(col -> col + " = ?")
                .collect(Collectors.joining(", "));

        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + whereColumn + " = ?";

        Object[] parameters = new Object[columnValues.size() + 1];
        int i = 0;
        for (Object value : columnValues.values()) {
            parameters[i++] = value;
        }
        parameters[i] = whereValue;

        executeSql(sql, parameters);
    }

    /**
     * Delete a row from a table.
     *
     * @param tableName The table name
     * @param whereColumn The column to use in WHERE clause
     * @param whereValue The value to match in WHERE clause
     * @throws RuntimeException If deletion fails
     */
    public void deleteRow(String tableName, String whereColumn, Object whereValue) {
        String sql = "DELETE FROM " + tableName + " WHERE " + whereColumn + " = ?";
        executeSql(sql, whereValue);
    }

    /**
     * Execute multiple SQL statements in a transaction.
     *
     * @param sqlStatements The SQL statements to execute
     * @throws RuntimeException If execution fails
     */
    public void executeInTransaction(String... sqlStatements) {
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);

                try (Statement stmt = conn.createStatement()) {
                    for (String sql : sqlStatements) {
                        stmt.execute(sql);
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute statements in transaction", e);
        }
    }
}
