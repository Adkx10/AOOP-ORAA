/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utilities;

import model.Admin;
import model.Employee;
import model.Manager;

/**
 *
 * @author user
 */
public class UtilMethods {

    public static String getMonthNumber(String monthName) { //
        return switch (monthName.toLowerCase()) { //
            case "january" ->
                "01"; //
            case "february" ->
                "02"; //
            case "march" ->
                "03"; //
            case "april" ->
                "04"; //
            case "may" ->
                "05"; //
            case "june" ->
                "06"; //
            case "july" ->
                "07"; //
            case "august" ->
                "08"; //
            case "september" ->
                "09"; //
            case "october" ->
                "10"; //
            case "november" ->
                "11"; //
            case "december" ->
                "12"; //
            default ->
                null; // Invalid month name
        };
    }

    public static int getMonthIndex(String monthName) {
        String number = getMonthNumber(monthName);
        return number != null ? Integer.parseInt(number) : -1;
    }

    public static boolean canManagerViewEmployee(Employee currentUser, Employee targetEmployee) {
        if (!(currentUser instanceof Manager manager)) {
            return true; // Rule only applies to Managers
        }

        boolean isSelf = manager.getEmployeeNo().equalsIgnoreCase(targetEmployee.getEmployeeNo());
        boolean isAdmin = targetEmployee instanceof Admin;
        boolean isManager = targetEmployee instanceof Manager;

        // Manager can't view Admins or other Managers (unless it's themself)
        return !(isAdmin || (isManager && !isSelf));
    }

}
