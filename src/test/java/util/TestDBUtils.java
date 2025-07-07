package util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDBUtils {

    /**
     * Resets the AUTO_INCREMENT of a given table based on MAX(primary key) + 1.
     * If the table is empty, sets AUTO_INCREMENT to 1.
     * This method uses the provided Connection, and its DDL operations (ALTER TABLE)
     * typically auto-commit any pending transaction on that connection.
     *
     * @param conn             The database connection to use (should be the same connection used by the test suite if possible)
     * @param tableName        The name of the table to reset
     * @param primaryKeyColumn The primary key column (typically the auto-increment column)
     * @throws SQLException if any SQL error occurs
     */
    public static void resetAutoIncrement(Connection conn, String tableName, String primaryKeyColumn) throws SQLException {
        int nextId = 1;
        
        // Ensure auto-commit is temporarily enabled for ALTER TABLE, or
        // rely on its auto-commit behavior if it's known to be auto-committing.
        // If the passed 'conn' has auto-commit off, this operation might
        // cause a commit, or it might just fail if a transaction is already active.
        // For ALTER TABLE, it generally forces a commit.

        // Get the next ID
        String maxIdSql = "SELECT IFNULL(MAX(" + primaryKeyColumn + "), 0) + 1 AS nextId FROM " + tableName;
        try (Statement stmt = conn.createStatement(); // Use the provided connection
             ResultSet rs = stmt.executeQuery(maxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("nextId");
            }
        }

        // Alter the table to reset AUTO_INCREMENT
        String alterSql = "ALTER TABLE " + tableName + " AUTO_INCREMENT = " + nextId;
        try (Statement stmt = conn.createStatement()) { // Use the provided connection
            stmt.executeUpdate(alterSql);
            System.out.println("Reset AUTO_INCREMENT for " + tableName + " to " + nextId);
        }
    }
}