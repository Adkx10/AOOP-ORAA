/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Testing;

import dao.CredentialDAO;
import data.DBConnection;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class TestCredential {

    private static CredentialDAO dao;
    private static Connection conn;
    private static String testUsername;
    private static String testPassword;
    private static String testUserId;

    @BeforeAll
    public static void setup() throws SQLException {
        dao = new CredentialDAO();
        conn = DBConnection.getConnection();
    }

    @Test
    @Order(1)
    public void testAuthenticateUser_ValidCredentials() throws SQLException {
        System.out.println("Calling authenticateUser from CredentialDAO to verify if credentials UN:brad.sanjose & PW:Test123 is valid");
        assertTrue(dao.authenticateUser("brad.sanjose", "Test123"), "Authentication should succeed.");
    }

    @Test
    @Order(2)
    public void testAuthenticateUser_InvalidCredentials() throws SQLException {
        System.out.println("Calling authenticateUser from CredentialDAO to verify if credentials UN:fakeuser & fakepass is valid");
        assertFalse(dao.authenticateUser("fakeuser", "fakepass"), "Authentication should fail.");
    }

    @Test
    @Order(3)
    public void testGetUserRoles_success() throws SQLException {
        System.out.println("Calling getUserRoles from CredentialDAO to check if manuel.garcia has Admin role.");
        List<String> roles = dao.getUserRoles("manuel.garcia");
        assertNotNull(roles);
        assertTrue(roles.contains("Admin"), "User should have 'Admin' role.");
    }

    @Test
    @Order(4)
    public void testGetUserRoles_fail() throws SQLException {
        System.out.println("Calling getUserRoles from CredentialDAO to check if brad.sanjose has Admin role.");
        List<String> roles = dao.getUserRoles("brad.sanjose");
        assertNotNull(roles);
        assertFalse(roles.contains("Admin"), "User should have 'Admin' role.");
    }

    @Test
    @Order(5)
    public void testGetUserIdByUsername() throws SQLException {
        System.out.println("Calling getUserIdByUsername from CredentialDAO to get the user ID 10007 of brad.sanjose.");
        String userId = dao.getUserIdByUsername("brad.sanjose");
        assertEquals("10007", userId, "Returned user ID should match inserted one.");
    }

    @Test
    @Order(6)
    public void testGetPrimaryRoleNameByEmployeeId_Admin() throws SQLException {
        System.out.println("Calling getPrimaryRoleNamebyEmployeeId from Credential DAO to get the primary role 'Admin' of Manuel Garcia III using his Employee ID 10001.");
        String primaryRole = dao.getPrimaryRoleNameByEmployeeId("10001");
        assertEquals("Admin", primaryRole, "Primary role should be 'Admin'.");
    }

    @Test
    @Order(7)
    public void testGetPrimaryRoleNameByEmployeeId_Manager() throws SQLException {
        System.out.println("Calling getPrimaryRoleNamebyEmployeeId from Credential DAO to get the primary role 'Manager' of Brad San Jose using his Employee ID 10007.");
        String primaryRole = dao.getPrimaryRoleNameByEmployeeId("10007");
        assertEquals("Manager", primaryRole, "Primary role should be 'Manager'.");
    }

    @Test
    @Order(8)
    public void testGetPrimaryRoleNameByEmployeeId_RegularEmp() throws SQLException {
        System.out.println("Calling getPrimaryRoleNamebyEmployeeId from Credential DAO to get the primary role 'Regular Employee' of Mark Bautista using his Employee ID 10020.");
        String primaryRole = dao.getPrimaryRoleNameByEmployeeId("10020");
        assertEquals("Regular Employee", primaryRole, "Primary role should be 'Regular Employee'.");
    }

    @Test
    @Order(9)
    public void testAssignRole() throws SQLException {
        System.out.println("Calling assignRoleToUser from CredentialDAO to assign new role for Mark Bautista");
        String username = "mark.bautista";
        String userId = dao.getUserIdByUsername(username);
        assertNotNull(userId);

        // Step 1: Get original roles
        List<String> originalRoles = dao.getUserRoles(username);

        // Step 2: Assign a temporary role
        boolean assigned = dao.assignRoleToUser(conn, userId, "Admin");
        assertTrue(assigned, "Temporary role assignment should succeed.");

        // Step 3: Confirm the new role exists
        List<String> rolesAfter = dao.getUserRoles(username);
        assertTrue(rolesAfter.contains("Admin"));

        // Step 4: Restore original roles
        // For simplicity: delete all roles and re-assign the originals
        try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM userrole WHERE UserID = ?")) {
            deleteStmt.setString(1, userId);
            deleteStmt.executeUpdate();
        }

        for (String role : originalRoles) {
            boolean restored = dao.assignRoleToUser(conn, userId, role);
            assertTrue(restored, "Restored role: " + role);
        }

        // Step 5: Verify final role state matches the original
        List<String> finalRoles = dao.getUserRoles(username);
        assertEquals(originalRoles.size(), finalRoles.size(), "Role count should match original");
        assertTrue(finalRoles.containsAll(originalRoles), "Roles should be fully restored");
    }

    @Test
    @Order(10)
    public void testAddUser() throws SQLException {

        String username = "user.temp." + UUID.randomUUID();
        String password = "restoreTest123";
        String userId = null;

        System.out.println("Calling addUser from CredentialDAO for: " + username);

        // Step 2: Ensure user does not already exist
        try (PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM user WHERE Username = ?")) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    assertEquals(0, count, "Username should not already exist before test.");
                }
            }
        }

        // Step 3: Add user using DAO
        userId = dao.addUser(conn, username, password);
        assertNotNull(userId, "Generated user ID should not be null after addUser.");
        assertTrue(Integer.parseInt(userId) > 0, "Generated user ID should be a valid number.");

        // Step 4: Verify user exists
        try (PreparedStatement verifyStmt = conn.prepareStatement("SELECT Username FROM user WHERE UserID = ?")) {
            verifyStmt.setInt(1, Integer.parseInt(userId));
            try (ResultSet rs = verifyStmt.executeQuery()) {
                assertTrue(rs.next(), "Inserted user should exist in database.");
                assertEquals(username, rs.getString("Username"));
            }
        }

        // Step 5: Restore – remove inserted user (roles first)
        try (PreparedStatement deleteRoleStmt = conn.prepareStatement("DELETE FROM userrole WHERE UserID = ?")) {
            deleteRoleStmt.setInt(1, Integer.parseInt(userId));
            deleteRoleStmt.executeUpdate();
        }

        try (PreparedStatement deleteUserStmt = conn.prepareStatement("DELETE FROM user WHERE UserID = ?")) {
            deleteUserStmt.setInt(1, Integer.parseInt(userId));
            deleteUserStmt.executeUpdate();
        }

        // Step 6: Confirm user is deleted
        try (PreparedStatement confirmStmt = conn.prepareStatement("SELECT * FROM user WHERE UserID = ?")) {
            confirmStmt.setInt(1, Integer.parseInt(userId));
            try (ResultSet rs = confirmStmt.executeQuery()) {
                assertFalse(rs.next(), "User should be deleted after test cleanup.");
            }
        }
    }

    @AfterAll
    public static void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
            TestDBUtils.resetAutoIncrement(conn, "userrole", "UserRoleID");
            conn.close();
        }
    }
}
