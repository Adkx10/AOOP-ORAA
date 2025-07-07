package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date; // For java.util.Date
import java.util.logging.Level;
import java.util.logging.Logger;

// This DAO handles operations for the 'salary' table.
public class SalaryDAO {

    private static final Logger LOGGER = Logger.getLogger(SalaryDAO.class.getName());

    /**
     * Retrieves the current salary details for a given employee.
     * It fetches the most recent active salary record based on EffectiveDate.
     * This method gets its own connection as it's typically a read-only operation.
     *
     * @param employeeId The ID of the employee.
     * @return A SalaryInfo object containing basic salary, hourly rate, and effective date, or null if no record is found.
     * @throws SQLException if a database access error occurs.
     */
    public SalaryInfo getLatestSalaryByEmployeeId(String employeeId) throws SQLException {
        // Query to get the latest (most recent EffectiveDate) non-deleted salary record
        String sql = "SELECT BasicSalary, HourlyRate, EffectiveDate FROM salary " +
                     "WHERE EmployeeID = ? AND IsDeleted = 0 " +
                     "ORDER BY EffectiveDate DESC, LastModifiedDate DESC LIMIT 1";

        SalaryInfo salaryInfo = null;
        try (Connection conn = DBConnection.getConnection(); // This method gets its own connection
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    salaryInfo = new SalaryInfo(
                        rs.getDouble("BasicSalary"),
                        rs.getDouble("HourlyRate"),
                        rs.getDate("EffectiveDate") // Retrieve EffectiveDate as java.util.Date
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting latest salary for employee ID: " + employeeId, e);
            throw e;
        }
        return salaryInfo;
    }

    /**
     * Inserts a new salary record for an employee using a provided connection.
     * This method is designed to be part of a larger transaction.
     *
     * @param conn The database connection to use for the transaction.
     * @param employeeId The ID of the employee.
     * @param basicSalary The basic salary.
     * @return true if insertion was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean addSalary(Connection conn, String employeeId, double basicSalary) throws SQLException { // <-- Modified signature
        String sql = "INSERT INTO salary (EmployeeID, BasicSalary) VALUES (?, ?)";
        // Use the provided connection, do NOT close it here
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // <-- Use provided conn
            pstmt.setString(1, employeeId);
            pstmt.setDouble(2, basicSalary);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding salary for employee " + employeeId, e);
            throw e;
        }
    }

    /**
     * Marks the current active (IsDeleted = 0) salary record for an employee as deleted (IsDeleted = 1)
     * using a provided connection. This is a soft delete and is designed to be part of a larger transaction.
     * This is used when a new salary record is added, effectively deactivating the old one.
     *
     * @param conn The database connection to use for the transaction.
     * @param employeeId The ID of the employee whose salary record needs to be soft-deleted.
     * @return true if a record was updated, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean softDeleteCurrentSalary(Connection conn, String employeeId) throws SQLException { // <-- Modified signature
        // Find the most recent active salary record and mark it as deleted.
        String sql = "UPDATE salary SET IsDeleted = 1 " +
                     "WHERE EmployeeID = ? AND IsDeleted = 0 " +
                     "ORDER BY EffectiveDate DESC, LastModifiedDate DESC LIMIT 1"; // Ensure only the latest is soft-deleted

        // Use the provided connection, do NOT close it here
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { // <-- Use provided conn
            pstmt.setString(1, employeeId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error soft-deleting current salary for employee " + employeeId, e);
            throw e;
        }
    }

    /**
     * A simple inner class (POJO) to hold basic salary, hourly rate, and effective date.
     */
    public static class SalaryInfo {
        public double basicSalary;
        public double hourlyRate;
        public Date effectiveDate; // Added effectiveDate as java.util.Date

        public SalaryInfo(double basicSalary, double hourlyRate, Date effectiveDate) {
            this.basicSalary = basicSalary;
            this.hourlyRate = hourlyRate;
            this.effectiveDate = effectiveDate;
        }
    }
}