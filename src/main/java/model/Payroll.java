package model;

import java.time.LocalDate;

public class Payroll {

    private int payrollId;
    private String employeeNo;
    private String employeeName;
    private String position;
    
    private double baseSalary;
    private double grossPay;
    private double sssDeduction;
    private double philhealthDeduction;
    private double pagibigDeduction;
    private double withholdingTax;
    private double totalDeductions;
    private double netPay;

    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate paymentDate;

    // --- Constructors ---
    public Payroll() {}

    public Payroll(String employeeNo, String employeeName, String position,
                   double baseSalary, double grossPay,
                   double sssDeduction, double philhealthDeduction,
                   double pagibigDeduction, double withholdingTax,
                   double totalDeductions, double netPay,
                   String periodName, LocalDate startDate, LocalDate endDate, LocalDate paymentDate) {

        this.employeeNo = employeeNo;
        this.employeeName = employeeName;
        this.position = position;
        this.baseSalary = baseSalary;
        this.grossPay = grossPay;
        this.sssDeduction = sssDeduction;
        this.philhealthDeduction = philhealthDeduction;
        this.pagibigDeduction = pagibigDeduction;
        this.withholdingTax = withholdingTax;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
        this.periodName = periodName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentDate = paymentDate;
    }

   
    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public void setGrossPay(double grossPay) {
        this.grossPay = grossPay;
    }

    public double getSssDeduction() {
        return sssDeduction;
    }

    public void setSssDeduction(double sssDeduction) {
        this.sssDeduction = sssDeduction;
    }

    public double getPhilhealthDeduction() {
        return philhealthDeduction;
    }

    public void setPhilhealthDeduction(double philhealthDeduction) {
        this.philhealthDeduction = philhealthDeduction;
    }

    public double getPagibigDeduction() {
        return pagibigDeduction;
    }

    public void setPagibigDeduction(double pagibigDeduction) {
        this.pagibigDeduction = pagibigDeduction;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public void setWithholdingTax(double withholdingTax) {
        this.withholdingTax = withholdingTax;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public void setTotalDeductions(double totalDeductions) {
        this.totalDeductions = totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    @Override
    public String toString() {
        return "Payroll{" +
                "employeeNo='" + employeeNo + '\'' +
                ", name='" + employeeName + '\'' +
                ", netPay=" + netPay +
                ", period='" + periodName + '\'' +
                '}';
    }
}