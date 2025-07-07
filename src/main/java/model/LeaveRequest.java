package model;

import java.util.Date; // Use java.util.Date for consistency with JDateChooser

/**
 * This class represents a leave request record from the database.
 * Updated to match 'leaverequest' table schema more closely,
 * especially with the single 'RequestedDate' for the leave period.
 */
public class LeaveRequest {
    private int requestId; // Corresponds to LeaveRequestID in DB
    private String employeeId; // Corresponds to EmployeeID in DB
    private int leaveTypeID; // Corresponds to LeaveTypeID in DB
    private String leaveTypeName; // To store the actual name (e.g., "Vacation Leave") for display
    private Date requestedDate; // Corresponds to RequestedDate in DB (the actual date of leave)
    private String status;      // Corresponds to Status in DB (enum: Pending, Approved, Rejected)
    private String reason;     // Corresponds to Reason in DB
    private Date submissionDate; // Corresponds to RequestDate in DB (timestamp of submission)
    private String lastName;    // For display purposes, not directly in leaverequest table
    private String firstName;   // For display purposes, not directly in leaverequest table
    private String approvedByEmployeeID; // Corresponds to ApprovedByEmployeeID in DB
    private Date approvalDate; // Corresponds to ApprovalDate in DB
    private String remarks;

    // Constructor for creating new leave requests (from GUI, with a single requested date)
    public LeaveRequest(String employeeId, String lastName, String firstName, String reason, String leaveTypeName,
                        Date requestedDate, String status, String remarks) {
        this.employeeId = employeeId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.reason = reason;
        this.leaveTypeName = leaveTypeName;
        this.requestedDate = requestedDate; // Set the single requested date
        this.status = status;
        this.remarks = remarks;
        // submissionDate, ApprovedByEmployeeID, ApprovalDate set by DB or later logic
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

    // --- Getters ---
    public int getRequestId() { return requestId; }
    public String getEmployeeId() { return employeeId; }
    public int getLeaveTypeID() { return leaveTypeID; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public Date getRequestedDate() { return requestedDate; } // New: Getter for the single requested date
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public Date getSubmissionDate() { return submissionDate; } // Renamed from getRequestDate
    public String getLastName() { return lastName; }
    public String getFirstName() { return firstName; }
    public String getApprovedByEmployeeID() { return approvedByEmployeeID; }
    public Date getApprovalDate() { return approvalDate; }
    public String getRemarks() { return remarks; }


    // --- Setters (only for fields that might be updated post-creation, e.g., status/remarks/approval info) ---
    public void setRequestId(int requestId) { this.requestId = requestId; }
    public void setLeaveTypeID(int leaveTypeID) { this.leaveTypeID = leaveTypeID; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public void setRequestedDate(Date requestedDate) { this.requestedDate = requestedDate; } // New: Setter for requestedDate
    public void setStatus(String status) { this.status = status; }
    public void setReason(String reason) { this.reason = reason; }
    public void setSubmissionDate(Date submissionDate) { this.submissionDate = submissionDate; } // Renamed from setRequestDate
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setApprovedByEmployeeID(String approvedByEmployeeID) { this.approvedByEmployeeID = approvedByEmployeeID; }
    public void setApprovalDate(Date approvalDate) { this.approvalDate = approvalDate; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
