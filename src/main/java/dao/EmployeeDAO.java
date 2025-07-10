package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Admin;
import model.Employee;
import model.Manager;
import model.RegularEmployee;

public class EmployeeDAO {

    private static final Logger LOGGER = Logger.getLogger(EmployeeDAO.class.getName());
    private AddressDAO addressDAO;
    private PositionDAO positionDAO;
    private CredentialDAO credentialDAO; // To get role information

    public EmployeeDAO() {
        this.addressDAO = new AddressDAO();
        this.positionDAO = new PositionDAO();
        this.credentialDAO = new CredentialDAO();
    }

    public boolean addEmployee(Connection conn, Employee employee) throws SQLException { 
        int addressId = -1; 
        String fullAddressString = employee.getEmployeeAddress(); 
        String unitOrHouseStreet = ""; 
        String barangay = ""; 
        String cityMunicipality = ""; 
        String province = ""; 
        String region = ""; 
        String postalCode = ""; 

        String[] parts = fullAddressString.split(","); 
        if (parts.length >= 6) { 
            unitOrHouseStreet = parts[0].trim(); 
            barangay = parts[1].trim(); 
            cityMunicipality = parts[2].trim(); 
            province = parts[3].trim(); 
            region = parts[4].trim(); 
            postalCode = parts[5].trim(); 
        } else {
            LOGGER.log(Level.WARNING, "Employee address string has fewer than 6 expected comma-separated parts: " + fullAddressString); 
            throw new SQLException("Invalid or incomplete address format provided for employee " + employee.getEmployeeNo() + ". Expected 'Street, Barangay, City, Province, Region, Postal Code'.");
        }

        try {
            addressId = addressDAO.getOrCreateAddressId(conn, unitOrHouseStreet, barangay, cityMunicipality,
                    province, region, postalCode); 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Failed to get or create address ID for new employee " + employee.getEmployeeNo(), e); 
            throw e; 
        }

        
        int positionId = positionDAO.getPositionIdByName(employee.getEmployeePosition()); 
        if (positionId == -1) { 
            LOGGER.log(Level.SEVERE, "Position not found for name: " + employee.getEmployeePosition() + " for employee " + employee.getEmployeeNo()); 
            throw new SQLException("Position '" + employee.getEmployeePosition() + "' not found in the database. Cannot add employee."); 
        }

        String sql = "INSERT INTO employee (UserID, LastName, FirstName, Birthday, AddressID, PhoneNumber, "
                + 
                "SSSN, PhHN, TIN, HDMFN, EmpStatus, PositionID, "
                + 
                "SupervisorID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; 


        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setString(1, employee.getUserID()); 
            pstmt.setString(2, employee.getEmployeeLN()); 
            pstmt.setString(3, employee.getEmployeeFN()); 
            pstmt.setDate(4, new java.sql.Date(employee.getEmployeeDOB().getTime())); 
            pstmt.setInt(5, addressId); 
            pstmt.setString(6, employee.getEmployeePhoneNumber()); 
            pstmt.setString(7, employee.getEmployeeSSS()); 
            pstmt.setString(8, employee.getEmployeePhilHealth()); 
            pstmt.setString(9, employee.getEmployeeTIN()); 
            pstmt.setString(10, employee.getEmployeePagIbig()); 
            pstmt.setString(11, employee.getEmployeeStatus()); 
            pstmt.setInt(12, positionId); 
            pstmt.setString(13, employee.getEmployeeSupervisor()); 

            int rowsAffected = pstmt.executeUpdate(); 
            return rowsAffected > 0; 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error adding employee: " + employee.getEmployeeNo(), e); 
            throw e; 
        }
    }

    public Employee getEmployeeByEmployeeNo(String employeeNo) throws SQLException {
        String primaryRoleName = credentialDAO.getPrimaryRoleNameByEmployeeId(employeeNo);
        if (primaryRoleName == null) {
            LOGGER.log(Level.WARNING, "No primary role found for employee ID: " + employeeNo); 
            return null;
        }

        String sql = "SELECT e.EmployeeID, e.LastName, e.FirstName, e.Birthday, e.PhoneNumber, "
                + 
                "e.SSSN, e.PhHN, e.TIN, e.HDMFN, e.EmpStatus, "
                + 
                "e.SupervisorID, a.UnitOrHouseStreet, a.Barangay, a.CityMunicipality, "
                + 
                "a.Province, a.Region, a.PostalCode, p.PositionName "
                + 
                "FROM employee e "
                + 
                "LEFT JOIN address a ON e.AddressID = a.AddressID "
                + 
                "LEFT JOIN position p ON e.PositionID = p.PositionID "
                + 
                "WHERE e.EmployeeID = ? AND e.IsDeleted = 0"; 
        Employee employee = null; 

        try (Connection conn = DBConnection.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setString(1, employeeNo); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    AddressDAO.AddressComponents addressComponents = new AddressDAO.AddressComponents( 
                            rs.getString("UnitOrHouseStreet"), 
                            rs.getString("Barangay"), 
                            rs.getString("CityMunicipality"), 
                            rs.getString("Province"), 
                            rs.getString("Region"), 
                            rs.getString("PostalCode") 
                    );
                    String fullAddress = addressComponents.toString(); 

                    // Instantiate the correct subclass based on primaryRoleName
                    switch (primaryRoleName) {
                        case "Admin": 
                            employee = new Admin( 
                                    rs.getString("EmployeeID"), 
                                    rs.getString("FirstName"), 
                                    rs.getString("LastName"), 
                                    fullAddress,  
                                    rs.getDate("Birthday"), 
                                    rs.getString("PhoneNumber"), 
                                    rs.getString("SSSN"), 
                                    rs.getString("PhHN"), 
                                    rs.getString("TIN"), 
                                    rs.getString("HDMFN"), 
                                    rs.getString("EmpStatus"), 
                                    rs.getString("PositionName"), 
                                    rs.getString("SupervisorID"), 
                                    0.0 
                            );
                            break; 
                        case "Manager": 
                            employee = new Manager( 
                                    rs.getString("EmployeeID"), 
                                    rs.getString("FirstName"), 
                                    rs.getString("LastName"), 
                                    fullAddress, 
                                    rs.getDate("Birthday"), 
                                    rs.getString("PhoneNumber"), 
                                    rs.getString("SSSN"), 
                                    rs.getString("PhHN"), 
                                    rs.getString("TIN"), 
                                    rs.getString("HDMFN"), 
                                    rs.getString("EmpStatus"), 
                                    rs.getString("PositionName"), 
                                    rs.getString("SupervisorID"), 
                                    0.0 
                            );
                            break; 
                        case "Regular Employee": 
                            employee = new RegularEmployee( 
                                    rs.getString("EmployeeID"), 
                                    rs.getString("FirstName"), 
                                    rs.getString("LastName"), 
                                    fullAddress,  
                                    rs.getDate("Birthday"), 
                                    rs.getString("PhoneNumber"), 
                                    rs.getString("SSSN"), 
                                    rs.getString("PhHN"), 
                                    rs.getString("TIN"), 
                                    rs.getString("HDMFN"), 
                                    rs.getString("EmpStatus"), 
                                    rs.getString("PositionName"), 
                                    rs.getString("SupervisorID"), 
                                    0.0 
                            );
                            break;
                        default: 
                            LOGGER.log(Level.WARNING, "Unknown role name encountered for employee ID: " + employeeNo + " - " + primaryRoleName); 
                            return null;
                    }
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting employee by ID: " + employeeNo, e); 
            throw e; 
        }
        return employee; 
    }

    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> employees = new ArrayList<>(); 
        String sql = "SELECT e.EmployeeID, e.LastName, e.FirstName, e.Birthday, e.PhoneNumber, "
                + 
                "e.SSSN, e.PhHN, e.TIN, e.HDMFN, e.EmpStatus, "
                + 
                "e.SupervisorID, a.UnitOrHouseStreet, a.Barangay, a.CityMunicipality, "
                + 
                "a.Province, a.Region, a.PostalCode, p.PositionName "
                + 
                "FROM employee e "
                + 
                "LEFT JOIN address a ON e.AddressID = a.AddressID "
                + 
                "LEFT JOIN position p ON e.PositionID = p.PositionID "
                + 
                "WHERE e.IsDeleted = 0"; 

        try (Connection conn = DBConnection.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql); 
                 ResultSet rs = pstmt.executeQuery()) { 

            while (rs.next()) { 
                String currentEmployeeId = rs.getString("EmployeeID"); 
                String primaryRoleName = credentialDAO.getPrimaryRoleNameByEmployeeId(currentEmployeeId); 

                if (primaryRoleName == null) { 
                    LOGGER.log(Level.WARNING, "Skipping employee " + currentEmployeeId + " due to no primary role found."); 
                    continue; 
                }

                AddressDAO.AddressComponents addressComponents = new AddressDAO.AddressComponents( 
                        rs.getString("UnitOrHouseStreet"), 
                        rs.getString("Barangay"), 
                        rs.getString("CityMunicipality"), 
                        rs.getString("Province"), 
                        rs.getString("Region"), 
                        rs.getString("PostalCode") 
                );
                String fullAddress = addressComponents.toString(); 

                Employee employee = null; 
                switch (primaryRoleName) { 
                    case "Admin": 
                        employee = new Admin( 
                                rs.getString("EmployeeID"), 
                                rs.getString("FirstName"), 
                                rs.getString("LastName"), 
                                fullAddress, 
                                rs.getDate("Birthday"), 
                                rs.getString("PhoneNumber"), 
                                rs.getString("SSSN"), 
                                rs.getString("PhHN"), 
                                rs.getString("TIN"), 
                                rs.getString("HDMFN"), 
                                rs.getString("EmpStatus"), 
                                rs.getString("PositionName"), 
                                rs.getString("SupervisorID"), 
                                0.0 
                        );
                        break; 
                    case "Manager": 
                        employee = new Manager( 
                                rs.getString("EmployeeID"), 
                                rs.getString("FirstName"), 
                                rs.getString("LastName"), 
                                fullAddress,  
                                rs.getDate("Birthday"), 
                                rs.getString("PhoneNumber"), 
                                rs.getString("SSSN"), 
                                rs.getString("PhHN"), 
                                rs.getString("TIN"), 
                                rs.getString("HDMFN"), 
                                rs.getString("EmpStatus"), 
                                rs.getString("PositionName"), 
                                rs.getString("SupervisorID"), 
                                0.0 
                        );
                        break; 
                    case "Regular Employee": 
                        employee = new RegularEmployee( 
                                rs.getString("EmployeeID"), 
                                rs.getString("FirstName"), 
                                rs.getString("LastName"), 
                                fullAddress,  
                                rs.getDate("Birthday"), 
                                rs.getString("PhoneNumber"), 
                                rs.getString("SSSN"), 
                                rs.getString("PhHN"), 
                                rs.getString("TIN"), 
                                rs.getString("HDMFN"), 
                                rs.getString("EmpStatus"), 
                                rs.getString("PositionName"), 
                                rs.getString("SupervisorID"), 
                                0.0 
                        );
                        break; 
                    default: 
                        LOGGER.log(Level.WARNING, "Unknown role name encountered for employee ID: " + currentEmployeeId + " - " + primaryRoleName + ". Skipping this employee."); 
                        continue;
                }
                if (employee != null) { 
                    employees.add(employee); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting all employees", e); 
            throw e; 
        }
        return employees; 
    }

    public boolean updateEmployee(Connection conn, Employee employee) throws SQLException { 
        int addressId = -1; 
        String fullAddressString = employee.getEmployeeAddress(); 
        String unitOrHouseStreet = ""; 
        String barangay = ""; 
        String cityMunicipality = ""; 
        String province = ""; 
        String region = ""; 
        String postalCode = ""; 

        String[] parts = fullAddressString.split(","); 
        if (parts.length >= 6) { 
            unitOrHouseStreet = parts[0].trim(); 
            barangay = parts[1].trim(); 
            cityMunicipality = parts[2].trim(); 
            province = parts[3].trim(); 
            region = parts[4].trim(); 
            postalCode = parts[5].trim(); 
        } else {
            LOGGER.log(Level.WARNING, "Employee address string has fewer than 6 expected comma-separated parts during update: " + fullAddressString); 
            throw new SQLException("Invalid or incomplete address format provided for employee update " + employee.getEmployeeNo() + ". Expected 'Street, Barangay, City, Province, Region, Postal Code'."); 
        }

        try {
            addressId = addressDAO.getOrCreateAddressId(conn, unitOrHouseStreet, barangay, cityMunicipality, province, region, postalCode); 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Failed to get or create address ID for employee update " + employee.getEmployeeNo(), e); 
            throw e; 
        }


        int positionId = positionDAO.getPositionIdByName(employee.getEmployeePosition()); 
        if (positionId == -1) { 
            LOGGER.log(Level.SEVERE, "Position not found for name: " + employee.getEmployeePosition() + " for employee update " + employee.getEmployeeNo()); 
            throw new SQLException("Position '" + employee.getEmployeePosition() + "' not found in the database. Cannot update employee."); 
        }

        String sql = "UPDATE employee SET LastName = ?, FirstName = ?, Birthday = ?, AddressID = ?, "
                + 
                "PhoneNumber = ?, SSSN = ?, PhHN = ?, TIN = ?, "
                + 
                "HDMFN = ?, EmpStatus = ?, PositionID = ?, SupervisorID = ? "
                + 
                "WHERE EmployeeID = ?"; 


        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setString(1, employee.getEmployeeLN()); 
            pstmt.setString(2, employee.getEmployeeFN()); 
            pstmt.setDate(3, new java.sql.Date(employee.getEmployeeDOB().getTime())); 
            pstmt.setInt(4, addressId); 
            pstmt.setString(5, employee.getEmployeePhoneNumber()); 
            pstmt.setString(6, employee.getEmployeeSSS()); 
            pstmt.setString(7, employee.getEmployeePhilHealth()); 
            pstmt.setString(8, employee.getEmployeeTIN()); 
            pstmt.setString(9, employee.getEmployeePagIbig()); 
            pstmt.setString(10, employee.getEmployeeStatus()); 
            pstmt.setInt(11, positionId); 
            pstmt.setString(12, employee.getEmployeeSupervisor()); 
            pstmt.setString(13, employee.getEmployeeNo()); 

            int rowsAffected = pstmt.executeUpdate(); 
            return rowsAffected > 0; 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error updating employee: " + employee.getEmployeeNo(), e); 
            throw e; 
        }
    }


    public boolean deleteEmployee(Connection conn, String employeeNo) throws SQLException { 
        String sql = "UPDATE employee SET IsDeleted = 1 WHERE EmployeeID = ?"; 


        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setString(1, employeeNo); 

            int rowsAffected = pstmt.executeUpdate(); 
            return rowsAffected > 0; 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error soft-deleting employee: " + employeeNo, e); 
            throw e; 
        }
    }
}
