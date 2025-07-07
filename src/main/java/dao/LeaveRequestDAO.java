package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp; // For datetime columns
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date; // java.util.Date
import model.LeaveRequest;
import dao.EmployeeDAO; // Needed to fetch Employee details (FN/LN)

public class LeaveRequestDAO {

    private static final Logger LOGGER = Logger.getLogger(LeaveRequestDAO.class.getName());
    private LeaveTypeDAO leaveTypeDAO;
    private EmployeeDAO employeeDAO; // To fetch employee details for LeaveRequest constructor

    public LeaveRequestDAO() {
        this.leaveTypeDAO = new LeaveTypeDAO();
        this.employeeDAO = new EmployeeDAO(); // Initialize EmployeeDAO
    }

    // Helper method to build LeaveRequest object from ResultSet
    private LeaveRequest buildLeaveRequestFromResultSet(ResultSet rs) throws SQLException {
        int requestId = rs.getInt("LeaveRequestID");
        String employeeId = rs.getString("EmployeeID");
        int leaveTypeID = rs.getInt("LeaveTypeID");
        String leaveTypeName = leaveTypeDAO.getLeaveTypeName(leaveTypeID); // This DAO gets its own connection
        Date requestedDate = rs.getDate("RequestedDate"); // Changed from StartDate
        String status = rs.getString("Status");
        String reason = rs.getString("Reason");
        Date submissionDate = rs.getTimestamp("RequestDate"); // Changed from RequestDate to SubmissionDate
        String approvedByEmployeeID = rs.getString("ApprovedByEmployeeID");
        Date approvalDate = rs.getTimestamp("ApprovalDate");
        String remarks = rs.getString("Remarks");

        // Fetch LastName and FirstName using EmployeeDAO
        String lastName = "";
        String firstName = "";
        try {
            // This method needs to be able to get its own connection
            model.Employee employee = employeeDAO.getEmployeeByEmployeeNo(employeeId);
            if (employee != null) {
                lastName = employee.getEmployeeLN();
                firstName = employee.getEmployeeFN();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not fetch employee details for leave request " + requestId + ": " + e.getMessage(), e);
        }

        return new LeaveRequest(
            requestId, employeeId, leaveTypeID, leaveTypeName, requestedDate,
            status, reason, submissionDate, lastName, firstName, approvedByEmployeeID, approvalDate, remarks
        );
    }

    /**
     * Inserts a new leave request into the database using a provided connection.
     *
     * @param conn The database connection to use for the transaction.
     * @param request The LeaveRequest object to submit.
     * @return true if insertion was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean submitLeaveRequest(Connection conn, LeaveRequest request) throws SQLException { //
        int leaveTypeId = leaveTypeDAO.getLeaveTypeId(request.getLeaveTypeName()); // This DAO gets its own connection
        if (leaveTypeId == -1) {
            LOGGER.log(Level.WARNING, "Invalid Leave Type Name: " + request.getLeaveTypeName()); //
            throw new SQLException("Invalid Leave Type provided: " + request.getLeaveTypeName()); //
        }

        // SQL query: RequestDate is handled by DB's CURRENT_TIMESTAMP default
        // Changed StartDate and EndDate to RequestedDate
        String sql = "INSERT INTO leaverequest (EmployeeID, LeaveTypeID, RequestedDate, Status, Reason, Remarks) VALUES (?, ?, ?, ?, ?, ?)"; //
        
        // Use the provided connection, do NOT close it here
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { //

            pstmt.setString(1, request.getEmployeeId()); //
            pstmt.setInt(2, leaveTypeId); //
            pstmt.setDate(3, new java.sql.Date(request.getRequestedDate().getTime())); // Use RequestedDate
            pstmt.setString(4, request.getStatus()); //
            pstmt.setString(5, request.getReason()); //
            pstmt.setString(6, request.getRemarks()); //

            int rowsAffected = pstmt.executeUpdate(); //
            
            if (rowsAffected > 0) { //
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) { //
                    if (generatedKeys.next()) { //
                        request.setRequestId(generatedKeys.getInt(1)); //
                    }
                }
            }
            return rowsAffected > 0; //
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error submitting leave request for employee: " + request.getEmployeeId(), e); //
            throw e; //
        }
    }

    // Retrieves leave requests for a specific employee.
    public List<LeaveRequest> getLeaveRequestsByEmployeeNo(String employeeId) throws SQLException {
        // Changed lr.StartDate to lr.RequestedDate in SELECT and WHERE
        String sql = "SELECT lr.*, e.LastName, e.FirstName, lt.LeaveName " + //
                     "FROM leaverequest lr " + //
                     "JOIN employee e ON lr.EmployeeID = e.EmployeeID " + //
                     "JOIN leavetype lt ON lr.LeaveTypeID = lt.LeaveTypeID " + //
                     "WHERE lr.EmployeeID = ? AND lr.IsDeleted = 0 ORDER BY lr.RequestDate DESC"; // RequestDate is submissionDate
        List<LeaveRequest> requests = new ArrayList<>(); //

        try (Connection conn = DBConnection.getConnection(); // Gets its own connection
             PreparedStatement pstmt = conn.prepareStatement(sql)) { //

            pstmt.setString(1, employeeId); //
            try (ResultSet rs = pstmt.executeQuery()) { //
                while (rs.next()) { //
                    requests.add(buildLeaveRequestFromResultSet(rs)); //
                }
            }
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error getting leave requests for employee: " + employeeId, e); //
            throw e; //
        }
        return requests; //
    }

    // Retrieves all leave requests from the database.
    public List<LeaveRequest> getAllLeaveRequests() throws SQLException {
        // Changed lr.StartDate to lr.RequestedDate in SELECT
        String sql = "SELECT lr.*, e.LastName, e.FirstName, lt.LeaveName " + //
                     "FROM leaverequest lr " + //
                     "JOIN employee e ON lr.EmployeeID = e.EmployeeID " + //
                     "JOIN leavetype lt ON lr.LeaveTypeID = lt.LeaveTypeID " + //
                     "WHERE lr.IsDeleted = 0 ORDER BY lr.RequestDate DESC"; //
        List<LeaveRequest> requests = new ArrayList<>(); //

        try (Connection conn = DBConnection.getConnection(); // Gets its own connection
             PreparedStatement pstmt = conn.prepareStatement(sql); //
             ResultSet rs = pstmt.executeQuery()) { //

            while (rs.next()) { //
                requests.add(buildLeaveRequestFromResultSet(rs)); //
            }
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error getting all leave requests", e); //
            throw e; //
        }
        return requests; //
    }

    /**
     * Updates the status and remarks of a specific leave request using a provided connection.
     * Also updates ApprovalDate and ApprovedByEmployeeID if status is 'Approved'.
     *
     * @param conn The database connection to use for the transaction.
     * @param employeeId The EmployeeID of the request.
     * @param requestedDate The RequestedDate (java.util.Date) of the request used to identify the request.
     * @param newStatus The new status to set.
     * @param newRemarks The new remarks.
     * @param approvedByEmployeeId The ID of the employee approving the request (for 'Approved' status).
     * @return true if update was successful, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean updateLeaveRequestStatusAndRemarks(Connection conn, String employeeId, Date requestedDate, String newStatus, String newRemarks, String approvedByEmployeeId) throws SQLException { //
        // Changed StartDate to RequestedDate in WHERE clause
        String sql = "UPDATE leaverequest SET Status = ?, Remarks = ?, ApprovalDate = ?, ApprovedByEmployeeID = ? WHERE EmployeeID = ? AND RequestedDate = ?"; //
        
        // Use the provided connection, do NOT close it here
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { //

            pstmt.setString(1, newStatus); //
            pstmt.setString(2, newRemarks); //
            
            if ("Approved".equalsIgnoreCase(newStatus)) { //
                pstmt.setTimestamp(3, new Timestamp(new Date().getTime())); // Current timestamp
                pstmt.setString(4, approvedByEmployeeId); //
            } else { // For Pending or Rejected, clear approval info
                pstmt.setNull(3, java.sql.Types.TIMESTAMP); //
                pstmt.setNull(4, java.sql.Types.VARCHAR); //
            }

            pstmt.setString(5, employeeId); //
            pstmt.setDate(6, new java.sql.Date(requestedDate.getTime())); // Use RequestedDate for WHERE clause

            int rowsAffected = pstmt.executeUpdate(); //
            return rowsAffected > 0; //
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error updating leave request status for employee: " + employeeId + " on date: " + requestedDate, e); //
            throw e; //
        }
    }

    /**
     * Deducts leave days from employee's leave balance using a provided connection.
     * This method would be called after a leave request is approved.
     * Since 'leaverequest' now has a single 'RequestedDate', a leave is assumed to be 1 day.
     *
     * @param conn The database connection to use for the transaction.
     * @param employeeId The ID of the employee.
     * @param leaveTypeId The ID of the leave type.
     * @param daysToDeduct The number of days to deduct.
     * @return true if balance was updated, false otherwise.
     * @throws SQLException if a database access error occurs.
     */
    public boolean deductLeaveBalance(Connection conn, String employeeId, int leaveTypeId, double daysToDeduct) throws SQLException { //
        // The daysToDeduct parameter is still passed, but for a single-day leave, it should typically be 1.0.
        String sql = "UPDATE leavebalance SET BalanceDays = BalanceDays - ? " + //
                     "WHERE EmployeeID = ? AND LeaveTypeID = ? AND IsDeleted = 0"; //
        
        // Use the provided connection, do NOT close it here
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { //
            pstmt.setDouble(1, daysToDeduct); // This will typically be 1.0 for a single-day leave
            pstmt.setString(2, employeeId); //
            pstmt.setInt(3, leaveTypeId); //
            int rowsAffected = pstmt.executeUpdate(); //
            return rowsAffected > 0; //
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error deducting leave balance for employee " + employeeId + ", type " + leaveTypeId, e); //
            throw e; //
        }
    }
}