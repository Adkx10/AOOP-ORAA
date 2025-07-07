package model;

import gui.*; // Imports all GUI classes
import java.sql.SQLException; // Import for database-related exceptions
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

// Removed all CSV-related imports (com.opencsv.*, java.io.*)
public class Admin extends Employee implements DataReader {

    private static final Logger LOGGER = Logger.getLogger(Admin.class.getName());

    // Default constructor
    public Admin() {
        super("", "", "", "", null, "", "", "", "", "", "", "", "", 0.0);
    }

    // For login (sets employeeNo, then data can be read from DB if needed)
    public Admin(String employeeNo) {
        super(employeeNo);
    }

    public Admin(String userID, String employeeFN, String employeeLN, String employeeAddress, Date employeeDOB, String employeePhoneNumber, String employeeSSS, String employeePhilHealth, String employeeTIN, String employeePagIbig, String employeeStatus, String employeePosition, String employeeSupervisor, double basicSalary) {
        super(userID, employeeFN, employeeLN, employeeAddress, employeeDOB, employeePhoneNumber, employeeSSS, employeePhilHealth, employeeTIN, employeePagIbig, employeeStatus, employeePosition, employeeSupervisor, basicSalary);
    }

    @Override
    public void accessPermissions(HomePage homePage) {
        homePage.addButton(homePage.getAddUpdateDeleteBtn());
        homePage.addButton(homePage.getViewAllSalaryBtn());
        homePage.addButton(homePage.getViewLeaveBtn());
    }

    @Override
    public void viewEmployeeDetails() {
        EmployeeData frame = new EmployeeData(this.getHomePage(), this.getEmployeeNo());
        frame.setVisible(true);
    }

    @Override
    public void processLeaveRequest() {
        ViewRequest frame = new ViewRequest(this.getHomePage());
        frame.setVisible(true);
        frame.leaveProcessor();
    }

    @Override
    public void viewSalary() {
        ViewSalary frame = new ViewSalary(this.getHomePage(), this.getEmployeeNo());
        frame.setVisible(true);
    }

}
