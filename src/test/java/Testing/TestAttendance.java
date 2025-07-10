package Testing;

import dao.AttendanceDAO;
import data.DBConnection;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestAttendance {

    private static AttendanceDAO attendanceDAO;
    private static Connection conn;

    private static int testEmployeeId = -1;
    private static int testUserId = -1;
    private static int testAddressId = -1;

    @BeforeAll
    public static void setUpAll() throws SQLException {
        attendanceDAO = new AttendanceDAO();
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            testUserId = insertTestUser("attendance-test-user");
            testAddressId = insertTestAddress();
            testEmployeeId = insertTestEmployee(testUserId, testAddressId);
            conn.commit();
            System.out.println("Created a temporary employee with ID: " + testEmployeeId + " for all attendance tests.");
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Failed to create the test employee for AttendanceDAO tests.");
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
            TestDBUtils.resetAutoIncrement(conn, "attendance", "AttendanceID");
            conn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Get Attendance Records - Success")
    public void testGetAttendanceRecords_Success() throws SQLException {
        System.out.println("Testing retrieval of attendance records for a specific month...");
        // Setup: Insert two records in the same month
        insertAttendance(testEmployeeId, LocalDate.of(2025, 8, 5), "08:00:00", "17:00:00");
        insertAttendance(testEmployeeId, LocalDate.of(2025, 8, 6), "08:05:00", "17:02:00");
        // Insert a record in a different month that should NOT be fetched
        insertAttendance(testEmployeeId, LocalDate.of(2025, 9, 1), "08:00:00", "17:00:00");
        conn.commit();

        // Act
        List<AttendanceDAO.AttendanceRecord> records = attendanceDAO.getAttendanceRecordsByEmployeeAndMonth(String.valueOf(testEmployeeId), "August", 2025);

        // Assert
        assertNotNull(records, "The list of attendance records should not be null.");
        assertEquals(2, records.size(), "Should retrieve exactly two records for August.");
    }

    @Test
    @Order(2)
    @DisplayName("Test Get Attendance Records - Empty for No Records")
    public void testGetAttendanceRecords_EmptyForNoRecords() throws SQLException {
        System.out.println("Testing retrieval for a month with no attendance records...");
        // No records are inserted for this month

        // Act
        List<AttendanceDAO.AttendanceRecord> records = attendanceDAO.getAttendanceRecordsByEmployeeAndMonth(String.valueOf(testEmployeeId), "July", 2025);

        // Assert
        assertNotNull(records, "The list should not be null, even if empty.");
        assertTrue(records.isEmpty(), "The list of records should be empty for a month with no attendance.");
    }

    @Test
    @Order(3)
    @DisplayName("Test Get Attendance Records - Fail for Invalid Month")
    public void testGetAttendanceRecords_FailForInvalidMonth() {
        System.out.println("Testing retrieval with an invalid month name...");
        // Assert that an SQLException is thrown for an invalid month
        assertThrows(SQLException.class, () -> {
            attendanceDAO.getAttendanceRecordsByEmployeeAndMonth(String.valueOf(testEmployeeId), "InvalidMonth", 2025);
        }, "Should throw an SQLException for an invalid month name.");
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
        String sql = "INSERT INTO employee (UserID, AddressID, LastName, FirstName) VALUES (?, ?, 'Test', 'AttendanceUser')";
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

    private static void insertAttendance(int employeeId, LocalDate date, String logIn, String logOut) throws SQLException {
        String sql = "INSERT INTO attendance (EmployeeID, Date, LogInTime, LogOutTime) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setString(3, logIn);
            pstmt.setString(4, logOut);
            pstmt.executeUpdate();
        }
    }

    private static void deleteTestEmployee(int employeeId, int userId, int addressId) throws SQLException {
        if (employeeId == -1 && userId == -1) return;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
        }
        try {
            if (employeeId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM attendance WHERE EmployeeID = ?")) { pstmt.setInt(1, employeeId); pstmt.executeUpdate(); }
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
