package model;

import dao.CredentialDAO;
import dao.EmployeeDAO;
import gui.HomePage;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Employee implements DataReader {

    private static final Logger LOGGER = Logger.getLogger(Employee.class.getName());

    private String employeeNo;
    private String employeeFN;
    private String employeeLN;
    private String employeeAddress;
    private Date employeeDOB;
    private String employeePhoneNumber;
    private String employeeSSS;
    private String employeePhilHealth;
    private String employeeTIN;
    private String employeePagIbig;
    private String employeeStatus;
    private String employeePosition;
    private String employeeSupervisor;
    private double basicSalary; 
    private String userID; // This is the EmployeeID from the 'user' table
    private HomePage homePage;
    DecimalFormat df = new DecimalFormat("#,##0.00");

    // Constructor for login/authentication context — used to identify user by EmployeeID only
    public Employee(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    // Constructor for creating new employees (AddEmployee)
    public Employee(String userID, String employeeFN, String employeeLN, String employeeAddress, Date employeeDOB, String employeePhoneNumber, String employeeSSS, String employeePhilHealth, String employeeTIN, String employeePagIbig, String employeeStatus, String employeePosition, String employeeSupervisor, double basicSalary) {
        this.userID = userID; // This is the EmployeeID
        this.employeeNo = userID; // Set employeeNo to userID for consistency
        this.employeeFN = employeeFN;
        this.employeeLN = employeeLN;
        this.employeeAddress = employeeAddress;
        this.employeeDOB = employeeDOB;
        this.employeePhoneNumber = employeePhoneNumber;
        this.employeeSSS = employeeSSS;
        this.employeePhilHealth = employeePhilHealth;
        this.employeeTIN = employeeTIN;
        this.employeePagIbig = employeePagIbig;
        this.employeeStatus = employeeStatus;
        this.employeePosition = employeePosition;
        this.employeeSupervisor = employeeSupervisor;
        this.basicSalary = basicSalary;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getEmployeeFN() {
        return employeeFN;
    }

    public String getEmployeeLN() {
        return employeeLN;
    }

    public Date getEmployeeDOB() {
        return employeeDOB;
    }

    public String getEmployeeAddress() {
        return employeeAddress;
    }

    public String getEmployeePhoneNumber() {
        return employeePhoneNumber;
    }

    public String getEmployeeSSS() {
        return employeeSSS;
    }

    public String getEmployeePhilHealth() {
        return employeePhilHealth;
    }

    public String getEmployeeTIN() {
        return employeeTIN;
    }

    public String getEmployeePagIbig() {
        return employeePagIbig;
    }

    public String getEmployeeStatus() {
        return employeeStatus;
    }

    public String getEmployeePosition() {
        return employeePosition;
    }

    public HomePage getHomePage() {
        return homePage;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public String getEmployeeSupervisor() {
        return employeeSupervisor;
    }

    public String getUserID() {
        return userID;
    }


    public void setEmployeeNo(String newEmployeeNo) {
        this.employeeNo = newEmployeeNo;
    }

    public void setEmployeeFN(String newEmployeeFN) {
        this.employeeFN = newEmployeeFN;
    }

    public void setEmployeeLN(String newEmployeeLN) {
        this.employeeLN = newEmployeeLN;
    }

    public void setEmployeeAddress(String newEmployeeAddress) {
        this.employeeAddress = newEmployeeAddress;
    }

    public void setEmployeeDOB(Date newEmployeeDOB) {
        this.employeeDOB = newEmployeeDOB;
    }

    public void setEmployeePhoneNumber(String newEmployeePhoneNumber) {
        this.employeePhoneNumber = newEmployeePhoneNumber;
    }

    public void setEmployeeSSS(String newEmployeeSSS) {
        this.employeeSSS = newEmployeeSSS;
    }

    public void setEmployeePhilHealth(String newEmployeePhilHealth) {
        this.employeePhilHealth = newEmployeePhilHealth;
    }

    public void setEmployeeTIN(String newEmployeeTIN) {
        this.employeeTIN = newEmployeeTIN;
    }

    public void setEmployeePagIbig(String newEmployeePagIbig) {
        this.employeePagIbig = newEmployeePagIbig;
    }

    public void setEmployeeStatus(String newEmployeeStatus) {
        this.employeeStatus = newEmployeeStatus;
    }

    public void setEmployeePosition(String newEmployeePosition) {
        this.employeePosition = newEmployeePosition;
    }

    public void setEmployeeSupervisor(String newEmployeeSupervisor) {
        this.employeeSupervisor = newEmployeeSupervisor;
    }

    public void setHomePage(HomePage newHomePage) {
        this.homePage = newHomePage;
    }

    public void setBasicSalary(double newBasicSalary) {
        this.basicSalary = newBasicSalary;
    }

    public void setUserID(String UserID) {
        this.userID = UserID;
    }


    public static Employee createEmployeeInstance(String username) throws SQLException {
        CredentialDAO credentialDAO = new CredentialDAO();

        // 1. Get the actual UserID (EmployeeID) from the username
        String employeeId = credentialDAO.getUserIdByUsername(username);
        if (employeeId == null) {
            LOGGER.log(Level.WARNING, "No UserID found for username: " + username);
            return null; // User not found
        }

        // 2. Get ALL RoleNames for this user
        List<String> roles = credentialDAO.getUserRoles(username);

        // 3. Determine the highest privilege role
        if (roles.contains("Admin")) {
            return new Admin(employeeId);
        } else if (roles.contains("Manager")) {
            return new Manager(employeeId);
        } else if (roles.contains("Regular Employee")) {
            return new RegularEmployee(employeeId);
        } else {
            LOGGER.log(Level.SEVERE, "No recognized role found for username: " + username + ". Roles found: " + roles);
            throw new IllegalArgumentException("No recognized role found for user: " + username);
        }
    }


    @Override
    public boolean readData(String empNo) throws SQLException {
        EmployeeDAO employeeDAO = new EmployeeDAO();
        Employee fetchedEmployee = employeeDAO.getEmployeeByEmployeeNo(empNo);

        if (fetchedEmployee != null) {
            this.setEmployeeNo(fetchedEmployee.getEmployeeNo());
            this.setEmployeeLN(fetchedEmployee.getEmployeeLN());
            this.setEmployeeFN(fetchedEmployee.getEmployeeFN());
            this.setEmployeeDOB(fetchedEmployee.getEmployeeDOB());
            this.setEmployeeAddress(fetchedEmployee.getEmployeeAddress());
            this.setEmployeePhoneNumber(fetchedEmployee.getEmployeePhoneNumber());
            this.setEmployeeSSS(fetchedEmployee.getEmployeeSSS());
            this.setEmployeePhilHealth(fetchedEmployee.getEmployeePhilHealth());
            this.setEmployeeTIN(fetchedEmployee.getEmployeeTIN());
            this.setEmployeePagIbig(fetchedEmployee.getEmployeePagIbig());
            this.setEmployeeStatus(fetchedEmployee.getEmployeeStatus());
            this.setEmployeePosition(fetchedEmployee.getEmployeePosition());
            this.setEmployeeSupervisor(fetchedEmployee.getEmployeeSupervisor());
            this.setBasicSalary(fetchedEmployee.getBasicSalary()); // Set basic salary from fetched data
            return true;
        }
        return false;
    }

    public abstract void accessPermissions(HomePage homePage);

    public abstract void viewEmployeeDetails();

    public abstract void processLeaveRequest();

    public abstract void viewSalary();

}
