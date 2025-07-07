package model;

import dao.AttendanceDAO;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;
import java.sql.SQLException; // Import for database-related exceptions
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Removed all CSV-related imports (com.opencsv.*, java.io.*)
public class Attendance {

    private static final Logger LOGGER = Logger.getLogger(Attendance.class.getName());

    private String _employeeNo;
    private String _employeeName; // Not directly used in calculations here
    private String _month; // Will store the month name (e.g., "January")
    private String _date; // Individual record date string
    private String _timeIn; // Individual record time in string
    private String _timeOut; // Individual record time out string
    private String pattern = "HH:mm"; // Date format pattern for time parsing
    private Date time1;
    private Date time2;
    private double timeOne;
    private double timeTwo;
    private double hours; // Hours worked for a single record after calculations
    private double totalHours; // Accumulated total hours for the month
    private double gracePer; // Grace period adjustment for a single record

    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

    // --- Getter Methods ---
    public String EmployeeNo() {
        return _employeeNo;
    }

    public String EmployeeName() {
        return _employeeName;
    }

    public String Month() {
        return _month;
    } // Returns the set month name

    public String Date() {
        return _date;
    } // Returns the last processed record date

    public double TimeIn() throws ParseException {
        time1 = dateFormat.parse(_timeIn);
        // Convert milliseconds to hours, adjusting by 5 hours (timezone or initial offset)
        return (time1.getTime() / (60 * 60 * 1000)) - 5;
    }

    public double TimeOut() throws ParseException {
        time2 = dateFormat.parse(_timeOut);
        // Convert milliseconds to hours, adjusting by 5 hours (timezone or initial offset)
        return (time2.getTime() / (60 * 60 * 1000)) - 5;
    }

    /**
     * Calculates the hours worked for a single attendance record, including
     * grace period. This method relies on _timeIn and _timeOut being set from
     * an AttendanceRecord.
     *
     * @return The calculated hours worked for the current record.
     * @throws ParseException if time strings cannot be parsed.
     */
    public double HoursWorked() throws ParseException {
        hours = TimeOut() - TimeIn();
        // Adjust for lunch break if hours are within typical work day range
        if (hours >= 8 || hours > 4) { // This condition (hours > 4) might double-count deductions for 8+ hour days. Re-evaluate.
            hours -= 1; // Deduct 1 hour for lunch break
        }
        // Apply grace period logic
        if (hours >= 7.83 && hours < 8) { // If close to 8 hours (within 10 minutes)
            // The original calculation (TimeIn() - 8) + hours seems problematic.
            // A grace period usually means not penalizing for being slightly late,
            // or counting a full hour if it's almost met. This needs clarification.
            // For now, retaining the original logic, but it should be reviewed based on business rules.
            gracePer = (TimeIn() - 8) + hours;
        } else {
            gracePer = hours;
        }
        return gracePer;
    }

    public double FinalHW() {
        return totalHours;
    }

    // --- Setter Methods ---
    public void SetEmployeeNo(String EmployeeNo) {
        _employeeNo = EmployeeNo;
    }

    public void SetMonth(String Month) {
        _month = Month;
    } // Sets the month name (e.g., "January")

    /**
     * Computes the total hours worked for an employee for a given month and
     * year by fetching records from the database using AttendanceDAO.
     *
     * @param year The year for which to compute attendance.
     * @return true if attendance records were found and processed, false
     * otherwise.
     * @throws SQLException if a database access error occurs.
     * @throws ParseException if time strings from the database cannot be
     * parsed.
     */
    public boolean ComputeHourWorked(int year) throws SQLException, ParseException {
        AttendanceDAO attendanceDAO = new AttendanceDAO();
        // Fetch attendance records for the specified employee, month name, and year
        List<AttendanceDAO.AttendanceRecord> records = attendanceDAO.getAttendanceRecordsByEmployeeAndMonth(_employeeNo, _month, year);

        totalHours = 0; // Reset total hours before summing up for the current month

        if (records.isEmpty()) {
            LOGGER.log(Level.INFO, "No attendance records found for employee {0} for month {1}, year {2}", new Object[]{_employeeNo, _month, year});
            return false; // No records found for this employee and month
        }

        for (AttendanceDAO.AttendanceRecord record : records) {
            // Set individual record data to perform calculation for each day
            _date = record.getRecordDate();
            _timeIn = record.getLogInTime();
            _timeOut = record.getLogOutTime();

            // Calculate hours worked for this specific record (day)
            // This will update 'gracePer' based on current _timeIn, _timeOut
            HoursWorked();
            totalHours += gracePer; // Accumulate the calculated daily hours
        }
        return true; // Records were found and processed
    }
}
