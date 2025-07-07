package model;

import gui.*; // Imports all GUI classes
import java.sql.SQLException; // Import for database-related exceptions
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

// Removed all CSV-related imports (com.opencsv.*, java.io.*)

public class Manager extends Employee implements DataReader {

    private static final Logger LOGGER = Logger.getLogger(Manager.class.getName());

    // Default constructor
    public Manager() {
        super("", "", "", "", null, "", "", "", "", "", "", "", "", 0.0);
    }

    // For login (sets employeeNo, then data can be read from DB if needed)
    public Manager(String employeeNo) {
        super(employeeNo);
    }

    public Manager(String userID, String employeeFN, String employeeLN, String employeeAddress, Date employeeDOB, String employeePhoneNumber, String employeeSSS, String employeePhilHealth, String employeeTIN, String employeePagIbig, String employeeStatus, String employeePosition, String employeeSupervisor, double basicSalary) {
        super(userID, employeeFN, employeeLN, employeeAddress, employeeDOB, employeePhoneNumber, employeeSSS, employeePhilHealth, employeeTIN, employeePagIbig, employeeStatus, employeePosition, employeeSupervisor, basicSalary);
    }

    @Override
    public void accessPermissions(HomePage homePage) {
        homePage.addButton(homePage.getViewEmployeeDetailsBtn());
        homePage.addButton(homePage.getViewAllSalaryBtn());
        homePage.addButton(homePage.getViewLeaveBtn());
    }

    @Override
    public void viewEmployeeDetails() {
            EmployeeData frame = new EmployeeData(this.getHomePage(), this.getEmployeeNo());
            frame.setVisible(true);
            frame.setReadOnly(); // Managers can view, but not add/delete/update other employees
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
