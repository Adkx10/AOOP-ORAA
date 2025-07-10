package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import model.LeaveRequest;
import dao.EmployeeDAO;

public class LeaveRequestDAO {

    private static final Logger LOGGER = Logger.getLogger(LeaveRequestDAO.class.getName());
    private LeaveTypeDAO leaveTypeDAO;
    private EmployeeDAO employeeDAO; // To fetch employee details for LeaveRequest constructor

    public LeaveRequestDAO() {
        this.leaveTypeDAO = new LeaveTypeDAO();
        this.employeeDAO = new EmployeeDAO();
    }

    private LeaveRequest buildLeaveRequestFromResultSet(ResultSet rs) throws SQLException {
        int requestId = rs.getInt("LeaveRequestID");
        String employeeId = rs.getString("EmployeeID");
        int leaveTypeID = rs.getInt("LeaveTypeID");
        String leaveTypeName = leaveTypeDAO.getLeaveTypeName(leaveTypeID);
        Date requestedDate = rs.getDate("RequestedDate");
        String status = rs.getString("Status");
        String reason = rs.getString("Reason");
        Date submissionDate = rs.getTimestamp("RequestDate");
        String approvedByEmployeeID = rs.getString("ApprovedByEmployeeID");
        Date approvalDate = rs.getTimestamp("ApprovalDate");
        String remarks = rs.getString("Remarks");

        String lastName = "";
        String firstName = "";
        try {

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

    public boolean submitLeaveRequest(Connection conn, LeaveRequest request) throws SQLException {
        int leaveTypeId = leaveTypeDAO.getLeaveTypeId(request.getLeaveTypeName());
        if (leaveTypeId == -1) {
            LOGGER.log(Level.WARNING, "Invalid Leave Type Name: " + request.getLeaveTypeName());
            throw new SQLException("Invalid Leave Type provided: " + request.getLeaveTypeName());
        }

        String sql = "INSERT INTO leaverequest (EmployeeID, LeaveTypeID, RequestedDate, Status, Reason, Remarks) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, request.getEmployeeId());
            pstmt.setInt(2, leaveTypeId);
            pstmt.setDate(3, new java.sql.Date(request.getRequestedDate().getTime()));
            pstmt.setString(4, request.getStatus());
            pstmt.setString(5, request.getReason());
            pstmt.setString(6, request.getRemarks());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        request.setRequestId(generatedKeys.getInt(1));
                    }
                }
            }
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error submitting leave request for employee: " + request.getEmployeeId(), e);
            throw e;
        }
    }

    // Retrieves leave requests for a specific employee.
    public List<LeaveRequest> getLeaveRequestsByEmployeeNo(String employeeId) throws SQLException {
        String sql = "SELECT lr.*, e.LastName, e.FirstName, lt.LeaveName "
                + //
                "FROM leaverequest lr "
                + //
                "JOIN employee e ON lr.EmployeeID = e.EmployeeID "
                + //
                "JOIN leavetype lt ON lr.LeaveTypeID = lt.LeaveTypeID "
                + //
                "WHERE lr.EmployeeID = ? AND lr.IsDeleted = 0 ORDER BY lr.RequestDate DESC";
        List<LeaveRequest> requests = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(buildLeaveRequestFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting leave requests for employee: " + employeeId, e); //
            throw e;
        }
        return requests;
    }

    // Retrieves all leave requests from the database.
    public List<LeaveRequest> getAllLeaveRequests() throws SQLException {

        String sql = "SELECT lr.*, e.LastName, e.FirstName, lt.LeaveName "
                + "FROM leaverequest lr "
                + "JOIN employee e ON lr.EmployeeID = e.EmployeeID "
                + "JOIN leavetype lt ON lr.LeaveTypeID = lt.LeaveTypeID "
                + "WHERE lr.IsDeleted = 0 ORDER BY lr.RequestDate DESC";
        List<LeaveRequest> requests = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                requests.add(buildLeaveRequestFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all leave requests", e);
            throw e;
        }
        return requests;
    }

    public List<LeaveRequest> getLeaveBalancesByEmployeeNo(String employeeNo) throws SQLException {
        List<LeaveRequest> balances = new ArrayList<>();
        // SQL query to join leavebalance and leavetype tables
        String sql = "SELECT lt.LeaveName, lb.BalanceDays "
                + "FROM leavebalance lb "
                + "JOIN leavetype lt ON lb.LeaveTypeID = lt.LeaveTypeID "
                + "WHERE lb.EmployeeID = ?";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String leaveTypeName = rs.getString("LeaveName");
                    double balanceDays = rs.getDouble("BalanceDays");
                    balances.add(new LeaveRequest(leaveTypeName, balanceDays));
                }
            }
        }
        return balances;
    }

    public boolean deleteLeaveRequest(String employeeId, Date requestedDate) throws SQLException {
        String sql = "UPDATE leaverequest SET IsDeleted = 1 WHERE EmployeeID = ? AND RequestedDate = ? AND Status = 'Pending'";

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeId);
            pstmt.setDate(2, new java.sql.Date(requestedDate.getTime()));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting leave request for employee: " + employeeId + " on date: " + requestedDate, e);
            throw e;
        }
    }

    public boolean updateLeaveRequestStatusAndRemarks(Connection conn, String employeeId, Date requestedDate, String newStatus, String newRemarks, String approvedByEmployeeId) throws SQLException {

        String sql = "UPDATE leaverequest SET Status = ?, Remarks = ?, ApprovalDate = ?, ApprovedByEmployeeID = ? WHERE EmployeeID = ? AND RequestedDate = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, newRemarks);

            if ("Approved".equalsIgnoreCase(newStatus)) {
                pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
                pstmt.setString(4, approvedByEmployeeId);
            } else { // For Pending or Rejected, clear approval info
                pstmt.setNull(3, java.sql.Types.TIMESTAMP);
                pstmt.setNull(4, java.sql.Types.VARCHAR);
            }

            pstmt.setString(5, employeeId);
            pstmt.setDate(6, new java.sql.Date(requestedDate.getTime()));

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating leave request status for employee: " + employeeId + " on date: " + requestedDate, e);
            throw e;
        }
    }

    public boolean deductLeaveBalance(Connection conn, String employeeId, int leaveTypeId, double daysToDeduct) throws SQLException {

        String sql = "UPDATE leavebalance SET BalanceDays = BalanceDays - ? "
                + //
                "WHERE EmployeeID = ? AND LeaveTypeID = ? AND IsDeleted = 0";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, daysToDeduct);
            pstmt.setString(2, employeeId);
            pstmt.setInt(3, leaveTypeId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deducting leave balance for employee " + employeeId + ", type " + leaveTypeId, e);
            throw e;
        }
    }
}
