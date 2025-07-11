/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilities;

import data.DBConnection;
import java.io.InputStream;
import net.sf.jasperreports.engine.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author user
 */
public class ReportGenerator {
    public void generateReport(String reportName, Map<String, Object> parameters, Connection conn) {
        try {
            // Load .jrxml as a resource from the classpath
            InputStream reportStream = getClass().getClassLoader().getResourceAsStream(reportName);
            if (reportStream == null) {
                System.err.println("Could not find report: " + reportName);
                return;
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, conn);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}