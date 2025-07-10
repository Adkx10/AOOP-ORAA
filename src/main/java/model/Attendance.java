package model;

import dao.AttendanceDAO;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Attendance {

    private static final Logger LOGGER = Logger.getLogger(Attendance.class.getName());

    private String _employeeNo;
    private String _employeeName; 
    private String _month; 
    private String _date; 
    private String _timeIn; 
    private String _timeOut; 
    private String pattern = "HH:mm"; 
    private Date time1;
    private Date time2;
    private double timeOne;
    private double timeTwo;
    private double hours; 
    private double totalHours; 
    private double gracePer;

    SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

    public String EmployeeNo() {
        return _employeeNo;
    }

    public String EmployeeName() {
        return _employeeName;
    }

    public String Month() {
        return _month;
    }

    public String Date() {
        return _date;
    } 

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

 
    public double HoursWorked() throws ParseException {
        hours = TimeOut() - TimeIn();
        // Adjust for lunch break if hours are within typical work day range
        if (hours >= 8 || hours > 4) { // This condition (hours > 4) might double-count deductions for 8+ hour days. Re-evaluate.
            hours -= 1; // Deduct 1 hour for lunch break
        }
        // Apply grace period logic
        if (hours >= 7.83 && hours < 8) { // If close to 8 hours (within 10 minutes)

            gracePer = (TimeIn() - 8) + hours;
        } else {
            gracePer = hours;
        }
        return gracePer;
    }

    public double FinalHW() {
        return totalHours;
    }

    public void SetEmployeeNo(String EmployeeNo) {
        _employeeNo = EmployeeNo;
    }

    public void SetMonth(String Month) {
        _month = Month;
    } 


    public boolean ComputeHourWorked(int year) throws SQLException, ParseException {
        AttendanceDAO attendanceDAO = new AttendanceDAO();     
        List<AttendanceDAO.AttendanceRecord> records = attendanceDAO.getAttendanceRecordsByEmployeeAndMonth(_employeeNo, _month, year);

        totalHours = 0; 
        if (records.isEmpty()) {
            LOGGER.log(Level.INFO, "No attendance records found for employee {0} for month {1}, year {2}", new Object[]{_employeeNo, _month, year});
            return false;
        }

        for (AttendanceDAO.AttendanceRecord record : records) {
            
            _date = record.getRecordDate();
            _timeIn = record.getLogInTime();
            _timeOut = record.getLogOutTime();

            
            HoursWorked();
            totalHours += gracePer;
        }
        return true; 
    }
}
