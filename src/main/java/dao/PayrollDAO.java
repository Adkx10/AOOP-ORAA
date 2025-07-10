package dao;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import data.DBConnection;

public class PayrollDAO {

    public void calculatePayroll(String periodName, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection conn = DBConnection.getTransactionalConnection()) {
            try {
                //CHECK, CREATE/FIND, AND CLEAN PAY PERIOD
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
                
                // PRE-LOADING ALL SCHEDULES AND TYPES
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
                
                // CALCULATION AND INSERTION LOOP
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

                // FINALIZE MARK PERIOD AS PROCESSED
                String updatePayPeriodSql = "UPDATE payperiod SET IsProcessed = ?, ProcessedDate = ? WHERE PayPeriodID = ?";
                try (PreparedStatement pstmtUpdatePayPeriod = conn.prepareStatement(updatePayPeriodSql)) {
                    pstmtUpdatePayPeriod.setBoolean(1, true);
                    pstmtUpdatePayPeriod.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    pstmtUpdatePayPeriod.setInt(3, payPeriodId);
                    pstmtUpdatePayPeriod.executeUpdate();
                }

                // COMMIT SAVE ALL CHANGES AT ONCE
                conn.commit();
                System.out.println("Payroll transaction committed successfully.");

            } catch (SQLException e) {
                // ROLLBACK UNDO ALL CHANGES ON ANY ERROR
                System.err.println("SQL error occurred during payroll calculation. Rolling back transaction.");
                conn.rollback();
                e.printStackTrace();
                throw e; 
            }
        }
    }
}