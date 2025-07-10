package dao;

import data.DBConnection; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilities.UtilMethods;

public class AttendanceDAO {

    private static final Logger LOGGER = Logger.getLogger(AttendanceDAO.class.getName());
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Retrieves attendance records for a specific employee within a given month.
    public List<AttendanceRecord> getAttendanceRecordsByEmployeeAndMonth(String employeeNo, String monthName, int year) throws SQLException {
        List<AttendanceRecord> records = new ArrayList<>(); 

        // Convert month name to a 2-digit month number for SQL filtering
        String monthNumber = UtilMethods.getMonthNumber(monthName);
        if (monthNumber == null) { //
            LOGGER.log(Level.WARNING, "Invalid month name provided: " + monthName);
            throw new SQLException("Invalid month name provided: " + monthName); 
        }

        String sql = "SELECT AttendanceID, EmployeeID, Date, LogInTime, LogOutTime, HoursWorked " + 
                "FROM attendance WHERE EmployeeID = ? AND MONTH(Date) = ? AND YEAR(Date) = ?"; 

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, employeeNo); 
            pstmt.setString(2, monthNumber); 
            pstmt.setInt(3, year); 

            try (ResultSet rs = pstmt.executeQuery()) { 
                while (rs.next()) { 
                    AttendanceRecord record = new AttendanceRecord( 
                            rs.getInt("AttendanceID"), 
                            rs.getString("EmployeeID"), 
                            rs.getString("Date"), 
                            rs.getString("LogInTime"), 
                            rs.getString("LogOutTime"), 
                            rs.getDouble("HoursWorked") 
                    );
                    records.add(record); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting attendance records for employee " + employeeNo + " for month " + monthName + ", year " + year, e); 
            throw e; 
        }
        return records; 
    }

    //POJO to hold attendance data retrieved from the database.
    public static class AttendanceRecord { 

        private int attendanceId; 
        private String employeeId; 
        private String recordDate; 
        private String logInTime;
        private String logOutTime; 
        private double hoursWorked; 

        public AttendanceRecord(int attendanceId, String employeeId, String recordDate, String logInTime, String logOutTime, double hoursWorked) { //
            this.attendanceId = attendanceId; 
            this.employeeId = employeeId; 
            this.recordDate = recordDate; 
            this.logInTime = logInTime; 
            this.logOutTime = logOutTime; 
            this.hoursWorked = hoursWorked; 
        }

        // Getters
        public int getAttendanceId() { 
            return attendanceId; 
        }

        public String getEmployeeId() { 
            return employeeId; 
        }

        public String getRecordDate() { 
            return recordDate;
        }

        public String getLogInTime() { 
            return logInTime; 
        }

        public String getLogOutTime() { 
            return logOutTime; 
        }

        public double getHoursWorked() { 
            return hoursWorked; 
        }
    }
}