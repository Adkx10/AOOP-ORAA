package util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDBUtils {

    public static void resetAutoIncrement(Connection conn, String tableName, String primaryKeyColumn) throws SQLException {
        int nextId = 1;
        

        String maxIdSql = "SELECT IFNULL(MAX(" + primaryKeyColumn + "), 0) + 1 AS nextId FROM " + tableName;
        try (Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(maxIdSql)) {
            if (rs.next()) {
                nextId = rs.getInt("nextId");
            }
        }

        String alterSql = "ALTER TABLE " + tableName + " AUTO_INCREMENT = " + nextId;
        try (Statement stmt = conn.createStatement()) { 
            stmt.executeUpdate(alterSql);
            System.out.println("Reset AUTO_INCREMENT for " + tableName + " to " + nextId);
        }
    }
}