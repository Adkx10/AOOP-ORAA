package Testing;

import dao.LeaveRequestDAO;
import data.DBConnection;
import model.LeaveRequest;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestLeaveRequest {

    private static LeaveRequestDAO leaveRequestDAO;
    private static Connection conn;

    private static int testEmployeeId = -1;
    private static int testUserId = -1;
    private static int testAddressId = -1;
    private static int vacationLeaveTypeId = -1;

    @BeforeAll
    public static void setUpAll() throws SQLException {
        leaveRequestDAO = new LeaveRequestDAO();
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false);

        try {
            //Test User
            testUserId = insertTestUser("leave-test-user");
            testAddressId = insertTestAddress();
            testEmployeeId = insertTestEmployee(testUserId, testAddressId);
            vacationLeaveTypeId = getLeaveTypeIdByName("Vacation Leave");

            // Assign a starting leave balance to the test employee
            insertLeaveBalance(testEmployeeId, vacationLeaveTypeId, 15.0);
            conn.commit();
            System.out.println("Created a temporary employee with ID: " + testEmployeeId + " for all leave tests.");
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Failed to set up tests: " + e.getMessage());
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
            // Delete all test data first
            deleteTestEmployee(testEmployeeId, testUserId, testAddressId);
            conn.commit();

            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
            TestDBUtils.resetAutoIncrement(conn, "leavetype", "LeaveTypeID");
            TestDBUtils.resetAutoIncrement(conn, "leaverequest", "LeaveRequestID");
            TestDBUtils.resetAutoIncrement(conn, "leavebalance", "LeaveBalanceID");

            conn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Submit and Get Leave Request")
    public void testSubmitAndGetLeaveRequest() throws SQLException {
        System.out.println("Testing submission and retrieval of a leave request...");
        // Setup
        LeaveRequest newRequest = new LeaveRequest(
                0, // Request ID is generated
                String.valueOf(testEmployeeId),
                vacationLeaveTypeId,
                "Vacation Leave",
                Date.valueOf(LocalDate.of(2025, 10, 10)),
                "Pending",
                "Family trip",
                null, // Submission date is generated
                null, null, null, null, null
        );

        // Submit the request
        boolean submitted = leaveRequestDAO.submitLeaveRequest(conn, newRequest);
        assertTrue(submitted, "submitLeaveRequest should return true on success.");
        conn.commit();

        // Retrieve the request and verify its details
        List<LeaveRequest> requests = leaveRequestDAO.getLeaveRequestsByEmployeeNo(String.valueOf(testEmployeeId));
        assertNotNull(requests);
        assertFalse(requests.isEmpty(), "The list of leave requests should not be empty.");
        assertEquals(1, requests.size());

        LeaveRequest retrievedRequest = requests.get(0);
        assertEquals("Pending", retrievedRequest.getStatus());
        assertEquals("Family trip", retrievedRequest.getReason());
    }

    @Test
    @Order(2)
    @DisplayName("Test Update Leave Request Status")
    public void testUpdateLeaveRequestStatus() throws SQLException {
        System.out.println("Testing updating a leave request's status...");
        // Submit a request to be updated
        Date requestDate = Date.valueOf(LocalDate.of(2025, 11, 15));
        LeaveRequest newRequest = new LeaveRequest(0, String.valueOf(testEmployeeId), vacationLeaveTypeId, "Vacation Leave", requestDate, "Pending", "Personal day", null, null, null, null, null, null);
        leaveRequestDAO.submitLeaveRequest(conn, newRequest);
        conn.commit();

        // Update the status to 'Approved'
        boolean updated = leaveRequestDAO.updateLeaveRequestStatusAndRemarks(conn, String.valueOf(testEmployeeId), requestDate, "Approved", "Approved by Test Manager", "10007");
        assertTrue(updated, "updateLeaveRequestStatus should return true.");
        conn.commit();

        // Retrieve and check the updated status
        List<LeaveRequest> requests = leaveRequestDAO.getLeaveRequestsByEmployeeNo(String.valueOf(testEmployeeId));
        LeaveRequest updatedRequest = requests.stream()
                .filter(r -> r.getRequestedDate().equals(requestDate))
                .findFirst()
                .orElse(null);

        assertNotNull(updatedRequest, "Updated request should be found.");
        assertEquals("Approved", updatedRequest.getStatus());
        assertEquals("Approved by Test Manager", updatedRequest.getRemarks());
        assertNotNull(updatedRequest.getApprovalDate(), "Approval date should be set.");
    }

    @Test
    @Order(3)
    @DisplayName("Test Deduct Leave Balance")
    public void testDeductLeaveBalance() throws SQLException {
        System.out.println("Testing deduction of leave balance...");
        double initialBalance = 15.0;
        double daysToDeduct = 1.0;

        // Act
        boolean deducted = leaveRequestDAO.deductLeaveBalance(conn, String.valueOf(testEmployeeId), vacationLeaveTypeId, daysToDeduct);
        assertTrue(deducted, "deductLeaveBalance should return true.");
        conn.commit();

        // Check the new balance directly
        String sql = "SELECT BalanceDays FROM leavebalance WHERE EmployeeID = ? AND LeaveTypeID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testEmployeeId);
            pstmt.setInt(2, vacationLeaveTypeId);
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Leave balance record should exist.");
            assertEquals(initialBalance - daysToDeduct, rs.getDouble("BalanceDays"), 0.01, "Leave balance should be deducted correctly.");
        }
    }

    // HELPER METHODS
    private static int getLeaveTypeIdByName(String leaveTypeName) throws SQLException {
        String selectSql = "SELECT LeaveTypeID FROM leavetype WHERE LeaveName = ?";
        try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
            selectPstmt.setString(1, leaveTypeName);
            ResultSet rs = selectPstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("LeaveTypeID");
            } else {
                throw new SQLException("Prerequisite leave type '" + leaveTypeName + "' not found in the database.");
            }
        }
    }

    private static int insertTestUser(String username) throws SQLException {
        String sql = "INSERT INTO user (Username, Password) VALUES (?, 'testpass')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
    }

    private static int insertTestAddress() throws SQLException {
        String sql = "INSERT INTO address (UnitOrHouseStreet) VALUES ('123 Test Lane')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating address failed, no ID obtained.");
                }
            }
        }
    }

    private static int insertTestEmployee(int userId, int addressId) throws SQLException {
        String sql = "INSERT INTO employee (UserID, AddressID, LastName, FirstName) VALUES (?, ?, 'Test', 'LeaveUser')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, addressId);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating employee failed, no ID obtained.");
                }
            }
        }
    }

    private static void insertLeaveBalance(int employeeId, int leaveTypeId, double balance) throws SQLException {
        String sql = "INSERT INTO leavebalance (EmployeeID, LeaveTypeID, BalanceDays) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, leaveTypeId);
            pstmt.setDouble(3, balance);
            pstmt.executeUpdate();
        }
    }

    private static void deleteTestEmployee(int employeeId, int userId, int addressId) throws SQLException {
        if (employeeId == -1 && userId == -1) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
        }
        try {
            if (employeeId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM leaverequest WHERE EmployeeID = ?")) {
                    pstmt.setInt(1, employeeId);
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM leavebalance WHERE EmployeeID = ?")) {
                    pstmt.setInt(1, employeeId);
                    pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM employee WHERE EmployeeID = ?")) {
                    pstmt.setInt(1, employeeId);
                    pstmt.executeUpdate();
                }
            }
            if (userId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM user WHERE UserID = ?")) {
                    pstmt.setInt(1, userId);
                    pstmt.executeUpdate();
                }
            }
            if (addressId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM address WHERE AddressID = ?")) {
                    pstmt.setInt(1, addressId);
                    pstmt.executeUpdate();
                }
            }
        } finally {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
            }
        }
    }
}
