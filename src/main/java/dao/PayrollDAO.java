package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import data.DBConnection;

public class PayrollDAO {

    public void calculatePayroll(String periodName, LocalDate startDate, LocalDate endDate) throws SQLException {
        // Establishes a single transactional connection that is automatically closed.
        try (Connection conn = DBConnection.getTransactionalConnection()) {
            try {
                // --- 1. SETUP: CHECK, CREATE/FIND, AND CLEAN PAY PERIOD ---
                int payPeriodId = -1;
                boolean isPeriodProcessed = false;

                String selectPayPeriodSql = "SELECT PayPeriodID, IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(selectPayPeriodSql)) {
                    pstmt.setDate(1, Date.valueOf(startDate));
                    pstmt.setDate(2, Date.valueOf(endDate));
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        payPeriodId = rs.getInt("PayPeriodID");
                        isPeriodProcessed = rs.getBoolean("IsProcessed");
                    }
                }

                if (isPeriodProcessed) {
                    System.out.println("Payroll for " + periodName + " has already been processed. No action taken.");
                    return;
                }

                if (payPeriodId == -1) {
                    String insertPayPeriodSql = "INSERT INTO payperiod (StartDate, EndDate, PeriodName, IsProcessed, PaymentDate) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertPayPeriodSql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setDate(1, Date.valueOf(startDate));
                        pstmt.setDate(2, Date.valueOf(endDate));
                        pstmt.setString(3, periodName);
                        pstmt.setBoolean(4, false);
                        pstmt.setDate(5, Date.valueOf(endDate.plusDays(5)));
                        pstmt.executeUpdate();
                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                payPeriodId = generatedKeys.getInt(1);
                            } else {
                                throw new SQLException("Creating pay period failed, no ID obtained.");
                            }
                        }
                    }
                }
                
                // This is the fully corrected DELETE block
                try (PreparedStatement pstmtDeletePayslipDetail = conn.prepareStatement("DELETE FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE PayPeriodID = ?)");
                     PreparedStatement pstmtDeleteTaxComputation = conn.prepareStatement("DELETE FROM taxcomputation WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE PayPeriodID = ?)");
                     PreparedStatement pstmtDeleteDeduction = conn.prepareStatement("DELETE FROM deduction WHERE PayPeriodStartDate = ? AND PayperiodEndDate = ?");
                     PreparedStatement pstmtDeletePayslip = conn.prepareStatement("DELETE FROM payslip WHERE PayPeriodID = ?")) {

                    System.out.println("Deleting any partial or old payroll data for PayPeriodID: " + payPeriodId);

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 0;");
                    }

                    pstmtDeletePayslipDetail.setInt(1, payPeriodId);
                    pstmtDeletePayslipDetail.executeUpdate();

                    pstmtDeleteTaxComputation.setInt(1, payPeriodId);
                    pstmtDeleteTaxComputation.executeUpdate();

                    pstmtDeleteDeduction.setDate(1, Date.valueOf(startDate));
                    pstmtDeleteDeduction.setDate(2, Date.valueOf(endDate));
                    pstmtDeleteDeduction.executeUpdate();

                    pstmtDeletePayslip.setInt(1, payPeriodId);
                    pstmtDeletePayslip.executeUpdate();

                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
                    }
                    System.out.println("Previous payroll data for this period successfully cleared.");
                }
                
                // --- 2. PRE-LOADING: ALL SCHEDULES AND TYPES ---
                Map<String, Integer> deductionTypeMap = new HashMap<>();
                String insertDeductionTypeSql = "INSERT IGNORE INTO deductiontype (DeductionName) VALUES (?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertDeductionTypeSql)) {
                    pstmt.setString(1, "SSS Contribution"); pstmt.addBatch();
                    pstmt.setString(1, "Philhealth Contribution"); pstmt.addBatch();
                    pstmt.setString(1, "Pag-IBIG Contribution"); pstmt.addBatch();
                    pstmt.setString(1, "Withholding Tax"); pstmt.addBatch();
                    pstmt.executeBatch();
                }
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DeductionTypeID, DeductionName FROM deductiontype")) {
                    while (rs.next()) deductionTypeMap.put(rs.getString("DeductionName"), rs.getInt("DeductionTypeID"));
                }

                Map<Double, Map<String, Double>> sssSchedule = new HashMap<>();
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM sss_contribution_schedule ORDER BY CompensationRangeStart ASC")) {
                    while (rs.next()) {
                        Map<String, Double> data = new HashMap<>();
                        data.put("end", rs.getDouble("CompensationRangeEnd"));
                        data.put("contribution", rs.getDouble("ContributionAmount"));
                        sssSchedule.put(rs.getDouble("CompensationRangeStart"), data);
                    }
                }
                
                Map<Double, Map<String, Double>> philhealthSchedule = new HashMap<>();
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM philhealth_contribution_schedule ORDER BY MonthlyBasicSalaryMin ASC")) {
                    while (rs.next()) {
                         Map<String, Double> data = new HashMap<>();
                         data.put("max", rs.getDouble("MonthlyBasicSalaryMax"));
                         data.put("rate", rs.getDouble("PremiumRate"));
                         data.put("maxPremium", rs.getDouble("MaxMonthlyPremium"));
                         philhealthSchedule.put(rs.getDouble("MonthlyBasicSalaryMin"), data);
                    }
                }

                Map<Double, Map<String, Double>> pagibigSchedule = new HashMap<>();
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM pagibig_contribution_schedule ORDER BY MonthlyBasicSalaryMin ASC")) {
                     while (rs.next()) {
                        Map<String, Double> data = new HashMap<>();
                        data.put("max", rs.getDouble("MonthlyBasicSalaryMax"));
                        data.put("employeeRate", rs.getDouble("EmployeeContributionRate"));
                        pagibigSchedule.put(rs.getDouble("MonthlyBasicSalaryMin"), data);
                    }
                }

                Map<Double, Map<String, Double>> taxSchedule = new HashMap<>();
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM withholding_tax_schedule ORDER BY MonthlyRateMin ASC")) {
                    while (rs.next()) {
                        Map<String, Double> data = new HashMap<>();
                        data.put("max", rs.getDouble("MonthlyRateMax"));
                        data.put("percentage", rs.getDouble("TaxPercentage"));
                        data.put("fixed", rs.getDouble("FixedTaxAmount"));
                        data.put("excess", rs.getDouble("ExcessOverAmount"));
                        taxSchedule.put(rs.getDouble("MonthlyRateMin"), data);
                    }
                }
                
                // --- 3. CALCULATION AND INSERTION LOOP ---
                String selectEmployeeSql = "SELECT e.EmployeeID, s.BasicSalary, s.HourlyRate FROM employee e JOIN salary s ON e.EmployeeID = s.EmployeeID WHERE e.IsDeleted = 0 AND s.IsDeleted = 0";
                String selectAttendanceSql = "SELECT SUM(HoursWorked) AS TotalHoursWorked, SUM(OvertimeHours) AS TotalOvertimeHours FROM attendance WHERE EmployeeID = ? AND Date BETWEEN ? AND ? AND IsDeleted = 0";
                String selectAllowancesSql = "SELECT at.AllowanceName, a.Amount FROM allowance a JOIN allowancetype at ON a.AllowanceTypeID = at.AllowanceTypeID WHERE a.EmployeeID = ? AND a.EffectiveDate <= ? AND a.IsDeleted = 0";
                String insertPayslipSql = "INSERT INTO payslip (EmployeeID, PayPeriodID, GrossPay, TotalDeductions, TotalAllowances, NetPay, HoursWorked, TotalOvertimeHours, GenerateDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                String insertPayslipDetailSql = "INSERT INTO payslipdetail (PayslipID, Description, Amount, Type) VALUES (?, ?, ?, ?)";
                String insertDeductionSql = "INSERT INTO deduction (DeductionTypeID, EmployeeID, Amount, PayPeriodStartDate, PayperiodEndDate) VALUES (?, ?, ?, ?, ?)";
                String insertTaxComputationSql = "INSERT INTO taxcomputation (EmployeeID, PayslipID, TaxableIncome, TaxRate, TaxAmount, TaxPeriodStartDate, TaxPeriodEndDate) VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement pstmtEmployeeData = conn.prepareStatement(selectEmployeeSql);
                     PreparedStatement pstmtAttendance = conn.prepareStatement(selectAttendanceSql);
                     PreparedStatement pstmtIndividualAllowances = conn.prepareStatement(selectAllowancesSql);
                     PreparedStatement pstmtPayslip = conn.prepareStatement(insertPayslipSql, Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement pstmtPayslipDetail = conn.prepareStatement(insertPayslipDetailSql);
                     PreparedStatement pstmtDeduction = conn.prepareStatement(insertDeductionSql);
                     PreparedStatement pstmtTaxComputation = conn.prepareStatement(insertTaxComputationSql)) {
                    
                    ResultSet employeeRs = pstmtEmployeeData.executeQuery();
                    while (employeeRs.next()) {
                        int employeeId = employeeRs.getInt("EmployeeID");
                        double basicSalary = employeeRs.getDouble("BasicSalary");
                        double hourlyRate = employeeRs.getDouble("HourlyRate");

                        pstmtAttendance.setInt(1, employeeId);
                        pstmtAttendance.setDate(2, Date.valueOf(startDate));
                        pstmtAttendance.setDate(3, Date.valueOf(endDate));
                        
                        double totalHoursWorked = 0.0;
                        double totalOvertimeHours = 0.0;
                        try (ResultSet attendanceRs = pstmtAttendance.executeQuery()) {
                            if (attendanceRs.next()) {
                                totalHoursWorked = attendanceRs.getDouble("TotalHoursWorked");
                                totalOvertimeHours = attendanceRs.getDouble("TotalOvertimeHours");
                            }
                        }
                        
                        if (totalHoursWorked == 0.0 && totalOvertimeHours == 0.0) continue;

                        double grossPay = totalHoursWorked * hourlyRate;
                        
                        double totalAllowances = 0.0;
                        Map<String, Double> employeeAllowances = new HashMap<>();
                        pstmtIndividualAllowances.setInt(1, employeeId);
                        pstmtIndividualAllowances.setDate(2, Date.valueOf(endDate));
                        try (ResultSet allowanceRs = pstmtIndividualAllowances.executeQuery()) {
                            while (allowanceRs.next()) {
                                String name = allowanceRs.getString("AllowanceName");
                                double amount = allowanceRs.getDouble("Amount");
                                employeeAllowances.put(name, amount);
                                totalAllowances += amount;
                            }
                        }
                        
                        double sssContribution = 0.0;
                        for (Map.Entry<Double, Map<String, Double>> entry : sssSchedule.entrySet()) {
                            if (basicSalary >= entry.getKey() && (entry.getValue().get("end") == 0.0 || basicSalary <= entry.getValue().get("end"))) {
                                sssContribution = entry.getValue().get("contribution");
                                break;
                            }
                        }
                        
                        double philhealthContribution = 0.0;
                        for (Map.Entry<Double, Map<String, Double>> entry : philhealthSchedule.entrySet()) {
                            if (basicSalary >= entry.getKey() && (entry.getValue().get("max") == 0.0 || basicSalary <= entry.getValue().get("max"))) {
                                double totalPremium = basicSalary * entry.getValue().get("rate");
                                philhealthContribution = Math.min(totalPremium, entry.getValue().get("maxPremium")) / 2.0;
                                break;
                            }
                        }

                        double pagibigContribution = 0.0;
                        for (Map.Entry<Double, Map<String, Double>> entry : pagibigSchedule.entrySet()) {
                            if (basicSalary >= entry.getKey() && (entry.getValue().get("max") == 0.0 || basicSalary <= entry.getValue().get("max"))) {
                                pagibigContribution = Math.min(100.0, basicSalary * entry.getValue().get("employeeRate"));
                                break;
                            }
                        }
                        
                        double taxableIncome = grossPay - sssContribution - philhealthContribution - pagibigContribution;
                        taxableIncome = Math.max(0.0, taxableIncome);
                        
                        double withholdingTax = 0.0;
                        double taxRateForComputationTable = 0.0;
                        for (Map.Entry<Double, Map<String, Double>> entry : taxSchedule.entrySet()) {
                            if (taxableIncome >= entry.getKey() && (entry.getValue().get("max") == 0.0 || taxableIncome <= entry.getValue().get("max"))) {
                                withholdingTax = entry.getValue().get("fixed") + (taxableIncome - entry.getValue().get("excess")) * entry.getValue().get("percentage");
                                taxRateForComputationTable = entry.getValue().get("percentage");
                                break;
                            }
                        }
                        
                        double totalDeductions = sssContribution + philhealthContribution + pagibigContribution + withholdingTax;
                        double netPay = grossPay + totalAllowances - totalDeductions;
                        
                        pstmtPayslip.setInt(1, employeeId);
                        pstmtPayslip.setInt(2, payPeriodId);
                        pstmtPayslip.setDouble(3, grossPay);
                        pstmtPayslip.setDouble(4, totalDeductions);
                        pstmtPayslip.setDouble(5, totalAllowances);
                        pstmtPayslip.setDouble(6, netPay);
                        pstmtPayslip.setDouble(7, totalHoursWorked);
                        pstmtPayslip.setDouble(8, totalOvertimeHours);
                        pstmtPayslip.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
                        pstmtPayslip.executeUpdate();

                        int currentPayslipId;
                        try (ResultSet generatedKeys = pstmtPayslip.getGeneratedKeys()) {
                            if (generatedKeys.next()) currentPayslipId = generatedKeys.getInt(1);
                            else continue;
                        }

                        // Batch Payslip Details
                        pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, "Gross Pay"); pstmtPayslipDetail.setDouble(3, grossPay); pstmtPayslipDetail.setString(4, "Earning"); pstmtPayslipDetail.addBatch();
                        if (totalOvertimeHours > 0) {
                            double overtimePay = totalOvertimeHours * hourlyRate * 1.25;
                            pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, "Overtime Pay"); pstmtPayslipDetail.setDouble(3, overtimePay); pstmtPayslipDetail.setString(4, "Earning"); pstmtPayslipDetail.addBatch();
                        }
                        for (Map.Entry<String, Double> entry : employeeAllowances.entrySet()) {
                            pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, entry.getKey()); pstmtPayslipDetail.setDouble(3, entry.getValue()); pstmtPayslipDetail.setString(4, "Allowance"); pstmtPayslipDetail.addBatch();
                        }
                        if(sssContribution > 0) { pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, "SSS Contribution"); pstmtPayslipDetail.setDouble(3, sssContribution); pstmtPayslipDetail.setString(4, "Deduction"); pstmtPayslipDetail.addBatch(); }
                        if(philhealthContribution > 0) { pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, "Philhealth Contribution"); pstmtPayslipDetail.setDouble(3, philhealthContribution); pstmtPayslipDetail.setString(4, "Deduction"); pstmtPayslipDetail.addBatch(); }
                        if(pagibigContribution > 0) { pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, "Pag-IBIG Contribution"); pstmtPayslipDetail.setDouble(3, pagibigContribution); pstmtPayslipDetail.setString(4, "Deduction"); pstmtPayslipDetail.addBatch(); }
                        if(withholdingTax > 0) { pstmtPayslipDetail.setInt(1, currentPayslipId); pstmtPayslipDetail.setString(2, "Withholding Tax"); pstmtPayslipDetail.setDouble(3, withholdingTax); pstmtPayslipDetail.setString(4, "Tax"); pstmtPayslipDetail.addBatch(); }
                        pstmtPayslipDetail.executeBatch();
                        
                        // Add to overall Deduction and Tax batches
                        if(sssContribution > 0) { pstmtDeduction.setInt(1, deductionTypeMap.get("SSS Contribution")); pstmtDeduction.setInt(2, employeeId); pstmtDeduction.setDouble(3, sssContribution); pstmtDeduction.setDate(4, Date.valueOf(startDate)); pstmtDeduction.setDate(5, Date.valueOf(endDate)); pstmtDeduction.addBatch(); }
                        if(philhealthContribution > 0) { pstmtDeduction.setInt(1, deductionTypeMap.get("Philhealth Contribution")); pstmtDeduction.setInt(2, employeeId); pstmtDeduction.setDouble(3, philhealthContribution); pstmtDeduction.setDate(4, Date.valueOf(startDate)); pstmtDeduction.setDate(5, Date.valueOf(endDate)); pstmtDeduction.addBatch(); }
                        if(pagibigContribution > 0) { pstmtDeduction.setInt(1, deductionTypeMap.get("Pag-IBIG Contribution")); pstmtDeduction.setInt(2, employeeId); pstmtDeduction.setDouble(3, pagibigContribution); pstmtDeduction.setDate(4, Date.valueOf(startDate)); pstmtDeduction.setDate(5, Date.valueOf(endDate)); pstmtDeduction.addBatch(); }
                        if(withholdingTax > 0) { pstmtDeduction.setInt(1, deductionTypeMap.get("Withholding Tax")); pstmtDeduction.setInt(2, employeeId); pstmtDeduction.setDouble(3, withholdingTax); pstmtDeduction.setDate(4, Date.valueOf(startDate)); pstmtDeduction.setDate(5, Date.valueOf(endDate)); pstmtDeduction.addBatch(); }
                        
                        if (taxableIncome > 0 || withholdingTax > 0) {
                            pstmtTaxComputation.setInt(1, employeeId);
                            pstmtTaxComputation.setInt(2, currentPayslipId);
                            pstmtTaxComputation.setDouble(3, taxableIncome);
                            pstmtTaxComputation.setDouble(4, taxRateForComputationTable);
                            pstmtTaxComputation.setDouble(5, withholdingTax);
                            pstmtTaxComputation.setDate(6, Date.valueOf(startDate));
                            pstmtTaxComputation.setDate(7, Date.valueOf(endDate));
                            pstmtTaxComputation.addBatch();
                        }
                    } // End of employee while loop

                    pstmtDeduction.executeBatch();
                    pstmtTaxComputation.executeBatch();
                }

                // --- 4. FINALIZE: MARK PERIOD AS PROCESSED ---
                String updatePayPeriodSql = "UPDATE payperiod SET IsProcessed = ?, ProcessedDate = ? WHERE PayPeriodID = ?";
                try (PreparedStatement pstmtUpdatePayPeriod = conn.prepareStatement(updatePayPeriodSql)) {
                    pstmtUpdatePayPeriod.setBoolean(1, true);
                    pstmtUpdatePayPeriod.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    pstmtUpdatePayPeriod.setInt(3, payPeriodId);
                    pstmtUpdatePayPeriod.executeUpdate();
                }

                // --- 5. COMMIT: SAVE ALL CHANGES AT ONCE ---
                conn.commit();
                System.out.println("Payroll transaction committed successfully.");

            } catch (SQLException e) {
                // --- ROLLBACK: UNDO ALL CHANGES ON ANY ERROR ---
                System.err.println("SQL error occurred during payroll calculation. Rolling back transaction.");
                conn.rollback();
                e.printStackTrace();
                throw e; 
            }
        }
    }
}

//package dao;
//
//import java.sql.*;
//import java.time.LocalDate;
//import java.util.HashMap;
//import java.util.Map;
//
//import data.DBConnection; //
//
//public class PayrollDAO {
//
//    public void calculatePayroll(String periodName, LocalDate startDate, LocalDate endDate) throws SQLException {
//        // Use the transactional connection here for the main payroll logic
//        try (Connection conn = DBConnection.getTransactionalConnection()) { //
//            // autoCommit is already false for connections from getTransactionalConnection()
//
//            int payPeriodId = -1;
//            boolean isPeriodProcessed = false;
//
//            // --- Check if PayPeriod exists and is already processed ---
//            String selectPayPeriodSql = "SELECT PayPeriodID, IsProcessed FROM payperiod WHERE StartDate = ? AND EndDate = ?"; //
//            try (PreparedStatement pstmtSelectPayPeriod = conn.prepareStatement(selectPayPeriodSql)) { //
//                pstmtSelectPayPeriod.setDate(1, Date.valueOf(startDate)); //
//                pstmtSelectPayPeriod.setDate(2, Date.valueOf(endDate)); //
//                ResultSet rs = pstmtSelectPayPeriod.executeQuery(); //
//                if (rs.next()) { //
//                    payPeriodId = rs.getInt("PayPeriodID"); //
//                    isPeriodProcessed = rs.getBoolean("IsProcessed"); //
//                }
//                rs.close(); //
//            }
//
//            if (isPeriodProcessed) { //
//                System.out.println("Payroll for " + periodName + " (" + startDate + " to " + endDate + ") has already been processed and will not be re-calculated."); //
//                return; // Exit the method if already processed
//            }
//
//            // --- If not processed, proceed to insert/update PayPeriod record ---
//            String insertPayPeriodSql = "INSERT INTO payperiod (StartDate, EndDate, PeriodName, IsProcessed, PaymentDate) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE IsProcessed=VALUES(IsProcessed), PaymentDate=VALUES(PaymentDate)"; //
//            try (PreparedStatement pstmtInsertPayPeriod = conn.prepareStatement(insertPayPeriodSql, Statement.RETURN_GENERATED_KEYS)) { //
//
//                pstmtInsertPayPeriod.setDate(1, Date.valueOf(startDate)); //
//                pstmtInsertPayPeriod.setDate(2, Date.valueOf(endDate)); //
//                pstmtInsertPayPeriod.setString(3, periodName); //
//                pstmtInsertPayPeriod.setBoolean(4, false); // Explicitly set to false before calculation
//                pstmtInsertPayPeriod.setDate(5, Date.valueOf(endDate.plusDays(5))); // Example payment date 5 days after end of period
//                pstmtInsertPayPeriod.executeUpdate(); //
//
//                if (payPeriodId == -1) { // Only get generated keys if it was a new insert
//                    try (ResultSet generatedKeys = pstmtInsertPayPeriod.getGeneratedKeys()) { //
//                        if (generatedKeys.next()) { //
//                            payPeriodId = generatedKeys.getInt(1); //
//                        } else {
//                            // This case should ideally not happen if UNIQUE KEY on StartDate/EndDate is set
//                            throw new SQLException("Failed to retrieve PayPeriodID after new insert for " + periodName); //
//                        }
//                    }
//                }
//                // conn.commit(); // Removed this commit, as the transaction is managed at the end of calculatePayroll
//                System.out.println("PayPeriod ID being used/updated: " + payPeriodId); //
//            } catch (SQLException e) { //
//                conn.rollback(); //
//                System.err.println("Error managing PayPeriod: " + e.getMessage()); //
//                throw e; //
//            }
//
//            // --- Delete existing payroll data for this period to prevent duplicates for current run ---
//            try (PreparedStatement pstmtDeletePayslipDetail = conn.prepareStatement("DELETE FROM payslipdetail WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE PayPeriodID = ?)"); //
//                 PreparedStatement pstmtDeleteTaxComputation = conn.prepareStatement("DELETE FROM taxcomputation WHERE PayslipID IN (SELECT PayslipID FROM payslip WHERE PayPeriodID = ?)"); //
//                 PreparedStatement pstmtDeleteDeduction = conn.prepareStatement("DELETE FROM deduction WHERE PayPeriodStartDate = ? AND PayperiodEndDate = ?"); //
//                 PreparedStatement pstmtDeletePayslip = conn.prepareStatement("DELETE FROM payslip WHERE PayPeriodID = ?")) { //
//
//                System.out.println("Deleting any partial or old payroll data for PayPeriodID: " + payPeriodId + " (Dates: " + startDate + " to " + endDate + ") before re-calculating."); //
//
//                // Disable foreign key checks temporarily for cascade deletion safety
//                try (Statement stmt = conn.createStatement()) { //
//                    stmt.execute("SET FOREIGN_KEY_CHECKS = 0;"); //
//                }
//
//                pstmtDeletePayslipDetail.setInt(1, payPeriodId); //
//                pstmtDeletePayslipDetail.executeUpdate(); //
//
//                pstmtDeleteTaxComputation.setInt(1, payPeriodId); //
//                pstmtDeleteTaxComputation.executeUpdate(); //
//
//                pstmtDeleteDeduction.setDate(1, Date.valueOf(startDate)); //
//                pstmtDeleteDeduction.setDate(2, Date.valueOf(endDate)); //
//                pstmtDeleteDeduction.executeUpdate(); //
//
//                pstmtDeletePayslip.setInt(1, payPeriodId); //
//                pstmtDeletePayslip.executeUpdate(); //
//
//                // Re-enable foreign key checks
//                try (Statement stmt = conn.createStatement()) { //
//                    stmt.execute("SET FOREIGN_KEY_CHECKS = 1;"); //
//                }
//                // conn.commit(); // Removed this commit, as the transaction is managed at the end of calculatePayroll
//                System.out.println("Previous payroll data for this period successfully cleared."); //
//
//            } catch (SQLException e) { //
//                conn.rollback(); //
//                System.err.println("Error deleting existing payroll data: " + e.getMessage()); //
//                throw e; //
//            }
//
//
//            // --- 2. Ensure Deduction Types Exist & Fetch IDs ---
//            Map<String, Integer> deductionTypeMap = new HashMap<>(); //
//            String insertDeductionTypeSql = "INSERT IGNORE INTO deductiontype (DeductionName) VALUES (?)"; //
//            try (PreparedStatement pstmt = conn.prepareStatement(insertDeductionTypeSql)) { //
//                pstmt.setString(1, "SSS Contribution"); pstmt.addBatch(); //
//                pstmt.setString(1, "Philhealth Contribution"); pstmt.addBatch(); //
//                pstmt.setString(1, "Pag-IBIG Contribution"); pstmt.addBatch(); //
//                pstmt.setString(1, "Withholding Tax"); pstmt.addBatch(); //
//                pstmt.executeBatch(); //
//                // conn.commit(); // Removed this commit
//            }
//            try (Statement stmt = conn.createStatement()) { //
//                ResultSet rs = stmt.executeQuery("SELECT DeductionTypeID, DeductionName FROM deductiontype"); //
//                while (rs.next()) { //
//                    deductionTypeMap.put(rs.getString("DeductionName"), rs.getInt("DeductionTypeID")); //
//                }
//            }
//
//            // --- 3. Fetch all necessary Contribution/Tax Schedules (Caching for efficiency) ---
//            Map<Double, Map<String, Double>> sssSchedule = new HashMap<>(); //
//            try (Statement stmt = conn.createStatement()) { //
//                ResultSet rs = stmt.executeQuery("SELECT CompensationRangeStart, CompensationRangeEnd, ContributionAmount FROM sss_contribution_schedule ORDER BY CompensationRangeStart ASC"); //
//                while (rs.next()) { //
//                    double start = rs.getDouble("CompensationRangeStart"); //
//                    // getDouble returns 0.0 for NULL. We'll use 0.0 to signify no upper bound if it's explicitly 0 in DB or NULL.
//                    double end = rs.getDouble("CompensationRangeEnd"); //
//                    double contribution = rs.getDouble("ContributionAmount"); //
//                    Map<String, Double> rangeData = new HashMap<>(); //
//                    rangeData.put("end", end); //
//                    rangeData.put("contribution", contribution); //
//                    sssSchedule.put(start, rangeData); //
//                }
//            }
//
//            Map<Double, Map<String, Double>> philhealthSchedule = new HashMap<>(); //
//            try (Statement stmt = conn.createStatement()) { //
//                ResultSet rs = stmt.executeQuery("SELECT MonthlyBasicSalaryMin, MonthlyBasicSalaryMax, PremiumRate, MaxMonthlyPremium FROM philhealth_contribution_schedule ORDER BY MonthlyBasicSalaryMin ASC"); //
//                while (rs.next()) { //
//                    double min = rs.getDouble("MonthlyBasicSalaryMin"); //
//                    double max = rs.getDouble("MonthlyBasicSalaryMax"); //
//                    double rate = rs.getDouble("PremiumRate"); //
//                    double maxPremium = rs.getDouble("MaxMonthlyPremium"); //
//                    Map<String, Double> rangeData = new HashMap<>(); //
//                    rangeData.put("max", max); //
//                    rangeData.put("rate", rate); //
//                    rangeData.put("maxPremium", maxPremium); //
//                    philhealthSchedule.put(min, rangeData); //
//                }
//            }
//
//            Map<Double, Map<String, Double>> pagibigSchedule = new HashMap<>(); //
//            try (Statement stmt = conn.createStatement()) { //
//                ResultSet rs = stmt.executeQuery("SELECT MonthlyBasicSalaryMin, MonthlyBasicSalaryMax, EmployeeContributionRate, EmployerContributionRate, TotalContributionRate FROM pagibig_contribution_schedule ORDER BY MonthlyBasicSalaryMin ASC"); //
//                while (rs.next()) { //
//                    double min = rs.getDouble("MonthlyBasicSalaryMin"); //
//                    double max = rs.getDouble("MonthlyBasicSalaryMax"); //
//                    double empRate = rs.getDouble("EmployeeContributionRate"); //
//                    double erRate = rs.getDouble("EmployerContributionRate"); //
//                    double totalRate = rs.getDouble("TotalContributionRate"); //
//                    Map<String, Double> rangeData = new HashMap<>(); //
//                    rangeData.put("max", max); //
//                    rangeData.put("employeeRate", empRate); //
//                    rangeData.put("employerRate", erRate); //
//                    rangeData.put("totalRate", totalRate); //
//                    pagibigSchedule.put(min, rangeData); //
//                }
//            }
//
//            Map<Double, Map<String, Double>> taxSchedule = new HashMap<>(); //
//            try (Statement stmt = conn.createStatement()) { //
//                ResultSet rs = stmt.executeQuery("SELECT MonthlyRateMin, MonthlyRateMax, TaxPercentage, FixedTaxAmount, ExcessOverAmount FROM withholding_tax_schedule ORDER BY MonthlyRateMin ASC"); //
//                while (rs.next()) { //
//                    double min = rs.getDouble("MonthlyRateMin"); //
//                    double max = rs.getDouble("MonthlyRateMax"); //
//                    double percentage = rs.getDouble("TaxPercentage"); //
//                    double fixed = rs.getDouble("FixedTaxAmount"); //
//                    double excess = rs.getDouble("ExcessOverAmount"); //
//
//                    Map<String, Double> taxData = new HashMap<>(); //
//                    taxData.put("max", max); //
//                    taxData.put("percentage", percentage); //
//                    taxData.put("fixed", fixed); //
//                    taxData.put("excess", excess); //
//                    taxSchedule.put(min, taxData); //
//                }
//            }
//
//            // --- 4. Fetch Employee Data and loop through each for payroll calculation ---
//            String selectEmployeeDataSql = "SELECT e.EmployeeID, s.BasicSalary, s.HourlyRate " + //
//                    "FROM employee e JOIN salary s ON e.EmployeeID = s.EmployeeID " + //
//                    "WHERE e.IsDeleted = 0 AND s.IsDeleted = 0"; //
//
//            String selectAttendanceSql = "SELECT SUM(HoursWorked) AS TotalHoursWorked, SUM(OvertimeHours) AS TotalOvertimeHours " + //
//                    "FROM attendance " + //
//                    "WHERE EmployeeID = ? AND Date BETWEEN ? AND ? AND IsDeleted = 0"; //
//
//            String selectIndividualAllowancesSql = "SELECT at.AllowanceName, a.Amount " + //
//                                                  "FROM allowance a JOIN allowancetype at ON a.AllowanceTypeID = at.AllowanceTypeID " + //
//                                                  "WHERE a.EmployeeID = ? AND a.EffectiveDate <= ? AND a.IsDeleted = 0"; //
//
//            String insertPayslipSql = "INSERT INTO payslip (EmployeeID, PayPeriodID, GrossPay, TotalDeductions, TotalAllowances, NetPay, HoursWorked, TotalOvertimeHours, GenerateDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; //
//            String insertPayslipDetailSql = "INSERT INTO payslipdetail (PayslipID, Description, Amount, Type) VALUES (?, ?, ?, ?)"; //
//            String insertDeductionSql = "INSERT INTO deduction (DeductionTypeID, EmployeeID, Amount, PayPeriodStartDate, PayperiodEndDate) VALUES (?, ?, ?, ?, ?)"; //
//            String insertTaxComputationSql = "INSERT INTO taxcomputation (EmployeeID, PayslipID, TaxableIncome, TaxRate, TaxAmount, TaxPeriodStartDate, TaxPeriodEndDate) VALUES (?, ?, ?, ?, ?, ?, ?)"; //
//
//
//            try (PreparedStatement pstmtEmployeeData = conn.prepareStatement(selectEmployeeDataSql); //
//                 PreparedStatement pstmtAttendance = conn.prepareStatement(selectAttendanceSql); //
//                 PreparedStatement pstmtIndividualAllowances = conn.prepareStatement(selectIndividualAllowancesSql); //
//                 PreparedStatement pstmtPayslip = conn.prepareStatement(insertPayslipSql, Statement.RETURN_GENERATED_KEYS); //
//                 PreparedStatement pstmtPayslipDetail = conn.prepareStatement(insertPayslipDetailSql); //
//                 PreparedStatement pstmtDeduction = conn.prepareStatement(insertDeductionSql); //
//                 PreparedStatement pstmtTaxComputation = conn.prepareStatement(insertTaxComputationSql)) { //
//
//                ResultSet employeeRs = pstmtEmployeeData.executeQuery(); //
//
//                while (employeeRs.next()) { //
//                    int employeeId = employeeRs.getInt("EmployeeID"); //
//                    double basicSalary = employeeRs.getDouble("BasicSalary"); //
//                    double hourlyRate = employeeRs.getDouble("HourlyRate"); //
//
//                    System.out.println("Processing payroll for Employee ID: " + employeeId); //
//
//                    pstmtAttendance.setInt(1, employeeId); //
//                    pstmtAttendance.setDate(2, Date.valueOf(startDate)); //
//                    pstmtAttendance.setDate(3, Date.valueOf(endDate)); //
//                    ResultSet attendanceRs = pstmtAttendance.executeQuery(); //
//
//                    double totalHoursWorked = 0.0; //
//                    double totalOvertimeHours = 0.0; //
//                    if (attendanceRs.next()) { //
//                        totalHoursWorked = attendanceRs.getDouble("TotalHoursWorked"); //
//                        totalOvertimeHours = attendanceRs.getDouble("TotalOvertimeHours"); //
//                    }
//                    attendanceRs.close(); //
//
//                    if (totalHoursWorked == 0.0 && totalOvertimeHours == 0.0) { //
//                        System.out.println("Skipping payroll for Employee ID: " + employeeId + " as no hours worked or overtime hours were recorded for the period."); //
//                        continue; // Skip to the next employee
//                    }
//
//                    double grossPay; //
//                    double effectiveHourlyRate = hourlyRate; //
//                    if (effectiveHourlyRate == 0.0) { // Check if hourly rate is 0.0 (null from DB is typically 0 for getDouble)
//                        double standardMonthlyWorkingHours = 160.0; //
//                        if (basicSalary > 0.0) { //
//                            effectiveHourlyRate = basicSalary / standardMonthlyWorkingHours; //
//                        } else { //
//                            effectiveHourlyRate = 0.0; //
//                        }
//                    }
//                    grossPay = totalHoursWorked * effectiveHourlyRate; //
//
//
//                    pstmtIndividualAllowances.setInt(1, employeeId); //
//                    pstmtIndividualAllowances.setDate(2, Date.valueOf(endDate)); //
//                    ResultSet individualAllowanceRs = pstmtIndividualAllowances.executeQuery(); //
//                    Map<String, Double> employeeAllowances = new HashMap<>(); //
//                    double totalAllowances = 0.0; //
//
//                    while (individualAllowanceRs.next()) { //
//                        String allowanceName = individualAllowanceRs.getString("AllowanceName"); //
//                        double allowanceAmount = individualAllowanceRs.getDouble("Amount"); //
//                        if (allowanceAmount != 0.0) { // Check if allowanceAmount is not 0.0
//                            employeeAllowances.put(allowanceName, allowanceAmount); //
//                            totalAllowances += allowanceAmount; //
//                        }
//                    }
//                    individualAllowanceRs.close(); //
//
//
//                    // --- Calculate Deductions ---
//                    double totalDeductions = 0.0; //
//                    double sssContribution = 0.0; //
//                    double philhealthContribution = 0.0; //
//                    double pagibigContribution = 0.0; //
//                    double withholdingTax = 0.0; //
//                    double taxRateForComputationTable = 0.0; //
//
//                    // SSS Contribution calculation (based on monthly basic salary)
//                    for (Map.Entry<Double, Map<String, Double>> entry : sssSchedule.entrySet()) { //
//                        double rangeStart = entry.getKey(); //
//                        double rangeEnd = entry.getValue().get("end"); //
//                        double contribution = entry.getValue().get("contribution"); //
//
//                        // If rangeEnd from DB is 0.0, it might represent a NULL (no upper bound)
//                        if (basicSalary >= rangeStart && (rangeEnd == 0.0 || basicSalary <= rangeEnd)) { //
//                            sssContribution = contribution; //
//                            break; //
//                        }
//                    }
//                    totalDeductions += sssContribution; //
//                    // Insert SSS Contribution into 'deduction' table
//                    if (sssContribution > 0.0 && deductionTypeMap.containsKey("SSS Contribution")) { //
//                        pstmtDeduction.setInt(1, deductionTypeMap.get("SSS Contribution")); //
//                        pstmtDeduction.setInt(2, employeeId); //
//                        pstmtDeduction.setDouble(3, sssContribution); //
//                        pstmtDeduction.setDate(4, Date.valueOf(startDate)); //
//                        pstmtDeduction.setDate(5, Date.valueOf(endDate)); //
//                        pstmtDeduction.addBatch(); //
//                    }
//
//
//                    // Philhealth Contribution calculation (based on monthly basic salary)
//                    for (Map.Entry<Double, Map<String, Double>> entry : philhealthSchedule.entrySet()) { //
//                        double min = entry.getKey(); //
//                        double max = entry.getValue().get("max"); //
//                        double rate = entry.getValue().get("rate"); //
//                        double maxPremium = entry.getValue().get("maxPremium"); //
//
//                        if (basicSalary >= min && (max == 0.0 || basicSalary <= max)) { //
//                            double calculatedTotalPremium = basicSalary * rate; //
//                            double cappedTotalPremium = Math.min(calculatedTotalPremium, maxPremium); // Using Math.min
//                            philhealthContribution = cappedTotalPremium / 2.0; // Employee's 50% share
//                            break; //
//                        }
//                    }
//                    totalDeductions += philhealthContribution; //
//                    // Insert Philhealth Contribution into 'deduction' table
//                    if (philhealthContribution > 0.0 && deductionTypeMap.containsKey("Philhealth Contribution")) { //
//                        pstmtDeduction.setInt(1, deductionTypeMap.get("Philhealth Contribution")); //
//                        pstmtDeduction.setInt(2, employeeId); //
//                        pstmtDeduction.setDouble(3, philhealthContribution); //
//                        pstmtDeduction.setDate(4, Date.valueOf(startDate)); //
//                        pstmtDeduction.setDate(5, Date.valueOf(endDate)); //
//                        pstmtDeduction.addBatch(); //
//                    }
//
//
//                    // Pag-IBIG Contribution calculation (based on monthly basic salary)
//                    for (Map.Entry<Double, Map<String, Double>> entry : pagibigSchedule.entrySet()) { //
//                        double min = entry.getKey(); //
//                        double max = entry.getValue().get("max"); //
//                        double employeeRate = entry.getValue().get("employeeRate"); //
//
//                        if (basicSalary >= min && (max == 0.0 || basicSalary <= max)) { //
//                            pagibigContribution = basicSalary * employeeRate; //
//                            // Pag-IBIG has specific caps (e.g., max 100 for employee)
//                            if (pagibigContribution > 100.0) { //
//                                pagibigContribution = 100.0; //
//                            }
//                            break; //
//                        }
//                    }
//                    totalDeductions += pagibigContribution; //
//                    // Insert Pag-IBIG Contribution into 'deduction' table
//                    if (pagibigContribution > 0.0 && deductionTypeMap.containsKey("Pag-IBIG Contribution")) { //
//                        pstmtDeduction.setInt(1, deductionTypeMap.get("Pag-IBIG Contribution")); //
//                        pstmtDeduction.setInt(2, employeeId); //
//                        pstmtDeduction.setDouble(3, pagibigContribution); //
//                        pstmtDeduction.setDate(4, Date.valueOf(startDate)); //
//                        pstmtDeduction.setDate(5, Date.valueOf(endDate)); //
//                        pstmtDeduction.addBatch(); //
//                    }
//
//                    // Withholding Tax calculation (based on Taxable Income = Gross Pay - SSS - Philhealth - Pag-IBIG)
//                    double taxableIncome = grossPay - sssContribution - philhealthContribution - pagibigContribution; //
//                    taxableIncome = Math.max(0.0, taxableIncome); // Ensure taxable income is not negative
//
//                    for (Map.Entry<Double, Map<String, Double>> entry : taxSchedule.entrySet()) { //
//                        double min = entry.getKey(); //
//                        double max = entry.getValue().get("max"); //
//                        double percentage = entry.getValue().get("percentage"); //
//                        double fixed = entry.getValue().get("fixed"); //
//                        double excess = entry.getValue().get("excess"); //
//
//                        if (taxableIncome >= min && (max == 0.0 || taxableIncome <= max)) { //
//                            withholdingTax = fixed + (taxableIncome - excess) * percentage; //
//                            taxRateForComputationTable = percentage; //
//                            break; //
//                        }
//                    }
//                    totalDeductions += withholdingTax; //
//
//                    // Calculate Net Pay
//                    double netPay = grossPay + totalAllowances - totalDeductions; //
//
//                    // --- 5. Insert into Payslip ---
//                    pstmtPayslip.setInt(1, employeeId); //
//                    pstmtPayslip.setInt(2, payPeriodId); //
//                    pstmtPayslip.setDouble(3, grossPay); //
//                    pstmtPayslip.setDouble(4, totalDeductions); //
//                    pstmtPayslip.setDouble(5, totalAllowances); // Still populate total allowances for the Payslip summary
//                    pstmtPayslip.setDouble(6, netPay); //
//                    pstmtPayslip.setDouble(7, totalHoursWorked); //
//                    pstmtPayslip.setDouble(8, totalOvertimeHours); //
//                    pstmtPayslip.setTimestamp(9, new Timestamp(System.currentTimeMillis())); // Generation date/time
//                    pstmtPayslip.executeUpdate(); //
//
//                    int payslipId; //
//                    try (ResultSet generatedKeys = pstmtPayslip.getGeneratedKeys()) { //
//                        if (generatedKeys.next()) { //
//                            payslipId = generatedKeys.getInt(1); //
//                        } else {
//                            System.err.println("Warning: Failed to get PayslipID for EmployeeID: " + employeeId + ". Payslip details will not be added."); //
//                            continue; // Skip payslip detail insertion for this employee
//                        }
//                    }
//
//                    // --- Insert Withholding Tax into 'deduction' table as well ---
//                    // This is being added to the batch for pstmtDeduction
//                    if (withholdingTax > 0.0 && deductionTypeMap.containsKey("Withholding Tax")) { //
//                        pstmtDeduction.setInt(1, deductionTypeMap.get("Withholding Tax")); //
//                        pstmtDeduction.setInt(2, employeeId); //
//                        pstmtDeduction.setDouble(3, withholdingTax); //
//                        pstmtDeduction.setDate(4, Date.valueOf(startDate)); //
//                        pstmtDeduction.setDate(5, Date.valueOf(endDate)); //
//                        pstmtDeduction.addBatch(); //
//                    }
//
//                    // --- Insert into TaxComputation table ---
//                    // This is being added to the batch for pstmtTaxComputation
//                    if (taxableIncome > 0.0 || withholdingTax > 0.0) { // Only insert if there's tax info
//                        pstmtTaxComputation.setInt(1, employeeId); //
//                        pstmtTaxComputation.setInt(2, payslipId); //
//                        pstmtTaxComputation.setDouble(3, taxableIncome); //
//                        pstmtTaxComputation.setDouble(4, taxRateForComputationTable); // Use the percentage for TaxRate
//                        pstmtTaxComputation.setDouble(5, withholdingTax); //
//                        pstmtTaxComputation.setDate(6, Date.valueOf(startDate)); //
//                        pstmtTaxComputation.setDate(7, Date.valueOf(endDate)); //
//                        pstmtTaxComputation.addBatch(); //
//                    }
//
//                    // --- 6. Insert Payslip Details ---
//                    if (grossPay > 0.0) { //
//                        pstmtPayslipDetail.setInt(1, payslipId); //
//                        pstmtPayslipDetail.setString(2, "Gross Pay (Regular Hours)"); //
//                        pstmtPayslipDetail.setDouble(3, grossPay); //
//                        pstmtPayslipDetail.setString(4, "Earning"); //
//                        pstmtPayslipDetail.addBatch(); //
//                    }
//
//                    // If overtime pay is a separate earning, add it here
//                    if (totalOvertimeHours > 0.0 && effectiveHourlyRate > 0.0) { //
//                        double overtimePayRate = 1.25; // Example: 1.25x for overtime
//                        double calculatedOvertimePay = totalOvertimeHours * effectiveHourlyRate * overtimePayRate; //
//                        if (calculatedOvertimePay > 0.0) { //
//                            pstmtPayslipDetail.setInt(1, payslipId); //
//                            pstmtPayslipDetail.setString(2, "Overtime Pay"); //
//                            pstmtPayslipDetail.setDouble(3, calculatedOvertimePay); //
//                            pstmtPayslipDetail.setString(4, "Earning"); //
//                            pstmtPayslipDetail.addBatch(); //
//                        }
//                    }
//
//                    // --- Insert Individual Allowances into Payslip Details ---
//                    for (Map.Entry<String, Double> entry : employeeAllowances.entrySet()) { //
//                        String allowanceName = entry.getKey(); //
//                        double allowanceAmount = entry.getValue(); //
//                        if (allowanceAmount > 0.0) { //
//                            pstmtPayslipDetail.setInt(1, payslipId); //
//                            pstmtPayslipDetail.setString(2, allowanceName); //
//                            pstmtPayslipDetail.setDouble(3, allowanceAmount); //
//                            pstmtPayslipDetail.setString(4, "Allowance"); //
//                            pstmtPayslipDetail.addBatch(); //
//                        }
//                    }
//
//                    // Add social contributions and tax as details (to payslipdetail)
//                    if (sssContribution > 0.0) { //
//                        pstmtPayslipDetail.setInt(1, payslipId); //
//                        pstmtPayslipDetail.setString(2, "SSS Contribution"); //
//                        pstmtPayslipDetail.setDouble(3, sssContribution); //
//                        pstmtPayslipDetail.setString(4, "Deduction"); //
//                        pstmtPayslipDetail.addBatch(); //
//                    }
//                    if (philhealthContribution > 0.0) { //
//                        pstmtPayslipDetail.setInt(1, payslipId); //
//                        pstmtPayslipDetail.setString(2, "Philhealth Contribution"); //
//                        pstmtPayslipDetail.setDouble(3, philhealthContribution); //
//                        pstmtPayslipDetail.setString(4, "Deduction"); //
//                        pstmtPayslipDetail.addBatch(); //
//                    }
//                    if (pagibigContribution > 0.0) { //
//                        pstmtPayslipDetail.setInt(1, payslipId); //
//                        pstmtPayslipDetail.setString(2, "Pag-IBIG Contribution"); //
//                        pstmtPayslipDetail.setDouble(3, pagibigContribution); //
//                        pstmtPayslipDetail.setString(4, "Deduction"); //
//                        pstmtPayslipDetail.addBatch(); //
//                    }
//                    if (withholdingTax > 0.0) { //
//                        pstmtPayslipDetail.setInt(1, payslipId); //
//                        pstmtPayslipDetail.setString(2, "Withholding Tax"); //
//                        pstmtPayslipDetail.setDouble(3, withholdingTax); //
//                        pstmtPayslipDetail.setString(4, "Tax"); //
//                        pstmtPayslipDetail.addBatch(); //
//                    }
//
//                    pstmtPayslipDetail.executeBatch(); // Execute all payslip details for current employee
//
//                } // End of employee loop
//
//                // Execute batched deductions and tax computations *after* the loop processes all employees
//                // but *before* the main connection.commit() for the entire payroll run.
//                pstmtDeduction.executeBatch(); //
//                pstmtTaxComputation.executeBatch(); //
//
//                conn.commit(); // Commit all payslips, details, deductions, and tax computations
//                System.out.println("Payroll calculation and payslip generation completed successfully for " + periodName + "."); //
//
//                // --- 7. Mark PayPeriod as Processed ---
//                String updatePayPeriodSql = "UPDATE payperiod SET IsProcessed = ?, ProcessedDate = ? WHERE PayPeriodID = ?"; //
//                try (PreparedStatement pstmtUpdatePayPeriod = conn.prepareStatement(updatePayPeriodSql)) { //
//                    pstmtUpdatePayPeriod.setBoolean(1, true); //
//                    pstmtUpdatePayPeriod.setTimestamp(2, new Timestamp(System.currentTimeMillis())); //
//                    pstmtUpdatePayPeriod.setInt(3, payPeriodId); //
//                    pstmtUpdatePayPeriod.executeUpdate(); //
//                    conn.commit(); //
//                    System.out.println("PayPeriod marked as processed."); //
//                }
//            } catch (SQLException e) { //
//                conn.rollback(); // Rollback all operations for the current payroll run on error
//                System.err.println("Error during payroll calculation."); //
//                e.printStackTrace(); //
//                throw e; //
//            }
//            System.out.println("--- Payroll Calculation Finished ---"); //
//        }
//    }
//}
