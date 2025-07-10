package Testing;

import dao.PayrollDAO;
import data.DBConnection;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import util.TestDBUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestPayroll {

    private static Connection conn;
    private PayrollDAO payrollDAO;

    // This will store the EmployeeID that is generated in @BeforeAll
    private static int SINGLE_TEST_EMPLOYEE_ID = -1;

    @BeforeAll
    public static void setUpAll() throws SQLException {
        conn = DBConnection.getConnection();
        conn.setAutoCommit(false); // Manage transaction manually for setup/teardown
        System.out.println("--- TestPayroll: Database connection established. AutoCommit set to false. ---");

        // --- AUTOMATICALLY INSERT TEST DATA ---
        int userId = -1;
        int addressId = -1;

        try {
            // 1. Insert User and get UserID
            String userSql = "INSERT INTO user (Username, Password) VALUES ('single_test_user', 'pass123')";
            try (PreparedStatement pstmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId = rs.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            // Insert Address and get AddressID
            String addressSql = "INSERT INTO address (UnitOrHouseStreet, Barangay, CityMunicipality, Province, Region, PostalCode) VALUES ('123 Sample St', 'Brgy Test', 'Test City', 'Test Province', 'Region Test', '12345')";
            try (PreparedStatement pstmt = conn.prepareStatement(addressSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        addressId = rs.getInt(1);
                    } else {
                        throw new SQLException("Creating address failed, no ID obtained.");
                    }
                }
            }

            // 3. Insert Employee and get EmployeeID
            String employeeSql = "INSERT INTO employee (UserID, AddressID, LastName, FirstName, Birthday, PhoneNumber, SSSN, PhHN, TIN, HDMFN, EmpStatus, PositionID, SupervisorID) VALUES (?, ?, 'Test', 'One', '1990-01-01', '09123456789', 'SSSAAA', 'PHBBB', 'TINCCC', 'PAGDDD', 'Regular', 1, '10007')";
            try (PreparedStatement pstmt = conn.prepareStatement(employeeSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, addressId);
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        SINGLE_TEST_EMPLOYEE_ID = rs.getInt(1); // Set the static variable for all tests
                    } else {
                        throw new SQLException("Creating employee failed, no ID obtained.");
                    }
                }
            }

            // 4. Insert Salary
            String salarySql = "INSERT INTO salary (EmployeeID, BasicSalary, EffectiveDate) VALUES (?, 30000.00, '2024-01-01')";
            try (PreparedStatement pstmt = conn.prepareStatement(salarySql)) {
                pstmt.setInt(1, SINGLE_TEST_EMPLOYEE_ID);
                pstmt.executeUpdate();
            }

            // 5. Insert UserRole
            String userRoleSql = "INSERT INTO userrole (UserID, RoleID) VALUES (?, 3)";
            try (PreparedStatement pstmt = conn.prepareStatement(userRoleSql)) {
                pstmt.setInt(1, userId);
                pstmt.executeUpdate();
            }

            conn.commit(); // Commit all the setup data
            System.out.println("Successfully inserted test employee with generated ID: " + SINGLE_TEST_EMPLOYEE_ID);

        } catch (SQLException e) {
            conn.rollback(); // Rollback if any part of the setup fails
            System.err.println("Error during automated test data setup. Rolling back.");
            e.printStackTrace();
            throw e;
        }
    }

    // Runs before each test method
    @BeforeEach
    public void setUp() {
        payrollDAO = new PayrollDAO();
        System.out.println("\n--- Starting new test method ---");
    }

    // Runs after each test method
    @AfterEach
    public void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.rollback();
            System.out.println("--- TestPayroll: Transaction rolled back for test method. ---");
        }
    }

    // Teardown runs once after all tests to clean up the data
    @AfterAll
    public static void tearDownAll() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            if (SINGLE_TEST_EMPLOYEE_ID != -1) {
                deleteTestEmployeeFully(SINGLE_TEST_EMPLOYEE_ID);
            }

            // Reset AUTO_INCREMENTs to keep the database clean for the next full run
            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
            TestDBUtils.resetAutoIncrement(conn, "salary", "SalaryID");
            TestDBUtils.resetAutoIncrement(conn, "payperiod", "PayPeriodID");
            TestDBUtils.resetAutoIncrement(conn, "payslip", "PayslipID");
            TestDBUtils.resetAutoIncrement(conn, "payslipdetail", "PayslipDetailID");
            TestDBUtils.resetAutoIncrement(conn, "deduction", "DeductionID");
            TestDBUtils.resetAutoIncrement(conn, "taxcomputation", "TaxComputationID");
            TestDBUtils.resetAutoIncrement(conn, "allowancetype", "AllowanceTypeID");
            TestDBUtils.resetAutoIncrement(conn, "allowance", "AllowanceID");
            TestDBUtils.resetAutoIncrement(conn, "attendance", "AttendanceID");
            TestDBUtils.resetAutoIncrement(conn, "leaverequest", "LeaveRequestID");
            TestDBUtils.resetAutoIncrement(conn, "leavebalance", "LeaveBalanceID");
            TestDBUtils.resetAutoIncrement(conn, "userrole", "UserRoleID");
            TestDBUtils.resetAutoIncrement(conn, "role", "RoleID");

            conn.close();
            System.out.println("--- TestPayroll: Database connection closed and AUTO_INCREMENTs reset. ---");
        }
    }

    private static void deleteTestEmployeeFully(int generatedEmployeeId) throws SQLException {
        if (generatedEmployeeId == -1) return;

        int userId = -1;
        int addressId = -1;
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT UserID, AddressID FROM employee WHERE EmployeeID = ?")) {
            pstmt.setInt(1, generatedEmployeeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                userId = rs.getInt("UserID");
                addressId = rs.getInt("AddressID");
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
        }

        try {
            try (PreparedStatement pstmtAttendance = conn.prepareStatement("DELETE FROM attendance WHERE EmployeeID = ?");
                 PreparedStatement pstmtPayslipDetail = conn.prepareStatement("DELETE FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ?)");
                 PreparedStatement pstmtTaxComputation = conn.prepareStatement("DELETE FROM taxcomputation WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ?)");
                 PreparedStatement pstmtDeduction = conn.prepareStatement("DELETE FROM deduction WHERE EmployeeID = ?");
                 PreparedStatement pstmtPayslip = conn.prepareStatement("DELETE FROM payslip WHERE EmployeeID = ?");
                 PreparedStatement pstmtPayPeriod = conn.prepareStatement("DELETE FROM payperiod WHERE PeriodName LIKE '%-TEST'"); // Only delete PeriodName with the '-TEST' suffix from 'payperiod' table to only remove test data.
                 PreparedStatement pstmtAllowance = conn.prepareStatement("DELETE FROM allowance WHERE EmployeeID = ?");
                 PreparedStatement pstmtSalary = conn.prepareStatement("DELETE FROM salary WHERE EmployeeID = ?");
                 PreparedStatement pstmtEmployee = conn.prepareStatement("DELETE FROM employee WHERE EmployeeID = ?");
                 PreparedStatement pstmtUserRole = conn.prepareStatement("DELETE FROM userrole WHERE UserID = ?");
                 PreparedStatement pstmtUser = conn.prepareStatement("DELETE FROM user WHERE UserID = ?");
                 PreparedStatement pstmtAddress = conn.prepareStatement("DELETE FROM address WHERE AddressID = ?"))
            {
                pstmtAttendance.setInt(1, generatedEmployeeId); pstmtAttendance.executeUpdate();
                pstmtPayslipDetail.setInt(1, generatedEmployeeId); pstmtPayslipDetail.executeUpdate();
                pstmtTaxComputation.setInt(1, generatedEmployeeId); pstmtTaxComputation.executeUpdate();
                pstmtDeduction.setInt(1, generatedEmployeeId); pstmtDeduction.executeUpdate();
                pstmtPayslip.setInt(1, generatedEmployeeId); pstmtPayslip.executeUpdate();
                pstmtPayPeriod.executeUpdate();
                pstmtAllowance.setInt(1, generatedEmployeeId); pstmtAllowance.executeUpdate();
                pstmtSalary.setInt(1, generatedEmployeeId); pstmtSalary.executeUpdate();
                pstmtEmployee.setInt(1, generatedEmployeeId); pstmtEmployee.executeUpdate();
                if (userId != -1) {
                    pstmtUserRole.setInt(1, userId); pstmtUserRole.executeUpdate();
                    pstmtUser.setInt(1, userId); pstmtUser.executeUpdate();
                }
                if (addressId != -1) {
                    pstmtAddress.setInt(1, addressId); pstmtAddress.executeUpdate();
                }
            }
            conn.commit();
            System.out.println("Hard cleaned up test employee with EmployeeID: " + generatedEmployeeId);
        } catch (SQLException e) {
            System.err.println("Error hard cleaning up test employee with EmployeeID " + generatedEmployeeId + ": " + e.getMessage());
            conn.rollback();
            throw e;
        } finally {
            try (Statement stmt = conn.createStatement()) { stmt.execute("SET FOREIGN_KEY_CHECKS = 1;"); }
        }
    }

    private void insertAttendance(int employeeId, LocalDate date, String logInTime, String logOutTime) throws SQLException {
        String sql = "INSERT INTO attendance (EmployeeID, Date, LogInTime, LogOutTime) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(date));
            pstmt.setString(3, logInTime);
            pstmt.setString(4, logOutTime);
            pstmt.executeUpdate();
            System.out.println("Inserted attendance for " + employeeId + " on " + date);
        }
    }

    private void insertAllowance(int employeeId, String allowanceName, double amount, LocalDate effectiveDate) throws SQLException {
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
                pstmt.setDate(4, Date.valueOf(effectiveDate));
                pstmt.executeUpdate();
                System.out.println("Inserted allowance '" + allowanceName + "' for " + employeeId);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Payroll Calculation - Single Employee with Regular Hours and Allowances")
    void testCalculatePayroll_SingleEmployeeRegularHoursAndAllowances() throws SQLException {
        String periodName = "AUGUST 1-31, 2025-TEST";
        LocalDate startDate = LocalDate.of(2025, 8, 1);
        LocalDate endDate = LocalDate.of(2025, 8, 31);
        int employeeId = SINGLE_TEST_EMPLOYEE_ID;

        insertAttendance(employeeId, LocalDate.of(2025, 8, 5), "08:00:00", "17:00:00");
        insertAttendance(employeeId, LocalDate.of(2025, 8, 6), "08:00:00", "17:00:00");
        insertAllowance(employeeId, "Rice Subsidy", 1500.0, startDate);
        
        conn.commit();

        payrollDAO.calculatePayroll(periodName, startDate, endDate);

        String checkPayslipSql = "SELECT GrossPay FROM payslip WHERE EmployeeID = ? AND PayPeriodID IN (SELECT PayPeriodID FROM payperiod WHERE StartDate = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipSql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(startDate));
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Payslip should be generated.");
            assertTrue(rs.getDouble("GrossPay") > 0, "GrossPay should be calculated.");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test Payroll Calculation - Period Already Processed")
    void testCalculatePayroll_PeriodAlreadyProcessed() throws SQLException {
        String periodName = "SEPTEMBER 1-30, 2025-TEST";
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);

        String insertSql = "INSERT INTO payperiod (StartDate, EndDate, PeriodName, IsProcessed, PaymentDate) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            pstmt.setString(3, periodName);
            pstmt.setBoolean(4, true); // Mark as processed
            pstmt.setDate(5, Date.valueOf(endDate.plusDays(5)));
            pstmt.executeUpdate();
        }
        conn.commit();

        payrollDAO.calculatePayroll(periodName, startDate, endDate);

        String checkSql = "SELECT IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "PayPeriod should still exist.");
            assertTrue(rs.getBoolean("IsProcessed"), "PayPeriod should remain processed.");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test Payroll Calculation - Single Employee with Zero Hours")
    void testCalculatePayroll_SingleEmployeeWithZeroHours() throws SQLException {
        String periodName = "OCTOBER 1-31, 2025-TEST";
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 31);
        int employeeId = SINGLE_TEST_EMPLOYEE_ID;


        payrollDAO.calculatePayroll(periodName, startDate, endDate);

        String checkPayPeriodSql = "SELECT PayPeriodID, IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
        int payPeriodId = -1;
        try (PreparedStatement pstmt = conn.prepareStatement(checkPayPeriodSql)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "PayPeriod should be inserted even with no employees to pay.");
            payPeriodId = rs.getInt("PayPeriodID");
            assertTrue(rs.getBoolean("IsProcessed"), "PayPeriod should be marked as processed.");
        }

        String checkPayslipSql = "SELECT COUNT(*) FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipSql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next() && rs.getInt(1) == 0, "No payslip should be generated for an employee with zero hours.");
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test Payroll Calculation - Single Employee with Overtime")
    void testCalculatePayroll_SingleEmployeeOvertimeHours() throws SQLException {
        String periodName = "NOVEMBER 1-30, 2025-TEST";
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 11, 30);
        int employeeId = SINGLE_TEST_EMPLOYEE_ID;

        insertAttendance(employeeId, LocalDate.of(2025, 11, 3), "08:00:00", "18:00:00"); // 1 hour OT
        insertAttendance(employeeId, LocalDate.of(2025, 11, 4), "08:00:00", "19:00:00"); // 2 hours OT
        conn.commit();

        payrollDAO.calculatePayroll(periodName, startDate, endDate);

        String checkPayslipSql = "SELECT TotalOvertimeHours FROM payslip WHERE EmployeeID = ? AND PayPeriodID IN (SELECT PayPeriodID FROM payperiod WHERE StartDate = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipSql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setDate(2, Date.valueOf(startDate));
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next(), "Payslip should be generated for employee with overtime.");
            assertTrue(rs.getDouble("TotalOvertimeHours") > 0, "TotalOvertimeHours should be calculated and greater than zero.");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test Payroll Calculation - Existing Partial Data Cleared")
    void testCalculatePayroll_ExistingPartialDataCleared() throws SQLException {
        String periodName = "DECEMBER 1-31, 2025-TEST";
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        int employeeId = SINGLE_TEST_EMPLOYEE_ID;

        int payPeriodId;
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO payperiod (StartDate, EndDate, PeriodName, IsProcessed) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            pstmt.setString(3, periodName);
            pstmt.setBoolean(4, false); // Unprocessed
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            rs.next();
            payPeriodId = rs.getInt(1);
        }

        int partialPayslipId;
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO payslip (EmployeeID, PayPeriodID, GrossPay) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, payPeriodId);
            pstmt.setDouble(3, 1.00); // Dummy data
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            rs.next();
            partialPayslipId = rs.getInt(1);
        }

        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO payslipdetail (PayslipID, Description, Amount, Type) VALUES (?, 'Dummy Earning', 1.00, 'Earning')")) {
            pstmt.setInt(1, partialPayslipId);
            pstmt.executeUpdate();
        }
        
        insertAttendance(employeeId, LocalDate.of(2025, 12, 2), "08:00:00", "17:00:00");
        
        conn.commit();

        payrollDAO.calculatePayroll(periodName, startDate, endDate);

        String checkOldPayslipSql = "SELECT COUNT(*) FROM payslip WHERE PayslipID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkOldPayslipSql)) {
            pstmt.setInt(1, partialPayslipId);
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next() && rs.getInt(1) == 0, "Old dummy payslip should be deleted.");
        }

        String checkNewPayslipDetailSql = "SELECT COUNT(*) FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(checkNewPayslipDetailSql)) {
            pstmt.setInt(1, employeeId);
            pstmt.setInt(2, payPeriodId);
            ResultSet rs = pstmt.executeQuery();
            assertTrue(rs.next() && rs.getInt(1) > 0, "New payslip details should be generated.");
        }
    }
}

//FOR MANUAL INPUT TESTING

//package Testing;
//
//import dao.PayrollDAO;
//import dao.EmployeeDAO;
//import dao.SalaryDAO;
//import dao.CredentialDAO; // Needed to find UserID by Username
//import data.DBConnection;
//import org.junit.jupiter.api.*;
//import java.sql.*;
//import java.time.LocalDate;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID; // Still used for unique data within certain tests, but not for base IDs
//
//import static org.junit.jupiter.api.Assertions.*;
//import util.TestDBUtils; // Import your TestDBUtils
//
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//public class TestPayroll {
//
//    private static Connection conn;
//    private PayrollDAO payrollDAO;
//    private static EmployeeDAO employeeDAO;
//    private static SalaryDAO salaryDAO;
//    private static CredentialDAO credentialDAO; // New: to get UserID
//
//    // --- MANUALLY PRE-POPULATED EMPLOYEE DETAILS (for lookup) ---
//    // You MUST ensure these details match the single employee you manually insert.
//    private static final String MANUAL_TEST_USERNAME = "single_test_user";
//    private static final String MANUAL_TEST_EMPLOYEE_LASTNAME = "Test"; // Match the last name you inserted
//    private static final String MANUAL_TEST_EMPLOYEE_FIRSTNAME = "One"; // Match the first name you inserted
//
//    // This will store the actual AUTO_INCREMENTED EmployeeID retrieved from DB
//    private static int SINGLE_TEST_EMPLOYEE_ID = -1;
//
//    // Setup runs once before all tests
//    @BeforeAll
//    public static void setUpAll() throws SQLException {
//        conn = DBConnection.getConnection(); // Get a connection for @BeforeAll and @AfterAll
//        conn.setAutoCommit(false); // Manage transaction manually for setup/teardown
//
//        System.out.println("--- TestPayroll: Database connection established. AutoCommit set to false. ---");
//
//        employeeDAO = new EmployeeDAO();
//        salaryDAO = new SalaryDAO();
//        credentialDAO = new CredentialDAO(); // Initialize CredentialDAO
//
//        // --- Retrieve the dynamically generated EmployeeID of the manually inserted test user ---
//        String userIdString = credentialDAO.getUserIdByUsername(MANUAL_TEST_USERNAME);
//        if (userIdString == null) {
//            throw new SQLException("Manual test user '" + MANUAL_TEST_USERNAME + "' not found. Please ensure it is pre-populated in the database and its username is exact.");
//        }
//        int userId = Integer.parseInt(userIdString);
//
//        // Now find the EmployeeID using the UserID, Last Name, and First Name
//        String getEmployeeIdSql = "SELECT EmployeeID FROM employee WHERE UserID = ? AND LastName = ? AND FirstName = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(getEmployeeIdSql)) {
//            pstmt.setInt(1, userId);
//            pstmt.setString(2, MANUAL_TEST_EMPLOYEE_LASTNAME);
//            pstmt.setString(3, MANUAL_TEST_EMPLOYEE_FIRSTNAME);
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                SINGLE_TEST_EMPLOYEE_ID = rs.getInt("EmployeeID");
//                System.out.println("Retrieved manual test employee ID: " + SINGLE_TEST_EMPLOYEE_ID);
//            } else {
//                throw new SQLException("Manual test employee record not found for UserID " + userId + " and name '" + MANUAL_TEST_EMPLOYEE_FIRSTNAME + " " + MANUAL_TEST_EMPLOYEE_LASTNAME + "'. Please ensure employee details match.");
//            }
//        }
//
//        if (SINGLE_TEST_EMPLOYEE_ID == -1) {
//            throw new SQLException("Failed to retrieve the ID for the single manually pre-populated test employee. Test cannot proceed.");
//        }
//
//        // Commit the initial state (no actual DML in this specific block, but ensures connection is clear)
//        conn.commit();
//    }
//
//    // Runs before each test method
//    @BeforeEach
//    public void setUp() {
//        payrollDAO = new PayrollDAO();
//        System.out.println("\n--- Starting new test method ---");
//    }
//
//    // Runs after each test method
//    @AfterEach
//    public void tearDown() throws SQLException {
//        // Rollback all changes made during the test to keep the DB clean for the next test.
//        // This includes attendance, payslips, deductions etc. inserted by the test method.
//        if (conn != null) {
//            conn.rollback();
//            System.out.println("--- TestPayroll: Transaction rolled back for test method. ---");
//        }
//    }
//
//    // Teardown runs once after all tests
//    @AfterAll
//    public static void tearDownAll() throws SQLException {
//        if (conn != null && !conn.isClosed()) {
//            // --- Cleanup for the single manually pre-populated test employee ---
//            // This is a hard delete of data that might have been created by previous failed tests.
//            if (SINGLE_TEST_EMPLOYEE_ID != -1) {
//                deleteTestEmployeeFully(SINGLE_TEST_EMPLOYEE_ID);
//            }
//
//            // --- Reset AUTO_INCREMENTs ---
//            // These operations might auto-commit. Calling on the main 'conn'.
//            TestDBUtils.resetAutoIncrement(conn, "user", "UserID");
//            TestDBUtils.resetAutoIncrement(conn, "address", "AddressID");
//            TestDBUtils.resetAutoIncrement(conn, "employee", "EmployeeID");
//            TestDBUtils.resetAutoIncrement(conn, "salary", "SalaryID");
//            TestDBUtils.resetAutoIncrement(conn, "payperiod", "PayPeriodID");
//            TestDBUtils.resetAutoIncrement(conn, "payslip", "PayslipID");
//            TestDBUtils.resetAutoIncrement(conn, "payslipdetail", "PayslipDetailID");
//            TestDBUtils.resetAutoIncrement(conn, "deduction", "DeductionID");
//            TestDBUtils.resetAutoIncrement(conn, "taxcomputation", "TaxComputationID");
//            TestDBUtils.resetAutoIncrement(conn, "allowancetype", "AllowanceTypeID");
//            TestDBUtils.resetAutoIncrement(conn, "allowance", "AllowanceID");
//            TestDBUtils.resetAutoIncrement(conn, "attendance", "AttendanceID");
//            TestDBUtils.resetAutoIncrement(conn, "leaverequest", "LeaveRequestID");
//            TestDBUtils.resetAutoIncrement(conn, "leavebalance", "LeaveBalanceID");
//            TestDBUtils.resetAutoIncrement(conn, "userrole", "UserRoleID");
//            TestDBUtils.resetAutoIncrement(conn, "role", "RoleID"); // Only if you create roles dynamically
//
//            conn.close(); // Close the connection
//            System.out.println("--- TestPayroll: Database connection closed and AUTO_INCREMENTs reset. ---");
//        }
//    }
//
//    /**
//     * Performs a hard delete of a test employee and all associated records.
//     * This is used for cleanup of dynamically generated test data in @AfterAll.
//     * Disables FK checks for robust cascade deletion.
//     * @param generatedEmployeeId The auto-generated EmployeeID to delete.
//     */
//    private static void deleteTestEmployeeFully(int generatedEmployeeId) throws SQLException {
//        if (generatedEmployeeId == -1) return;
//
//        // Find associated UserID and AddressID first for complete cleanup
//        int userId = -1;
//        int addressId = -1;
//        try (PreparedStatement pstmt = conn.prepareStatement("SELECT UserID, AddressID FROM employee WHERE EmployeeID = ?")) {
//            pstmt.setInt(1, generatedEmployeeId);
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                userId = rs.getInt("UserID");
//                addressId = rs.getInt("AddressID");
//            }
//        }
//
//        // Disable foreign key checks temporarily
//        try (Statement stmt = conn.createStatement()) {
//            stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
//        }
//
//        try {
//            // Delete in reverse order of foreign key dependencies.
//            // These are hard deletes to ensure clean state after tests.
//            try (PreparedStatement pstmtAttendance = conn.prepareStatement("DELETE FROM attendance WHERE EmployeeID = ?");
//                 PreparedStatement pstmtPayslipDetail = conn.prepareStatement("DELETE FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ?)");
//                 PreparedStatement pstmtTaxComputation = conn.prepareStatement("DELETE FROM taxcomputation WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ?)");
//                 PreparedStatement pstmtDeduction = conn.prepareStatement("DELETE FROM deduction WHERE EmployeeID = ?");
//                 PreparedStatement pstmtPayslip = conn.prepareStatement("DELETE FROM payslip WHERE EmployeeID = ?");
//                 PreparedStatement pstmtPayPeriod = conn.prepareStatement("DELETE FROM payperiod");
//                 PreparedStatement pstmtAllowance = conn.prepareStatement("DELETE FROM allowance WHERE EmployeeID = ?");
//                 PreparedStatement pstmtLeaveBalance = conn.prepareStatement("DELETE FROM leavebalance WHERE EmployeeID = ?");
//                 PreparedStatement pstmtLeaveRequest = conn.prepareStatement("DELETE FROM leaverequest WHERE EmployeeID = ?");
//                 PreparedStatement pstmtSalary = conn.prepareStatement("DELETE FROM salary WHERE EmployeeID = ?");
//                 PreparedStatement pstmtEmployee = conn.prepareStatement("DELETE FROM employee WHERE EmployeeID = ?");
//                 PreparedStatement pstmtUserRole = conn.prepareStatement("DELETE FROM userrole WHERE UserID = ?");
//                 PreparedStatement pstmtUser = conn.prepareStatement("DELETE FROM user WHERE UserID = ?");
//                 PreparedStatement pstmtAddress = conn.prepareStatement("DELETE FROM address WHERE AddressID = ?"))
//            {
//                pstmtAttendance.setInt(1, generatedEmployeeId); pstmtAttendance.executeUpdate();
//                pstmtPayslipDetail.setInt(1, generatedEmployeeId); pstmtPayslipDetail.executeUpdate();
//                pstmtTaxComputation.setInt(1, generatedEmployeeId); pstmtTaxComputation.executeUpdate();
//                pstmtDeduction.setInt(1, generatedEmployeeId); pstmtDeduction.executeUpdate();
//                pstmtPayslip.setInt(1, generatedEmployeeId); pstmtPayslip.executeUpdate();
//                pstmtPayPeriod.executeUpdate();
//                pstmtAllowance.setInt(1, generatedEmployeeId); pstmtAllowance.executeUpdate();
//                pstmtLeaveBalance.setInt(1, generatedEmployeeId); pstmtLeaveBalance.executeUpdate();
//                pstmtLeaveRequest.setInt(1, generatedEmployeeId); pstmtLeaveRequest.executeUpdate();
//                pstmtSalary.setInt(1, generatedEmployeeId); pstmtSalary.executeUpdate();
//                pstmtEmployee.setInt(1, generatedEmployeeId); pstmtEmployee.executeUpdate();
//
//                if (userId != -1) {
//                    pstmtUserRole.setInt(1, userId); pstmtUserRole.executeUpdate();
//                    pstmtUser.setInt(1, userId); pstmtUser.executeUpdate();
//                }
//                if (addressId != -1) {
//                    pstmtAddress.setInt(1, addressId); pstmtAddress.executeUpdate();
//                }
//            }
//            conn.commit(); // Commit deletions
//            System.out.println("Hard cleaned up test employee with EmployeeID: " + generatedEmployeeId);
//        } catch (SQLException e) {
//            System.err.println("Error hard cleaning up test employee with EmployeeID " + generatedEmployeeId + ": " + e.getMessage());
//            conn.rollback();
//            throw e;
//        } finally {
//            try (Statement stmt = conn.createStatement()) { stmt.execute("SET FOREIGN_KEY_CHECKS = 1;"); } // Re-enable
//        }
//    }
//
//    /**
//     * Helper to insert attendance for a test.
//     * HoursWorked and OvertimeHours are GENERATED columns and not inserted directly.
//     * @param employeeId The EmployeeID.
//     * @param date The date of attendance.
//     * @param logInTime The login time (e.g., "08:00:00").
//     * @param logOutTime The logout time (e.g., "17:00:00").
//     * @throws SQLException if a database error occurs.
//     */
//    private void insertAttendance(int employeeId, LocalDate date, String logInTime, String logOutTime) throws SQLException {
//        // SQL omits HoursWorked and OvertimeHours
//        String sql = "INSERT INTO attendance (EmployeeID, Date, LogInTime, LogOutTime) VALUES (?, ?, ?, ?)";
//        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setDate(2, Date.valueOf(date));
//            pstmt.setString(3, logInTime);
//            pstmt.setString(4, logOutTime);
//            pstmt.executeUpdate();
//            System.out.println("Inserted attendance for " + employeeId + " on " + date + ": " + logInTime + " to " + logOutTime + " (HoursWorked & OvertimeHours generated).");
//        }
//    }
//
//    /**
//     * Helper to insert allowance for a test.
//     * @param employeeId The EmployeeID.
//     * @param allowanceName The name of the allowance.
//     * @param amount The allowance amount.
//     * @param effectiveDate The effective date of the allowance.
//     * @throws SQLException if a database error occurs.
//     */
//    private void insertAllowance(int employeeId, String allowanceName, double amount, LocalDate effectiveDate) throws SQLException {
//        // Ensure AllowanceType exists or create it if not
//        int allowanceTypeId = -1;
//        String checkTypeSql = "SELECT AllowanceTypeID FROM allowancetype WHERE AllowanceName = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkTypeSql)) {
//            pstmt.setString(1, allowanceName);
//            ResultSet rs = pstmt.executeQuery();
//            if (rs.next()) {
//                allowanceTypeId = rs.getInt("AllowanceTypeID");
//            } else {
//                String insertTypeSql = "INSERT INTO allowancetype (AllowanceName) VALUES (?)";
//                try (PreparedStatement insertPstmt = conn.prepareStatement(insertTypeSql, Statement.RETURN_GENERATED_KEYS)) {
//                    insertPstmt.setString(1, allowanceName);
//                    insertPstmt.executeUpdate();
//                    ResultSet generatedKeys = insertPstmt.getGeneratedKeys();
//                    if (generatedKeys.next()) {
//                        allowanceTypeId = generatedKeys.getInt(1);
//                    }
//                }
//            }
//        }
//
//        if (allowanceTypeId != -1) {
//            String sql = "INSERT INTO allowance (EmployeeID, AllowanceTypeID, Amount, EffectiveDate) VALUES (?, ?, ?, ?)";
//            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//                pstmt.setInt(1, employeeId);
//                pstmt.setInt(2, allowanceTypeId);
//                pstmt.setDouble(3, amount);
//                pstmt.setDate(4, Date.valueOf(effectiveDate));
//                pstmt.executeUpdate();
//                System.out.println("Inserted allowance '" + allowanceName + "' for " + employeeId + ": " + amount);
//            }
//        }
//    }
//
//
//    @Test
//    @Order(1)
//    @DisplayName("Test Payroll Calculation - Single Employee with Regular Hours and Allowances")
//    void testCalculatePayroll_SingleEmployeeRegularHoursAndAllowances() throws SQLException {
//        String periodName = "AUGUST 1-31, 2025";
//        LocalDate startDate = LocalDate.of(2025, 8, 1);
//        LocalDate endDate = LocalDate.of(2025, 8, 31);
//        int employeeId = SINGLE_TEST_EMPLOYEE_ID; // Use the retrieved ID
//
//        // Insert attendance for the employee with times that generate 8 hours work
//        insertAttendance(employeeId, LocalDate.of(2025, 8, 5), "08:00:00", "17:00:00");
//        insertAttendance(employeeId, LocalDate.of(2025, 8, 6), "08:00:00", "17:00:00");
//        insertAttendance(employeeId, LocalDate.of(2025, 8, 7), "08:00:00", "17:00:00");
//        insertAttendance(employeeId, LocalDate.of(2025, 8, 8), "08:00:00", "17:00:00");
//        insertAttendance(employeeId, LocalDate.of(2025, 8, 9), "08:00:00", "17:00:00"); // Total 5 days
//
//        // Insert some allowances
//        insertAllowance(employeeId, "Rice Subsidy", 1500.0, startDate);
//        insertAllowance(employeeId, "Phone Allowance", 500.0, startDate);
//        insertAllowance(employeeId, "Clothing Allowance", 500.0, startDate);
//        
//        conn.commit();
//
//        // Call the payroll calculation
//        payrollDAO.calculatePayroll(periodName, startDate, endDate);
//
//        // --- Verification ---
//        // 1. Verify PayPeriod was inserted and marked as processed
//        String checkPayPeriodSql = "SELECT PayPeriodID, IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
//        int payPeriodId = -1;
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayPeriodSql)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next(), "PayPeriod should be inserted.");
//                payPeriodId = rs.getInt("PayPeriodID");
//                assertTrue(rs.getBoolean("IsProcessed"), "PayPeriod should be marked as processed.");
//            }
//        }
//        assertTrue(payPeriodId != -1, "PayPeriodID should be retrieved.");
//
//        // 2. Verify Payslip was generated for the employee
//        String checkPayslipSql = "SELECT GrossPay, TotalDeductions, TotalAllowances, NetPay, HoursWorked, TotalOvertimeHours FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setInt(2, payPeriodId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next(), "Payslip should be generated for employee " + employeeId);
//                // Fetch employee's basic salary and hourly rate from DB for verification
//                double basicSalary = 0.0;
//                double hourlyRate = 0.0; // This will be the generated value from DB
//                try (PreparedStatement empStmt = conn.prepareStatement("SELECT BasicSalary, HourlyRate FROM salary WHERE EmployeeID = ?")) {
//                    empStmt.setInt(1, employeeId);
//                    ResultSet empRs = empStmt.executeQuery();
//                    if (empRs.next()) {
//                        basicSalary = empRs.getDouble("BasicSalary");
//                        hourlyRate = empRs.getDouble("HourlyRate"); // Get generated hourly rate
//                    }
//                }
//                // Expected values are based on DB's generated columns (8:00-17:00 typically 8 hours worked, 0 overtime)
//                double expectedHoursWorked = 40.0; // 5 days * 8 hours/day
//                double expectedOvertimeHours = 0.0;
//                // Recalculate expected gross pay using the hourly rate *generated by the DB*
//                double expectedGrossPay = expectedHoursWorked * hourlyRate;
//                double expectedTotalAllowances = 1500.0 + 500.0 + 500.0; // Rice Subsidy + Phone Allowance
//                assertTrue(rs.getDouble("GrossPay") > 0.0, "GrossPay should be calculated.");
//                assertEquals(expectedTotalAllowances, rs.getDouble("TotalAllowances"), 0.01, "TotalAllowances should match.");
//                assertTrue(rs.getDouble("TotalDeductions") > 0.0, "TotalDeductions should be calculated.");
//                assertTrue(rs.getDouble("NetPay") > 0.0, "NetPay should be calculated.");
//                assertEquals(expectedHoursWorked, rs.getDouble("HoursWorked"), 0.01, "HoursWorked should match DB's generated value.");
//                assertEquals(expectedOvertimeHours, rs.getDouble("TotalOvertimeHours"), 0.01, "OvertimeHours should match DB's generated value.");
//            }
//        }
//
//        // 3. Verify PayslipDetails, Deductions, TaxComputation are populated (check for existence)
//        String checkPayslipDetailSql = "SELECT COUNT(*) FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?)";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipDetailSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setInt(2, payPeriodId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) > 0, "Payslip details should be populated.");
//            }
//        }
//
//        String checkDeductionSql = "SELECT COUNT(*) FROM deduction WHERE EmployeeID = ? AND PayPeriodStartDate = ? AND PayperiodEndDate = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkDeductionSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setDate(2, Date.valueOf(startDate));
//            pstmt.setDate(3, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) > 0, "Deductions should be populated.");
//            }
//        }
//
//        String checkTaxComputationSql = "SELECT COUNT(*) FROM taxcomputation WHERE EmployeeID = ? AND TaxPeriodStartDate = ? AND TaxPeriodEndDate = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkTaxComputationSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setDate(2, Date.valueOf(startDate));
//            pstmt.setDate(3, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) > 0, "Tax computation should be populated (if applicable).");
//            }
//        }
//    }
//
//    @Test
//    @Order(2)
//    @DisplayName("Test Payroll Calculation - Period Already Processed (for single employee)")
//    void testCalculatePayroll_PeriodAlreadyProcessed() throws SQLException {
//        String periodName = "SEPTEMBER 1-30, 2025";
//        LocalDate startDate = LocalDate.of(2025, 9, 1);
//        LocalDate endDate = LocalDate.of(2025, 9, 30);
//
//        // Manually insert a processed pay period
//        String insertSql = "INSERT INTO payperiod (StartDate, EndDate, PeriodName, IsProcessed, PaymentDate) VALUES (?, ?, ?, ?, ?)";
//        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            pstmt.setString(3, periodName);
//            pstmt.setBoolean(4, true); // Mark as processed
//            pstmt.setDate(5, Date.valueOf(endDate.plusDays(5)));
//            pstmt.executeUpdate();
//            conn.commit(); // Commit this insert so the DAO sees it
//            System.out.println("Manually inserted a processed pay period for test.");
//        }
//
//        // Call the payroll calculation (should be skipped by DAO)
//        payrollDAO.calculatePayroll(periodName, startDate, endDate);
//
//        // Verify that IsProcessed is still true and no new data was added
//        String checkSql = "SELECT IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next(), "PayPeriod should still exist.");
//                assertTrue(rs.getBoolean("IsProcessed"), "PayPeriod should remain processed.");
//            }
//        }
//
//        // Verify no payslips were created for this period (assuming no prior payslips for this period before manual insert)
//        String checkPayslipCountSql = "SELECT COUNT(*) FROM payslip WHERE PayPeriodID IN (SELECT PayPeriodID FROM payperiod WHERE StartDate = ? AND EndDate = ?)";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipCountSql)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) == 0, "No new payslips should be generated.");
//            }
//        }
//    }
//
//    @Test
//    @Order(3)
//    @DisplayName("Test Payroll Calculation - Single Employee with Zero Hours Worked")
//    void testCalculatePayroll_SingleEmployeeWithZeroHours() throws SQLException {
//        String periodName = "OCTOBER 1-31, 2025";
//        LocalDate startDate = LocalDate.of(2025, 10, 1);
//        LocalDate endDate = LocalDate.of(2025, 10, 31);
//        int employeeId = SINGLE_TEST_EMPLOYEE_ID; // Use the retrieved ID
//
//        // Ensure no attendance for this employee in this period
//        // (This is implicitly handled by not calling insertAttendance for this employee/period)
//
//        // Call the payroll calculation
//        payrollDAO.calculatePayroll(periodName, startDate, endDate);
//
//        // --- Verification ---
//        // 1. Verify PayPeriod was inserted and marked as processed (it should be, even if no employees have hours)
//        String checkPayPeriodSql = "SELECT PayPeriodID, IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
//        int payPeriodId = -1;
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayPeriodSql)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next(), "PayPeriod should be inserted.");
//                payPeriodId = rs.getInt("PayPeriodID");
//                assertTrue(rs.getBoolean("IsProcessed"), "PayPeriod should be marked as processed.");
//            }
//        }
//        assertTrue(payPeriodId != -1, "PayPeriodID should be retrieved.");
//
//        // 2. Verify NO Payslip was generated for the employee with zero hours
//        String checkPayslipSql = "SELECT COUNT(*) FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setInt(2, payPeriodId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) == 0, "No payslip should be generated for employee " + employeeId + " with zero hours.");
//            }
//        }
//    }
//
//    @Test
//    @Order(4)
//    @DisplayName("Test Payroll Calculation - Single Employee with Overtime Hours Included")
//    void testCalculatePayroll_SingleEmployeeOvertimeHours() throws SQLException {
//        String periodName = "NOVEMBER 1-30, 2025";
//        LocalDate startDate = LocalDate.of(2025, 11, 1);
//        LocalDate endDate = LocalDate.of(2025, 11, 30);
//        int employeeId = SINGLE_TEST_EMPLOYEE_ID; // Use the retrieved ID
//
//        // Insert attendance with times that generate regular and overtime hours
//        insertAttendance(employeeId, LocalDate.of(2025, 11, 3), "08:00:00", "18:00:00"); // Example: 1 hour OT (08:00-18:00, assuming 1hr break, means 9 hrs worked, 1 OT)
//        insertAttendance(employeeId, LocalDate.of(2025, 11, 4), "08:00:00", "19:00:00"); // Example: 2 hours OT (08:00-19:00, assuming 1hr break, means 10 hrs worked, 2 OT)
//
//        conn.commit();
//        
//        // Call the payroll calculation
//        payrollDAO.calculatePayroll(periodName, startDate, endDate);
//
//        // --- Verification ---
//        // 1. Verify PayPeriod was inserted and marked as processed
//        String checkPayPeriodSql = "SELECT PayPeriodID, IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
//        int payPeriodId = -1;
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayPeriodSql)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next(), "PayPeriod should be inserted.");
//                payPeriodId = rs.getInt("PayPeriodID");
//                assertTrue(rs.getBoolean("IsProcessed"), "PayPeriod should be marked as processed.");
//            }
//        }
//        assertTrue(payPeriodId != -1, "PayPeriodID should be retrieved.");
//
//        // 2. Verify Payslip includes overtime hours
//        String checkPayslipSql = "SELECT GrossPay, TotalOvertimeHours FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayslipSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setInt(2, payPeriodId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next(), "Payslip should be generated for employee " + employeeId);
//                // EXPECTED OVERTIME: Sum of OT generated by the times above (1 + 2 = 3 hours).
//                // This relies on your DB's generated column correctly calculating these.
//                double expectedTotalOvertimeHours = 3.0; // Expected based on example times
//                assertEquals(expectedTotalOvertimeHours, rs.getDouble("TotalOvertimeHours"), 0.01, "TotalOvertimeHours should match DB's generated value.");
//                // Verify an overtime pay detail exists in payslipdetail
//                String checkOvertimeDetailSql = "SELECT Amount FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?) AND Description = 'Overtime Pay'";
//                try (PreparedStatement pstmtDetail = conn.prepareStatement(checkOvertimeDetailSql)) {
//                    pstmtDetail.setInt(1, employeeId);
//                    pstmtDetail.setInt(2, payPeriodId);
//                    try (ResultSet detailRs = pstmtDetail.executeQuery()) {
//                        if (expectedTotalOvertimeHours > 0.0) {
//                            assertTrue(detailRs.next(), "Overtime Pay detail should exist.");
//                            assertTrue(detailRs.getDouble("Amount") > 0.0, "Overtime Pay amount should be greater than 0.");
//                        } else {
//                            assertFalse(detailRs.next(), "Overtime Pay detail should NOT exist if no overtime hours.");
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Test
//    @Order(5)
//    @DisplayName("Test Payroll Calculation - Existing Partial Data Cleared (for single employee)")
//    void testCalculatePayroll_ExistingPartialDataCleared() throws SQLException {
//        String periodName = "DECEMBER 1-31, 2025";
//        LocalDate startDate = LocalDate.of(2025, 12, 1);
//        LocalDate endDate = LocalDate.of(2025, 12, 31);
//        int employeeId = SINGLE_TEST_EMPLOYEE_ID; // Use retrieved ID
//
//        // Manually insert a payperiod (unprocessed) and some partial payslip data
//        String insertPayPeriodSql = "INSERT INTO payperiod (StartDate, EndDate, PeriodName, IsProcessed, PaymentDate) VALUES (?, ?, ?, ?, ?)";
//        int payPeriodId;
//        try (PreparedStatement pstmt = conn.prepareStatement(insertPayPeriodSql, Statement.RETURN_GENERATED_KEYS)) {
//            pstmt.setDate(1, Date.valueOf(startDate));
//            pstmt.setDate(2, Date.valueOf(endDate));
//            pstmt.setString(3, periodName);
//            pstmt.setBoolean(4, false); // Unprocessed
//            pstmt.setDate(5, Date.valueOf(endDate.plusDays(5)));
//            pstmt.executeUpdate();
//            try (ResultSet rs = pstmt.getGeneratedKeys()) {
//                assertTrue(rs.next());
//                payPeriodId = rs.getInt(1);
//            }
//            System.out.println("Manually inserted an unprocessed pay period with ID: " + payPeriodId);
//        }
//
//        // Insert a partial payslip for the employee
//        // IMPORTANT: If 'HoursWorked' and 'TotalOvertimeHours' are generated in 'payslip' table,
//        // you must omit them from this INSERT statement. Assuming they are direct inputs here for dummy data.
//        String insertPartialPayslipSql = "INSERT INTO payslip (EmployeeID, PayPeriodID, GrossPay, TotalDeductions, TotalAllowances, NetPay, HoursWorked, TotalOvertimeHours, GenerateDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
//        int partialPayslipId;
//        try (PreparedStatement pstmt = conn.prepareStatement(insertPartialPayslipSql, Statement.RETURN_GENERATED_KEYS)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setInt(2, payPeriodId);
//            pstmt.setDouble(3, 1000.0); // Dummy data
//            pstmt.setDouble(4, 50.0);
//            pstmt.setDouble(5, 0.0);
//            pstmt.setDouble(6, 950.0);
//            pstmt.setDouble(7, 8.0); // Dummy HoursWorked
//            pstmt.setDouble(8, 0.0); // Dummy TotalOvertimeHours
//            pstmt.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
//            pstmt.executeUpdate();
//            try (ResultSet rs = pstmt.getGeneratedKeys()) {
//                assertTrue(rs.next());
//                partialPayslipId = rs.getInt(1);
//            }
//            System.out.println("Manually inserted a partial payslip with ID: " + partialPayslipId);
//        }
//
//        // Insert some dummy payslip details for the partial payslip
//        String insertDummyDetailSql = "INSERT INTO payslipdetail (PayslipID, Description, Amount, Type) VALUES (?, ?, ?, ?)";
//        try (PreparedStatement pstmt = conn.prepareStatement(insertDummyDetailSql)) {
//            pstmt.setInt(1, partialPayslipId);
//            pstmt.setString(2, "Dummy Earning");
//            pstmt.setDouble(3, 1000.0);
//            pstmt.setString(4, "Earning");
//            pstmt.executeUpdate();
//            System.out.println("Manually inserted dummy payslip detail.");
//        }
//        
//        // Insert attendance for the employee (actual data for calculation)
//        insertAttendance(employeeId, LocalDate.of(2025, 12, 2), "08:00:00", "17:00:00");
//        
//        conn.commit();
//
//        // Call the payroll calculation (should clear dummy data and re-calculate)
//        payrollDAO.calculatePayroll(periodName, startDate, endDate);
//
//        // --- Verification ---
//        // 1. Verify that the payperiod is now processed
//        String checkPayPeriodProcessedSql = "SELECT IsProcessed FROM payperiod WHERE PayPeriodID = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkPayPeriodProcessedSql)) {
//            pstmt.setInt(1, payPeriodId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getBoolean("IsProcessed"), "PayPeriod should be marked as processed after recalculation.");
//            }
//        }
//
//        // 2. Verify that old payslip details are gone and new ones exist
//        String checkOldPayslipDetailSql = "SELECT COUNT(*) FROM payslipdetail WHERE PayslipID = ?";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkOldPayslipDetailSql)) {
//            pstmt.setInt(1, partialPayslipId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) == 0, "Old payslip details should be deleted.");
//            }
//        }
//
//        String checkNewPayslipDetailSql = "SELECT COUNT(*) FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE EmployeeID = ? AND PayPeriodID = ?)";
//        try (PreparedStatement pstmt = conn.prepareStatement(checkNewPayslipDetailSql)) {
//            pstmt.setInt(1, employeeId);
//            pstmt.setInt(2, payPeriodId);
//            try (ResultSet rs = pstmt.executeQuery()) {
//                assertTrue(rs.next() && rs.getInt(1) > 0, "New payslip details should be generated.");
//            }
//        }
//    }
//}