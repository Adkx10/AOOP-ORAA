package model;

import gui.EmployeeData;
import gui.HomePage;
import gui.ViewRequest;
import gui.ViewSalary;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegularEmployee extends Employee implements DataReader {

    private static final Logger LOGGER = Logger.getLogger(RegularEmployee.class.getName());

    // Default constructor
    public RegularEmployee() {
        super("", "", "", "", null, "", "", "", "", "", "", "", "", 0.0);
    }

    // For login (sets employeeNo)
    public RegularEmployee(String employeeNo) {
        super(employeeNo);
    }

    public RegularEmployee(String userID, String employeeFN, String employeeLN, String employeeAddress, Date employeeDOB, String employeePhoneNumber, String employeeSSS, String employeePhilHealth, String employeeTIN, String employeePagIbig, String employeeStatus, String employeePosition, String employeeSupervisor, double basicSalary) {
        super(userID, employeeFN, employeeLN, employeeAddress, employeeDOB, employeePhoneNumber, employeeSSS, employeePhilHealth, employeeTIN, employeePagIbig, employeeStatus, employeePosition, employeeSupervisor, basicSalary);
    }

    @Override
    public void accessPermissions(HomePage homePage) {
        homePage.addButton(homePage.getViewPersonalDetailsBtn());
        homePage.addButton(homePage.getViewSalaryBtn());
        homePage.addButton(homePage.getRequestLeaveBtn());
    }

    @Override
    public void viewEmployeeDetails() {
            EmployeeData frame = new EmployeeData(this.getHomePage(), this.getEmployeeNo());
            frame.setVisible(true);
            frame.viewOwnInfo(); // Regular employees only see their own info
    }

    @Override
    public void processLeaveRequest() {
            ViewRequest frame = new ViewRequest(this.getHomePage());
            frame.setVisible(true);
            frame.leaveRequestor(); // Regular employees can only request leave
    }

    @Override
    public void viewSalary() {
            ViewSalary frame = new ViewSalary(this.getHomePage(), this.getEmployeeNo());
            frame.setVisible(true);
            frame.viewOwnSalary(); // Regular employees only view their own salary
    }
    
}
