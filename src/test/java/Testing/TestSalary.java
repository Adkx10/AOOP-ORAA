package Testing;

import dao.SalaryDAO;
import data.DBConnection;
import org.junit.jupiter.api.*;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSalary {

    private static SalaryDAO salaryDAO;
    private static Connection conn;
    
    // This will hold the ID of a test employee created for the tests
    private static int testEmployeeId = -1;
    private static int testUserId = -1;
    private static int testAddressId = -1;

    @BeforeAll
    public static void setUpAll() throws SQLException {
        salaryDAO = new SalaryDAO();
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false); // Manually manage transactions
        
        // --- SETUP A SINGLE EMPLOYEE FOR ALL TESTS IN THIS CLASS ---
        try {
            // Use private helper methods to create test data
            testUserId = insertTestUser("salary-test-user");
            testAddressId = insertTestAddress();
            testEmployeeId = insertTestEmployee(testUserId, testAddressId);
            conn.commit();
            System.out.println("Created a temporary employee with ID: " + testEmployeeId + " for all salary tests.");
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Failed to create the test employee for SalaryDAO tests.");
            e.printStackTrace();
            throw e;
        }
    }

    @AfterEach
    public void tearDownEach() throws SQLException {
        // Rollback any uncommitted changes after each test
        if (conn != null) {
            conn.rollback();
        }
    }

    @AfterAll
    public static void tearDownAll() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            // --- CLEANUP THE TEST EMPLOYEE ---
            // Use private helper method to delete test data
            deleteTestEmployee(testEmployeeId, testUserId, testAddressId);
            conn.commit();
            
            // Reset auto-increment values for a clean slate
            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
            TestDBUtils.resetAutoIncrement(conn, "salary", "SalaryID");
            conn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Get Latest Salary - Success")
    public void testGetLatestSalaryByEmployeeId_Success() throws SQLException {
        System.out.println("Testing successful retrieval of an existing salary...");
        SalaryDAO.SalaryInfo salaryInfo = salaryDAO.getLatestSalaryByEmployeeId(String.valueOf(testEmployeeId));
        
        assertNotNull(salaryInfo, "SalaryInfo should not be null for the test employee.");
        assertEquals(35000.0, salaryInfo.basicSalary, 0.01, "Basic salary should match the initial value.");
    }

    @Test
    @Order(2)
    @DisplayName("Test Get Latest Salary - Fail for Non-Existent Employee")
    public void testGetLatestSalaryByEmployeeId_Fail() throws SQLException {
        System.out.println("Testing retrieval of salary for a non-existent employee...");
        SalaryDAO.SalaryInfo salaryInfo = salaryDAO.getLatestSalaryByEmployeeId("99999");
        
        assertNull(salaryInfo, "SalaryInfo should be null for a non-existent employee ID.");
    }

    @Test
    @Order(3)
    @DisplayName("Test Add New Salary")
    public void testAddSalary_Success() throws SQLException {
        System.out.println("Testing the addition of a new salary record...");
        double newSalary = 40000.0;
        
        // Act: A proper salary update involves deactivating the old record first.
        // This also prevents test state from leaking into subsequent tests.
        salaryDAO.softDeleteCurrentSalary(conn, String.valueOf(testEmployeeId));
        
        // Add the new salary record
        boolean added = salaryDAO.addSalary(conn, String.valueOf(testEmployeeId), newSalary);
        assertTrue(added, "addSalary should return true on success.");
        conn.commit();

        // Assert: Verify the new salary is now the latest
        SalaryDAO.SalaryInfo latestSalary = salaryDAO.getLatestSalaryByEmployeeId(String.valueOf(testEmployeeId));
        assertNotNull(latestSalary, "The newly added salary should be found.");
        assertEquals(newSalary, latestSalary.basicSalary, 0.01, "The new salary should be the latest record.");
    }

    @Test
    @Order(4)
    @DisplayName("Test Soft Delete Current Salary")
    public void testSoftDeleteCurrentSalary() throws SQLException {
        System.out.println("Testing the soft deletion of a salary record...");
        
        // First, ensure there is an active salary
        SalaryDAO.SalaryInfo initialSalary = salaryDAO.getLatestSalaryByEmployeeId(String.valueOf(testEmployeeId));
        assertNotNull(initialSalary, "An initial active salary must exist to test deletion.");

        // Act: Soft delete the current salary
        boolean deleted = salaryDAO.softDeleteCurrentSalary(conn, String.valueOf(testEmployeeId));
        assertTrue(deleted, "softDeleteCurrentSalary should return true when an active record is found.");
        conn.commit();

        // Assert: Verify that there are no more active salaries for this employee
        SalaryDAO.SalaryInfo salaryAfterDelete = salaryDAO.getLatestSalaryByEmployeeId(String.valueOf(testEmployeeId));
        assertNull(salaryAfterDelete, "There should be no active salary after a soft delete.");
        
        // Optional: Directly check the IsDeleted flag in the database
        String sql = "SELECT IsDeleted FROM salary WHERE EmployeeID = ? ORDER BY EffectiveDate DESC LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testEmployeeId);
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "A salary record should still exist.");
            assertTrue(rs.getBoolean("IsDeleted"), "The IsDeleted flag should be set to true.");
        }
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
        int employeeId = -1;
        String employeeSql = "INSERT INTO employee (UserID, AddressID, LastName, FirstName) VALUES (?, ?, 'Test', 'SalaryUser')";
        try (PreparedStatement pstmt = conn.prepareStatement(employeeSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, addressId);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) employeeId = rs.getInt(1);
                else throw new SQLException("Creating employee failed, no ID obtained.");
            }
        }

        String salarySql = "INSERT INTO salary (EmployeeID, BasicSalary) VALUES (?, 35000.0)";
        try (PreparedStatement pstmt = conn.prepareStatement(salarySql)) {
            pstmt.setInt(1, employeeId);
            pstmt.executeUpdate();
        }
        return employeeId;
    }

    private static void deleteTestEmployee(int employeeId, int userId, int addressId) throws SQLException {
        if (employeeId == -1 && userId == -1) return;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
        }
        try {
            if (employeeId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM salary WHERE EmployeeID = ?")) { pstmt.setInt(1, employeeId); pstmt.executeUpdate(); }
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
