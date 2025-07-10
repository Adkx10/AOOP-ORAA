package Testing;

import dao.AllowanceDAO;
import data.DBConnection;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestAllowance {

    private static AllowanceDAO allowanceDAO;
    private static Connection conn;

    private static int testEmployeeId = -1;
    private static int testUserId = -1;
    private static int testAddressId = -1;

    @BeforeAll
    public static void setUpAll() throws SQLException {
        allowanceDAO = new AllowanceDAO();
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            testUserId = insertTestUser("allowance-test-user");
            testAddressId = insertTestAddress();
            testEmployeeId = insertTestEmployee(testUserId, testAddressId);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    @AfterEach
    public void tearDownEach() throws SQLException {
        if (conn != null) {
            conn.rollback();
        }
    }

    @AfterAll
    public static void tearDownAll() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            deleteTestEmployee(testEmployeeId, testUserId, testAddressId);
            conn.commit();

            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
            TestDBUtils.resetAutoIncrement(conn, "allowancetype", "AllowanceTypeID");
            TestDBUtils.resetAutoIncrement(conn, "allowance", "AllowanceID");
            conn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Get Current Allowances - Success")
    public void testGetCurrentAllowancesByEmployeeId_Success() throws SQLException {
        // Setup: Insert some allowances for the test employee
        insertAllowance(testEmployeeId, "Rice Subsidy", 1500.0);
        insertAllowance(testEmployeeId, "Phone Allowance", 500.0);
        // FIX: Do NOT commit here. Let @AfterEach handle the rollback.

        // Act: Pass the shared connection to the DAO method
        AllowanceDAO.AllowanceInfo allowanceInfo = allowanceDAO.getCurrentAllowancesByEmployeeId(conn, String.valueOf(testEmployeeId));

        // Assert
        assertNotNull(allowanceInfo, "AllowanceInfo should not be null for an employee with allowances.");
        assertEquals(1500.0, allowanceInfo.riceSubsidy, 0.01);
        assertEquals(500.0, allowanceInfo.phoneAllowance, 0.01);
    }

    @Test
    @Order(2)
    @DisplayName("Test Get Current Allowances - No Allowances")
    public void testGetCurrentAllowancesByEmployeeId_NoAllowances() throws SQLException {
        // No setup needed; the previous test's data was rolled back.

        // Act: Pass the shared connection
        AllowanceDAO.AllowanceInfo allowanceInfo = allowanceDAO.getCurrentAllowancesByEmployeeId(conn, String.valueOf(testEmployeeId));

        // Assert
        assertNull(allowanceInfo, "AllowanceInfo should be null for an employee with no active allowances.");
    }

    @Test
    @Order(3)
    @DisplayName("Test Get Current Allowances - Non-Existent Employee")
    public void testGetCurrentAllowancesByEmployeeId_NonExistentEmployee() throws SQLException {
        // Act: Pass the shared connection
        AllowanceDAO.AllowanceInfo allowanceInfo = allowanceDAO.getCurrentAllowancesByEmployeeId(conn, "99999");

        // Assert
        assertNull(allowanceInfo, "AllowanceInfo should be null for a non-existent employee ID.");
    }

    // --- HELPER METHODS ---

    private static int insertTestUser(String username) throws SQLException {
        String sql = "INSERT INTO user (Username, Password) VALUES (?, 'testpass')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                else throw new SQLException("Creating user failed, no ID obtained.");
            }
        }
    }

    private static int insertTestAddress() throws SQLException {
        String sql = "INSERT INTO address (UnitOrHouseStreet) VALUES ('123 Test Lane')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                else throw new SQLException("Creating address failed, no ID obtained.");
            }
        }
    }

    private static int insertTestEmployee(int userId, int addressId) throws SQLException {
        String sql = "INSERT INTO employee (UserID, AddressID, LastName, FirstName) VALUES (?, ?, 'Test', 'AllowanceUser')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, addressId);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                else throw new SQLException("Creating employee failed, no ID obtained.");
            }
        }
    }
    
    private static void insertAllowance(int employeeId, String allowanceName, double amount) throws SQLException {
        int allowanceTypeId = -1;
        String checkTypeSql = "SELECT AllowanceTypeID FROM allowancetype WHERE AllowanceName = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkTypeSql)) {
            pstmt.setString(1, allowanceName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                allowanceTypeId = rs.getInt("AllowanceTypeID");
            } else {
                String insertTypeSql = "INSERT INTO allowancetype (AllowanceName) VALUES (?)";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertTypeSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertPstmt.setString(1, allowanceName);
                    insertPstmt.executeUpdate();
                    ResultSet generatedKeys = insertPstmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        allowanceTypeId = generatedKeys.getInt(1);
                    }
                }
            }
        }

        if (allowanceTypeId != -1) {
            String sql = "INSERT INTO allowance (EmployeeID, AllowanceTypeID, Amount, EffectiveDate) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, employeeId);
                pstmt.setInt(2, allowanceTypeId);
                pstmt.setDouble(3, amount);
                pstmt.setDate(4, Date.valueOf(LocalDate.now()));
                pstmt.executeUpdate();
            }
        }
    }

    private static void deleteTestEmployee(int employeeId, int userId, int addressId) throws SQLException {
        if (employeeId == -1 && userId == -1) return;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
        }
        try {
            if (employeeId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM allowance WHERE EmployeeID = ?")) { pstmt.setInt(1, employeeId); pstmt.executeUpdate(); }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM employee WHERE EmployeeID = ?")) { pstmt.setInt(1, employeeId); pstmt.executeUpdate(); }
            }
            if (userId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE UserID = ?")) { pstmt.setInt(1, userId); pstmt.executeUpdate(); }
            }
            if (addressId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM address WHERE AddressID = ?")) { pstmt.setInt(1, addressId); pstmt.executeUpdate(); }
            }
        } finally {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
            }
        }
    }
}
