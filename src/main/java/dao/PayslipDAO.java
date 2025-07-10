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

    private static final Logger LOGGER = Logger.getLogger(PayslipDAO.class.getName());

    public Payslip getPayslipByEmployeeAndMonth(String employeeId, String monthName, int year) throws SQLException {

        String monthNumber = getMonthNumber(monthName); 
        if (monthNumber == null) { 
            LOGGER.log(Level.WARNING, "Invalid month name provided to PayslipDAO: " + monthName); 
            throw new SQLException("Invalid month name provided: " + monthName); 
        }


        String sql = "SELECT `Payslip No`, `Employee ID`, `Employee Name`, `Period Start Date`, " + 
                "`Period End Date`, `Employee Position`, `Monthly Rate`, `Daily Rate`, " + 
                "`Days Worked`, `Overtime`, `Gross Income`, `Rice Subsidy`, `Phone Allowance`, " + 
                "`Clothing Allowance`, `Total Benefits`, `Social Security System`, `Philhealth`, " + 
                "`Pag-Ibig`, `Withholding Tax`, `Total Deductions`, `Take Home Pay` " + 
                "FROM payslip_generation " + 
                "WHERE `Employee ID` = ? AND YEAR(`Period Start Date`) = ? AND MONTH(`Period Start Date`) = ?"; 

        Payslip payslip = null; 

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setString(1, employeeId); 
            pstmt.setInt(2, year); 
            pstmt.setInt(3, Integer.parseInt(monthNumber));

            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    payslip = new Payslip( 
                            rs.getString("Payslip No"), 
                            rs.getString("Employee ID"), 
                            rs.getString("Employee Name"), 
                            rs.getString("Period Start Date"), 
                            rs.getString("Period End Date"), 
                            rs.getString("Employee Position"), 
                            rs.getDouble("Monthly Rate"), 
                            rs.getDouble("Daily Rate"), 
                            rs.getInt("Days Worked"), 
                            rs.getDouble("Overtime"), 
                            rs.getDouble("Gross Income"), 
                            rs.getDouble("Rice Subsidy"), 
                            rs.getDouble("Phone Allowance"), 
                            rs.getDouble("Clothing Allowance"), 
                            rs.getDouble("Total Benefits"), 
                            rs.getDouble("Social Security System"), 
                            rs.getDouble("Philhealth"), 
                            rs.getDouble("Pag-Ibig"), 
                            rs.getDouble("Withholding Tax"), 
                            rs.getDouble("Total Deductions"), 
                            rs.getDouble("Take Home Pay") 
                    );
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting payslip for employee " + employeeId + " for " + monthName + ", " + year, e); 
            throw e; 
        }
        return payslip; 
    }

    private String getMonthNumber(String monthName) { 
        return switch (monthName.toLowerCase()) { 
            case "january" -> "01"; 
            case "february" -> "02"; 
            case "march" -> "03"; 
            case "april" -> "04"; 
            case "may" -> "05"; 
            case "june" -> "06"; 
            case "july" -> "07"; 
            case "august" -> "08"; 
            case "september" -> "09"; 
            case "october" -> "10"; 
            case "november" -> "11"; 
            case "december" -> "12"; 
            default -> null; 
        };
    }
}