package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllowanceDAO {

    private static final Logger LOGGER = Logger.getLogger(AllowanceDAO.class.getName());

    public AllowanceInfo getCurrentAllowancesByEmployeeId(Connection conn, String employeeId) throws SQLException {
        String sql = "SELECT at.AllowanceName, SUM(a.Amount) AS TotalAmount " +
                     "FROM allowance a " +
                     "JOIN allowancetype at ON a.AllowanceTypeID = at.AllowanceTypeID " +
                     "WHERE a.EmployeeID = ? AND a.IsDeleted = 0 " +
                     "GROUP BY at.AllowanceName";

        double riceSubsidy = 0.0;
        double phoneAllowance = 0.0;
        double clothingAllowance = 0.0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String allowanceType = rs.getString("AllowanceName");
                    double totalAmount = rs.getDouble("TotalAmount");

                    if (allowanceType != null) {
                        switch (allowanceType.toLowerCase()) {
                            case "rice subsidy":
                                riceSubsidy = totalAmount;
                                break;
                            case "phone allowance":
                                phoneAllowance = totalAmount;
                                break;
                            case "clothing allowance":
                                clothingAllowance = totalAmount;
                                break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting current allowances for employee ID: " + employeeId, e);
            throw e;
        }

        if (riceSubsidy == 0.0 && phoneAllowance == 0.0 && clothingAllowance == 0.0) {
            return null;
        }
        return new AllowanceInfo(riceSubsidy, phoneAllowance, clothingAllowance);
    }

    //POJO to hold allowance amounts.
    public static class AllowanceInfo {
        public double riceSubsidy;
        public double phoneAllowance;
        public double clothingAllowance;

        public AllowanceInfo(double riceSubsidy, double phoneAllowance, double clothingAllowance) {
            this.riceSubsidy = riceSubsidy;
            this.phoneAllowance = phoneAllowance;
            this.clothingAllowance = clothingAllowance;
        }
    }
}
