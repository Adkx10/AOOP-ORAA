package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;  
import java.util.logging.Level;
import java.util.logging.Logger;


public class SalaryDAO {

    private static final Logger LOGGER = Logger.getLogger(SalaryDAO.class.getName());


    public SalaryInfo getLatestSalaryByEmployeeId(String employeeId) throws SQLException {
         
        String sql = "SELECT BasicSalary, HourlyRate, EffectiveDate FROM salary " +
                     "WHERE EmployeeID = ? AND IsDeleted = 0 " +
                     "ORDER BY EffectiveDate DESC, LastModifiedDate DESC LIMIT 1";

        SalaryInfo salaryInfo = null;
        try (Connection conn = DBConnection.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    salaryInfo = new SalaryInfo(
                        rs.getDouble("BasicSalary"),
                        rs.getDouble("HourlyRate"),
                        rs.getDate("EffectiveDate") 
                    );
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting latest salary for employee ID: " + employeeId, e);
            throw e;
        }
        return salaryInfo;
    }

    public boolean addSalary(Connection conn, String employeeId, double basicSalary) throws SQLException {  
        String sql = "INSERT INTO salary (EmployeeID, BasicSalary) VALUES (?, ?)";
         
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {  
            pstmt.setString(1, employeeId);
            pstmt.setDouble(2, basicSalary);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding salary for employee " + employeeId, e);
            throw e;
        }
    }


    public boolean softDeleteCurrentSalary(Connection conn, String employeeId) throws SQLException {  
         
        String sql = "UPDATE salary SET IsDeleted = 1 " +
                     "WHERE EmployeeID = ? AND IsDeleted = 0 " +
                     "ORDER BY EffectiveDate DESC, LastModifiedDate DESC LIMIT 1";  

         
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {  
            pstmt.setString(1, employeeId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error soft-deleting current salary for employee " + employeeId, e);
            throw e;
        }
    }

    // POJO
    public static class SalaryInfo {
        public double basicSalary;
        public double hourlyRate;
        public Date effectiveDate;  

        public SalaryInfo(double basicSalary, double hourlyRate, Date effectiveDate) {
            this.basicSalary = basicSalary;
            this.hourlyRate = hourlyRate;
            this.effectiveDate = effectiveDate;
        }
    }
}