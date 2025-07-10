package Testing;

import dao.AddressDAO;
import dao.EmployeeDAO;
import data.DBConnection;
import model.RegularEmployee;
import model.Employee;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestEmployee {

    private static EmployeeDAO empDao;
    private static Connection conn;
    
    @BeforeAll
    public static void setUpAll() throws SQLException {
        empDao = new EmployeeDAO();
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false); // Manually manage transactions for tests
    }

    @AfterEach
    public void tearDownEach() throws SQLException {
        // Rollback any changes after each test to ensure isolation
        if (conn != null) {
            conn.rollback();
        }
    }

    @AfterAll
    public static void tearDownAll() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            // Reset auto-increment values for a clean slate on next run
            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
            TestDBUtils.resetAutoIncrement(conn, "salary", "SalaryID");
            TestDBUtils.resetAutoIncrement(conn, "userrole", "UserRoleID");
            conn.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Get Employee By Employee No - Success")
    public void testGetEmployeeByEmployeeNo_Success() throws SQLException {
        System.out.println("Testing successful retrieval of an existing employee (ID: 10002)...");
        Employee emp = empDao.getEmployeeByEmployeeNo("10002");
        assertNotNull(emp, "Employee should not be null for a valid ID.");
        assertEquals("Lim", emp.getEmployeeLN(), "Employee's last name should match the record in the database.");
    }

    @Test
    @Order(2)
    @DisplayName("Test Get Employee By Employee No - Fail")
    public void testGetEmployeeByEmployeeNo_Fail() throws SQLException {
        System.out.println("Testing failed retrieval of a non-existent employee (ID: 99999)...");
        Employee emp = empDao.getEmployeeByEmployeeNo("99999");
        assertNull(emp, "Employee should be null for a non-existent ID.");
    }

    @Test
    @Order(3)
    @DisplayName("Test Add Regular Employee")
    public void testAddRegularEmployee() throws SQLException {
        System.out.println("Testing the addition of a new regular employee...");
        int userId = -1;
        int addressId = -1;
        int employeeId = -1;

        try {
            // 1. Setup: Create a temporary user, address, and user role
            userId = insertTestUser("atest-user" + UUID.randomUUID());
            addressId = insertTestAddress();
            insertTestUserRole(userId, 3); // Assuming RoleID 3 is for "Payroll Rank and File"

            // 2. Act: Create and add the new employee
            Employee testEmp = createTestEmployeeObject(String.valueOf(userId), String.valueOf(addressId));
            boolean added = empDao.addEmployee(conn, testEmp);
            assertTrue(added, "addEmployee should return true on success.");
            
            conn.commit(); // Commit to make the new employee visible for verification

            // 3. Assert: Verify the employee was inserted correctly
            String sql = "SELECT EmployeeID, LastName, AddressID FROM employee WHERE UserID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                assertTrue(rs.next(), "Employee should exist in the database after being added.");
                employeeId = rs.getInt("EmployeeID");
                assertEquals("TestLast", rs.getString("LastName"));
                assertEquals(addressId, rs.getInt("AddressID"));
                System.out.println("Successfully verified new employee with ID: " + employeeId);
            }
        } finally {
            // 4. Cleanup: Hard delete all created test data
            deleteTestEmployee(employeeId, userId, addressId);
            conn.commit();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test Update Employee Details")
    public void testUpdateEmployeeDetails() throws SQLException {
        System.out.println("Testing the update of an employee's details...");
        int userId = -1, addressId = -1, employeeId = -1;

        try {
            // 1. Setup: Create a temporary employee to update
            userId = insertTestUser("utest-user-" + UUID.randomUUID());
            addressId = insertTestAddress();
            insertTestUserRole(userId, 3); // Add user role
            Employee testEmp = createTestEmployeeObject(String.valueOf(userId), String.valueOf(addressId));
            empDao.addEmployee(conn, testEmp);
            conn.commit();

            // Retrieve the full employee object to get its generated ID
            Employee employeeToUpdate = empDao.getEmployeeByEmployeeNo(String.valueOf(userId));
            assertNotNull(employeeToUpdate, "Could not retrieve newly created employee for update.");
            employeeId = Integer.parseInt(employeeToUpdate.getEmployeeNo());

            // 2. Act: Change details and call the update method
            String newPhoneNumber = "09987654321";
            employeeToUpdate.setEmployeePhoneNumber(newPhoneNumber);
            employeeToUpdate.setEmployeeStatus("Regular");
            
            boolean updated = empDao.updateEmployee(conn, employeeToUpdate);
            assertTrue(updated, "updateEmployee should return true on success.");
            conn.commit();

            // 3. Assert: Verify the details were updated in the database
            Employee updatedEmployee = empDao.getEmployeeByEmployeeNo(String.valueOf(employeeId));
            assertNotNull(updatedEmployee);
            assertEquals(newPhoneNumber, updatedEmployee.getEmployeePhoneNumber(), "Phone number should be updated.");
            assertEquals("Regular", updatedEmployee.getEmployeeStatus(), "Employee status should be updated.");
            System.out.println("Successfully verified employee update.");

        } finally {
            // 4. Cleanup
            deleteTestEmployee(employeeId, userId, addressId);
            conn.commit();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test Delete Employee (Soft Delete)")
    public void testDeleteEmployee_SoftDelete() throws SQLException {
        System.out.println("Testing the soft deletion of an employee...");
        int userId = -1, addressId = -1, employeeId = -1;

        try {
            // 1. Setup: Create a temporary employee to delete
            userId = insertTestUser("dtest-user-" + UUID.randomUUID());
            addressId = insertTestAddress();
            insertTestUserRole(userId, 3); // Add user role
            Employee testEmp = createTestEmployeeObject(String.valueOf(userId), String.valueOf(addressId));
            empDao.addEmployee(conn, testEmp);
            conn.commit();
            
            Employee employeeToDelete = empDao.getEmployeeByEmployeeNo(String.valueOf(userId));
            assertNotNull(employeeToDelete, "Could not retrieve newly created employee for deletion.");
            employeeId = Integer.parseInt(employeeToDelete.getEmployeeNo());

            // 2. Act: Call the delete method
            boolean deleted = empDao.deleteEmployee(conn, String.valueOf(employeeId));
            assertTrue(deleted, "deleteEmployee should return true on success.");
            conn.commit();

            // 3. Assert: Verify the IsDeleted flag is set to true
            String sql = "SELECT IsDeleted FROM employee WHERE EmployeeID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);
                ResultSet rs = stmt.executeQuery();
                assertTrue(rs.next(), "Employee record should still exist after soft delete.");
                assertTrue(rs.getBoolean("IsDeleted"), "IsDeleted flag should be true after soft delete.");
                System.out.println("Successfully verified employee soft delete.");
            }
        } finally {
            // 4. Cleanup
            deleteTestEmployee(employeeId, userId, addressId);
            conn.commit();
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Get All Employees")
    public void testGetAllEmployees() throws SQLException {
        System.out.println("Testing retrieval of all employees...");
        
        // Get the initial count of employees
        int initialCount = empDao.getAllEmployees().size();
        
        // Setup: Add two new employees to ensure there's something to fetch
        int userId1 = -1, addressId1 = -1, employeeId1 = -1;
        int userId2 = -1, addressId2 = -1, employeeId2 = -1;
        
        try {
            // Employee 1
            userId1 = insertTestUser("gtest-user1-" + UUID.randomUUID());
            addressId1 = insertTestAddress();
            insertTestUserRole(userId1, 3);
            empDao.addEmployee(conn, createTestEmployeeObject(String.valueOf(userId1), String.valueOf(addressId1)));
            
            // Employee 2
            userId2 = insertTestUser("gtest-user2-" + UUID.randomUUID());
            addressId2 = insertTestAddress();
            insertTestUserRole(userId2, 3);
            empDao.addEmployee(conn, createTestEmployeeObject(String.valueOf(userId2), String.valueOf(addressId2)));
            
            conn.commit();
            
            // Retrieve IDs for cleanup
            employeeId1 = Integer.parseInt(empDao.getEmployeeByEmployeeNo(String.valueOf(userId1)).getEmployeeNo());
            employeeId2 = Integer.parseInt(empDao.getEmployeeByEmployeeNo(String.valueOf(userId2)).getEmployeeNo());

            // Act: Call the method to get all employees
            List<Employee> allEmployees = empDao.getAllEmployees();

            // Assert: Check if the list size has increased accordingly
            assertNotNull(allEmployees, "The list of employees should not be null.");
            assertFalse(allEmployees.isEmpty(), "The list of employees should not be empty.");
            assertEquals(initialCount + 2, allEmployees.size(), "The list size should increase by two.");
            System.out.println("Successfully verified that getAllEmployees returns an updated list.");

        } finally {
            // Cleanup
            deleteTestEmployee(employeeId1, userId1, addressId1);
            deleteTestEmployee(employeeId2, userId2, addressId2);
            conn.commit();
        }
    }

    // --- HELPER METHODS ---

    private Employee createTestEmployeeObject(String userId, String addressId) {
        Employee testEmp = new RegularEmployee();
        testEmp.setUserID(userId);
        testEmp.setEmployeeAddress("123 Test St, Test Brgy, Test City, Test Province, Test Region, 12345");
        testEmp.setEmployeeLN("TestLast");
        testEmp.setEmployeeFN("TestFirst");
        testEmp.setEmployeeDOB(Date.valueOf("2000-01-01"));
        testEmp.setEmployeePhoneNumber("09123456789");
        testEmp.setEmployeePosition("Payroll Rank and File");
        testEmp.setEmployeeSSS("11-1111111-1");
        testEmp.setEmployeePhilHealth("222222222222");
        testEmp.setEmployeeTIN("333-333-333");
        testEmp.setEmployeePagIbig("444444444444");
        testEmp.setEmployeeSupervisor("10007");
        testEmp.setBasicSalary(30000);
        testEmp.setEmployeeStatus("Probationary");
        return testEmp;
    }


    private int insertTestUser(String username) throws SQLException {
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


    private int insertTestAddress() throws SQLException {
        String sql = "INSERT INTO address (UnitOrHouseStreet, Barangay, CityMunicipality, Province, Region, PostalCode) VALUES ('123 Test St', 'Test Brgy', 'Test City', 'Test Province', 'Test Region', '12345')";
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


    private void insertTestUserRole(int userId, int roleId) throws SQLException {
        String sql = "INSERT INTO userrole (UserID, RoleID) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, roleId);
            pstmt.executeUpdate();
        }
    }


    private void deleteTestEmployee(int employeeId, int userId, int addressId) throws SQLException {
        if (employeeId == -1 && userId == -1 && addressId == -1) return;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
        }

        try {
            if (employeeId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM salary WHERE EmployeeID = ?")) { pstmt.setInt(1, employeeId); pstmt.executeUpdate(); }
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM employee WHERE EmployeeID = ?")) { pstmt.setInt(1, employeeId); pstmt.executeUpdate(); }
            }
            if (userId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM userrole WHERE UserID = ?")) { pstmt.setInt(1, userId); pstmt.executeUpdate(); }
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






//package Testing;
//
//import dao.AddressDAO;
//import dao.EmployeeDAO;
//import data.DBConnection;
//import model.RegularEmployee;
//import model.Employee;
//import org.junit.jupiter.api.*;
//import java.sql.*;
//import java.util.UUID;
//import static org.junit.jupiter.api.Assertions.*;
//import util.TestDBUtils;
//
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//
//public class TestEmployee {
//
//    private static EmployeeDAO empDao;
//    private static Connection conn;
//    private static AddressDAO addDao;
//    
//
//    @BeforeAll
//    public static void setUp() throws SQLException {
//        empDao = new EmployeeDAO();
//        conn = DBConnection.getConnection();
//        addDao = new AddressDAO();
//    }
//
//    @Test
//    @Order(1)
//    public void testGetEmployeeByEmployeeNo_Success() throws SQLException {
//        System.out.println("Calling getEmployeeByEmployeeNo from EmployeeDAO to verify if an employee does exist using an Employee No: 10002.");
//        Employee emp = empDao.getEmployeeByEmployeeNo("10002");
//        assertNotNull(emp, "ID should not be null.");
//        assertEquals("Lim", emp.getEmployeeLN());
//    }
//
//    @Test
//    @Order(2)
//    public void testGetEmployeeByEmployeeNo_Fail() throws SQLException {
//        System.out.println("Calling getEmployeeByEmployeeNo from EmployeeDAO to verify if an employee does exist using an Employee No: 99999.");
//        Employee emp = empDao.getEmployeeByEmployeeNo("99999");
//        assertNull(emp, "ID should not be null.");
//    }
//
//    @Test
//    @Order(3)
//    public void testAddRegularEmployee() throws SQLException {
//
//        System.out.println("Calling addUser from CredentialDAO to add a temporary user, required step to add an employee.");
//        // Insert a temporary user
//        String tempUsername = "testuser." + UUID.randomUUID();
//        String tempPassword = "pass123";
//        String insertUserSql = "INSERT INTO user (Username, Password) VALUES (?, ?)";
//
//        int userId;
//        try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
//            userStmt.setString(1, tempUsername);
//            userStmt.setString(2, tempPassword);
//            userStmt.executeUpdate();
//            try (ResultSet rs = userStmt.getGeneratedKeys()) {
//                assertTrue(rs.next(), "User ID should be generated.");
//                userId = rs.getInt(1);
//            }
//        }
//        // Creating and Adding the user
//        System.out.println("Creating and adding an employee to the database.");
//        Employee testEmp = new RegularEmployee();
//        testEmp.setUserID(String.valueOf(userId));
//        testEmp.setEmployeeLN("TestLast");
//        testEmp.setEmployeeFN("TestFirst");
//        testEmp.setEmployeeDOB(Date.valueOf("2000-01-01"));
//        testEmp.setEmployeePhoneNumber("09123456789");
//        testEmp.setEmployeePosition("Payroll Rank and File");
//        testEmp.setEmployeeAddress("123 Test St, Brgy Test, CityTest, ProvinceTest, RegionTest, 1234");
//        testEmp.setEmployeeSSS("123-4567890-0");
//        testEmp.setEmployeePhilHealth("1234567890");
//        testEmp.setEmployeeTIN("123-456-789");
//        testEmp.setEmployeePagIbig("123456789012");
//        testEmp.setEmployeeSupervisor("10007");
//        testEmp.setBasicSalary(25000);
//        testEmp.setEmployeeStatus("Probationary");
//
//        boolean added = empDao.addEmployee(conn, testEmp);
//        assertTrue(added, "Employee should be added successfully.");
//
//        // Verify employee was inserted
//        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM employee WHERE EmployeeID = ?")) {
//
//            stmt.setString(1, String.valueOf(userId));
//            try (ResultSet rs = stmt.executeQuery()) {
//                assertTrue(rs.next(), "Employee should exist in DB.");
//                assertEquals("TestLast", rs.getString("LastName"));
//                System.out.println("Verify test employee was inserted with Employee #: " + rs.getInt("EmployeeID") + ".");
//                System.out.println(rs.getInt("AddressID"));
//            }
//        }
//
//        // Cleanup
//        System.out.println("Restoring the database to what it was before the test.");
//        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM employee WHERE UserID = ?")) {
//            stmt.setInt(1, userId);
//            stmt.executeUpdate();
//        }
//        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM userrole WHERE UserID = ?")) {
//            stmt.setInt(1, userId);
//            stmt.executeUpdate();
//        }
//        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM user WHERE UserID = ?")) {
//            stmt.setInt(1, userId);
//            stmt.executeUpdate();
//        }
//        AddressDAO.AddressComponents addr = addDao.getAddressComponentsById(36);
//        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM address WHERE AddressID = ?")){
//            stmt.setInt(1, 36);
//            stmt.executeUpdate();
//        }
//    }
//    
//
//    @AfterAll
//    public static void tearDown() throws SQLException {
//        if (conn != null && !conn.isClosed()) {
//            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
//            TestDBUtils.resetAutoIncrement(conn, "userrole", "UserRoleID");
//            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
//            TestDBUtils.resetAutoIncrement(conn, "salary", "SalaryID");
//            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
//            conn.close();
//        }
//    }
//}
