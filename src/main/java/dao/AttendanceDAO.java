package dao;

import data.DBConnection; //
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

// This class handles database operations for the 'attendance' table.
public class AttendanceDAO {

    private static final Logger LOGGER = Logger.getLogger(AttendanceDAO.class.getName()); //
    // Define a formatter for your database date string (if applicable)
    // Your DB stores date as YYYY-MM-DD
    private static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //

    // Retrieves attendance records for a specific employee within a given month.
    public List<AttendanceRecord> getAttendanceRecordsByEmployeeAndMonth(String employeeNo, String monthName, int year) throws SQLException {
        List<AttendanceRecord> records = new ArrayList<>(); //

        // Convert month name to a 2-digit month number for SQL filtering
        String monthNumber = getMonthNumber(monthName); //
        if (monthNumber == null) { //
            LOGGER.log(Level.WARNING, "Invalid month name provided: " + monthName); //
            // Throw SQLException or a custom exception to indicate invalid input.
            // For now, re-throwing a generic SQLException.
            throw new SQLException("Invalid month name provided: " + monthName); //
        }

        // The SQL query selects records for a specific employee and within a specific month of a year.
        // Corrected column name from 'RecordDate' to 'Date'.
        String sql = "SELECT AttendanceID, EmployeeID, Date, LogInTime, LogOutTime, HoursWorked " + //
                "FROM attendance WHERE EmployeeID = ? AND MONTH(Date) = ? AND YEAR(Date) = ?"; //

        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { // Gets its own connection

            pstmt.setString(1, employeeNo); //
            pstmt.setString(2, monthNumber); // Use the 2-digit month number
            pstmt.setInt(3, year); // Set the year

            try (ResultSet rs = pstmt.executeQuery()) { //
                while (rs.next()) { //
                    AttendanceRecord record = new AttendanceRecord( //
                            rs.getInt("AttendanceID"), //
                            rs.getString("EmployeeID"), //
                            rs.getString("Date"), // Corrected column name here
                            rs.getString("LogInTime"), //
                            rs.getString("LogOutTime"), //
                            rs.getDouble("HoursWorked") //
                    );
                    records.add(record); //
                }
            }
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error getting attendance records for employee " + employeeNo + " for month " + monthName + ", year " + year, e); //
            throw e; // Re-throw the exception
        }
        return records; //
    }

    // Helper method to convert month name to a two-digit number.
    private String getMonthNumber(String monthName) { //
        return switch (monthName.toLowerCase()) { //
            case "january" -> "01"; //
            case "february" -> "02"; //
            case "march" -> "03"; //
            case "april" -> "04"; //
            case "may" -> "05"; //
            case "june" -> "06"; //
            case "july" -> "07"; //
            case "august" -> "08"; //
            case "september" -> "09"; //
            case "october" -> "10"; //
            case "november" -> "11"; //
            case "december" -> "12"; //
            default -> null; // Invalid month name
        };
    }

    // A simple POJO to hold attendance data retrieved from the database.
    public static class AttendanceRecord { //

        private int attendanceId; //
        private String employeeId; //
        private String recordDate; // This is the 'Date' column from the DB
        private String logInTime; //
        private String logOutTime; //
        private double hoursWorked; //

        public AttendanceRecord(int attendanceId, String employeeId, String recordDate, String logInTime, String logOutTime, double hoursWorked) { //
            this.attendanceId = attendanceId; //
            this.employeeId = employeeId; //
            this.recordDate = recordDate; //
            this.logInTime = logInTime; //
            this.logOutTime = logOutTime; //
            this.hoursWorked = hoursWorked; //
        }

        // Getters
        public int getAttendanceId() { //
            return attendanceId; //
        }

        public String getEmployeeId() { //
            return employeeId; //
        }

        public String getRecordDate() { //
            return recordDate; //
        } // Renamed for clarity to avoid confusion with java.util.Date

        public String getLogInTime() { //
            return logInTime; //
        }

        public String getLogOutTime() { //
            return logOutTime; //
        }

        public double getHoursWorked() { //
            return hoursWorked; //
        }
    }
}