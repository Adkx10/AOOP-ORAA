package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Payslip;

public class PayslipDAO {

    private static final Logger LOGGER = Logger.getLogger(PayslipDAO.class.getName()); //

    /**
     * Retrieves payslip details for a specific employee, month, and year from
     * the payslip_generation view.
     *
     * @param employeeId The Employee ID.
     * @param monthName The month as a String (e.g., "January").
     * @param year The year.
     * @return A Payslip object if found, or null otherwise.
     * @throws SQLException if a database access error occurs or invalid month
     * name is provided.
     */
    public Payslip getPayslipByEmployeeAndMonth(String employeeId, String monthName, int year) throws SQLException {
        // Convert month name to a 2-digit month number for SQL filtering
        String monthNumber = getMonthNumber(monthName); //
        if (monthNumber == null) { //
            LOGGER.log(Level.WARNING, "Invalid month name provided to PayslipDAO: " + monthName); //
            throw new SQLException("Invalid month name provided: " + monthName); //
        }

        // SQL query to select from payslip_generation view
        // Using backticks for column names with spaces
        String sql = "SELECT `Payslip No`, `Employee ID`, `Employee Name`, `Period Start Date`, " + //
                "`Period End Date`, `Employee Position`, `Monthly Rate`, `Daily Rate`, " + //
                "`Days Worked`, `Overtime`, `Gross Income`, `Rice Subsidy`, `Phone Allowance`, " + //
                "`Clothing Allowance`, `Total Benefits`, `Social Security System`, `Philhealth`, " + //
                "`Pag-Ibig`, `Withholding Tax`, `Total Deductions`, `Take Home Pay` " + //
                "FROM payslip_generation " + //
                "WHERE `Employee ID` = ? AND YEAR(`Period Start Date`) = ? AND MONTH(`Period Start Date`) = ?"; //

        Payslip payslip = null; //

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { // Gets its own connection

            pstmt.setString(1, employeeId); //
            pstmt.setInt(2, year); //
            pstmt.setInt(3, Integer.parseInt(monthNumber)); // Pass the numeric month

            try (ResultSet rs = pstmt.executeQuery()) { //
                if (rs.next()) { //
                    payslip = new Payslip( //
                            rs.getString("Payslip No"), //
                            rs.getString("Employee ID"), //
                            rs.getString("Employee Name"), //
                            rs.getString("Period Start Date"), //
                            rs.getString("Period End Date"), //
                            rs.getString("Employee Position"), //
                            rs.getDouble("Monthly Rate"), //
                            rs.getDouble("Daily Rate"), //
                            rs.getInt("Days Worked"), //
                            rs.getDouble("Overtime"), //
                            rs.getDouble("Gross Income"), //
                            rs.getDouble("Rice Subsidy"), //
                            rs.getDouble("Phone Allowance"), //
                            rs.getDouble("Clothing Allowance"), //
                            rs.getDouble("Total Benefits"), //
                            rs.getDouble("Social Security System"), //
                            rs.getDouble("Philhealth"), //
                            rs.getDouble("Pag-Ibig"), //
                            rs.getDouble("Withholding Tax"), //
                            rs.getDouble("Total Deductions"), //
                            rs.getDouble("Take Home Pay") //
                    );
                }
            }
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error getting payslip for employee " + employeeId + " for " + monthName + ", " + year, e); //
            throw e; //
        }
        return payslip; //
    }

    /**
     * Helper method to convert month name to a two-digit number. This method is
     * duplicated from AttendanceDAO but is kept here for PayslipDAO's
     * independence.
     *
     * @param monthName The full month name (e.g., "January").
     * @return The two-digit month number as a String (e.g., "01"), or null if
     * invalid.
     */
    private String getMonthNumber(String monthName) { //
        return switch (monthName.toLowerCase()) { //
            case "january" -> "01"; //
            case "february" -> "02"; //
            case "march" -> "03"; //
            case "april" -> "04"; //
            case "may" -> "05"; //
            case "june" -> "06"; //
            case "july" -> "07"; //
            case "august" -> "08"; //
            case "september" -> "09"; //
            case "october" -> "10"; //
            case "november" -> "11"; //
            case "december" -> "12"; //
            default -> null; // Invalid month name
        };
    }
}