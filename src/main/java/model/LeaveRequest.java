package model;

import java.util.Date;

public class LeaveRequest {

    private int requestId;
    private String employeeId;
    private int leaveTypeID;
    private String leaveTypeName;
    private Date requestedDate;
    private String status;
    private String reason;
    private Date submissionDate;
    private String lastName;
    private String firstName;
    private String approvedByEmployeeID;
    private Date approvalDate;
    private String remarks;
    private double balanceDays;

    // Constructor for creating new leave requests (from GUI, with a single requested date)
    public LeaveRequest(String employeeId, String lastName, String firstName, String reason, String leaveTypeName,
            Date requestedDate, String status, String remarks) {
        this.employeeId = employeeId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.reason = reason;
        this.leaveTypeName = leaveTypeName;
        this.requestedDate = requestedDate;
        this.status = status;
        this.remarks = remarks;

    }

    // Constructor for retrieving existing leave requests (from DB, with all fields)
    public LeaveRequest(int requestId, String employeeId, int leaveTypeID, String leaveTypeName, Date requestedDate,
            String status, String reason, Date submissionDate,
            String lastName, String firstName, String approvedByEmployeeID, Date approvalDate, String remarks) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.leaveTypeID = leaveTypeID;
        this.leaveTypeName = leaveTypeName;
        this.requestedDate = requestedDate;
        this.status = status;
        this.reason = reason;
        this.submissionDate = submissionDate;
        this.lastName = lastName;
        this.firstName = firstName;
        this.approvedByEmployeeID = approvedByEmployeeID;
        this.approvalDate = approvalDate;
        this.remarks = remarks;
    }

    public LeaveRequest(String leaveTypeName, double balanceDays) {
        this.leaveTypeName = leaveTypeName;
        this.balanceDays = balanceDays;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public int getLeaveTypeID() {
        return leaveTypeID;
    }

    public String getLeaveTypeName() {
        return leaveTypeName;
    }

    public Date getRequestedDate() {
        return requestedDate;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Date getSubmissionDate() {
        return submissionDate;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getApprovedByEmployeeID() {
        return approvedByEmployeeID;
    }

    public Date getApprovalDate() {
        return approvalDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public double getBalanceDays() {
        return balanceDays;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public void setLeaveTypeID(int leaveTypeID) {
        this.leaveTypeID = leaveTypeID;
    }

    public void setLeaveTypeName(String leaveTypeName) {
        this.leaveTypeName = leaveTypeName;
    }

    public void setRequestedDate(Date requestedDate) {
        this.requestedDate = requestedDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setSubmissionDate(Date submissionDate) {
        this.submissionDate = submissionDate;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setApprovedByEmployeeID(String approvedByEmployeeID) {
        this.approvedByEmployeeID = approvedByEmployeeID;
    }

    public void setApprovalDate(Date approvalDate) {
        this.approvalDate = approvalDate;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
