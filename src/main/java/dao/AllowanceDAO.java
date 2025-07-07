package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

// This DAO handles operations for the 'allowance' and 'allowancetype' tables.
public class AllowanceDAO { // Assuming this is dao.AllowanceDAO

    private static final Logger LOGGER = Logger.getLogger(AllowanceDAO.class.getName()); //

    /**
     * Retrieves all active allowance amounts for a given employee.
     * It groups by AllowanceType to get the sum for each type (e.g., total Rice Subsidy).
     *
     * @param employeeId The ID of the employee.
     * @return An AllowanceInfo object containing the sum of amounts for each type of allowance,
     * or null if no allowances are found.
     * @throws SQLException if a database access error occurs.
     */
    public AllowanceInfo getCurrentAllowancesByEmployeeId(String employeeId) throws SQLException {
        // SQL to get the current allowance amounts for each type for an employee.
        // We'll join with 'allowancetype' to get the type name.
        // Assuming the latest effective allowance for each type is desired.
        // This query sums all allowances for an employee, but you might need to
        // refine it if only the *latest effective* allowance of each type is desired,
        // which would involve more complex subqueries or window functions.
        // For simplicity, I will sum allowances by type.
        
        String sql = "SELECT at.AllowanceType, SUM(a.Amount) AS TotalAmount " + //
                     "FROM allowance a " + //
                     "JOIN allowancetype at ON a.AllowanceTypeID = at.AllowanceTypeID " + //
                     "WHERE a.EmployeeID = ? AND a.IsDeleted = 0 " + //
                     "GROUP BY at.AllowanceType"; // Group by type to sum individual types

        double riceSubsidy = 0.0; //
        double phoneAllowance = 0.0; //
        double clothingAllowance = 0.0; //

        try (Connection conn = DBConnection.getConnection(); // Gets its own connection
             PreparedStatement pstmt = conn.prepareStatement(sql)) { //

            pstmt.setString(1, employeeId); //
            try (ResultSet rs = pstmt.executeQuery()) { //
                while (rs.next()) { //
                    String allowanceType = rs.getString("AllowanceType"); //
                    double totalAmount = rs.getDouble("TotalAmount"); //

                    switch (allowanceType.toLowerCase()) { // Match your AllowanceType names
                        case "rice subsidy": //
                            riceSubsidy = totalAmount; //
                            break; //
                        case "phone allowance": //
                            phoneAllowance = totalAmount; //
                            break; //
                        case "clothing allowance": //
                            clothingAllowance = totalAmount; //
                            break; //
                        // Add more cases if you have other allowance types
                    }
                }
            }
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error getting current allowances for employee ID: " + employeeId, e); //
            throw e; //
        }

        // Return null if all allowances are 0, or create AllowanceInfo object
        if (riceSubsidy == 0.0 && phoneAllowance == 0.0 && clothingAllowance == 0.0) { //
            return null; // No allowances found or all are zero
        }
        return new AllowanceInfo(riceSubsidy, phoneAllowance, clothingAllowance); //
    }

    /**
     * A simple inner class (POJO) to hold allowance amounts.
     */
    public static class AllowanceInfo { //
        public double riceSubsidy; //
        public double phoneAllowance; //
        public double clothingAllowance; //

        public AllowanceInfo(double riceSubsidy, double phoneAllowance, double clothingAllowance) { //
            this.riceSubsidy = riceSubsidy; //
            this.phoneAllowance = phoneAllowance; //
            this.clothingAllowance = clothingAllowance; //
        }
    }
}
