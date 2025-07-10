package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CredentialDAO {

    private static final Logger LOGGER = Logger.getLogger(CredentialDAO.class.getName());

    public boolean authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE Username = ? AND Password = ?"; 
        try (Connection conn = DBConnection.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql)) { 
            pstmt.setString(1, username); 
            pstmt.setString(2, password); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    return rs.getInt(1) > 0; 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error authenticating user: " + username, e); 
            throw e; 
        }
        return false; 
    }

    public List<String> getUserRoles(String username) throws SQLException {
        String sql = "SELECT r.RoleName FROM user u "
                + 
                "JOIN userrole ur ON u.UserID = ur.UserID "
                + 
                "JOIN role r ON ur.RoleID = r.RoleID "
                + 
                "WHERE u.Username = ?"; 
        List<String> roles = new ArrayList<>(); 
        try (Connection conn = DBConnection.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql)) { 
            pstmt.setString(1, username); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                while (rs.next()) { 
                    roles.add(rs.getString("RoleName")); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting roles for user: " + username, e); 
            throw e; 
        }
        return roles; 
    }

    public String getUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT UserID FROM user WHERE Username = ?"; 
        String userId = null; 
        try (Connection conn = DBConnection.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql)) { 
            pstmt.setString(1, username); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    userId = String.valueOf(rs.getInt("UserID")); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting UserID for username: " + username, e); 
            throw e; 
        }
        return userId; 
    }

    public String getPrimaryRoleNameByEmployeeId(String employeeId) throws SQLException {
        String sql = "SELECT r.RoleName FROM user u "
                + 
                "JOIN userrole ur ON u.UserID = ur.UserID "
                + 
                "JOIN role r ON ur.RoleID = r.RoleID "
                + 
                "WHERE u.UserID = ? "
                + 
                "ORDER BY CASE r.RoleName "
                + 
                "WHEN 'Admin' THEN 1 "
                + 
                "WHEN 'Manager' THEN 2 "
                + 
                "WHEN 'Regular Employee' THEN 3 "
                + 
                "ELSE 4 END LIMIT 1"; //Get the highest privilege role

        String roleName = null; 
        try (Connection conn = DBConnection.getConnection(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql)) { 
            pstmt.setString(1, employeeId); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    roleName = rs.getString("RoleName"); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting primary role for employee ID: " + employeeId, e); 
            throw e; 
        }
        return roleName; 
    }

    public String addUser(Connection conn, String username, String password) throws SQLException { 
        String sql = "INSERT INTO user (Username, Password) VALUES (?, ?)"; 
        String generatedUserId = null; 

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { 
            pstmt.setString(1, username); 
            pstmt.setString(2, password); 
            int rowsAffected = pstmt.executeUpdate(); 
            if (rowsAffected > 0) { 
                try (ResultSet rs = pstmt.getGeneratedKeys()) { 
                    if (rs.next()) { 
                        generatedUserId = String.valueOf(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error adding user: " + username, e); 
            throw e; 
        }
        return generatedUserId; 
    }


    public boolean assignRoleToUser(Connection conn, String userId, String roleName) throws SQLException { 

        String getRoleIdSql = "SELECT RoleID FROM role WHERE RoleName = ? AND IsDeleted = 0"; 
        int roleId = -1; 
        try (Connection localConn = DBConnection.getConnection();
                 PreparedStatement pstmt = localConn.prepareStatement(getRoleIdSql)) { 
            pstmt.setString(1, roleName); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    roleId = rs.getInt("RoleID"); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting RoleID for role name: " + roleName, e); 
            throw e; 
        }

        if (roleId == -1) { 
            LOGGER.log(Level.WARNING, "Role name '" + roleName + "' not found in database. Cannot assign role to user " + userId); 
            return false; 
        }


        String insertUserRoleSql = "INSERT INTO userrole (UserID, RoleID) VALUES (?, ?)"; 
        try (PreparedStatement pstmt = conn.prepareStatement(insertUserRoleSql)) { 
            pstmt.setString(1, userId); 
            pstmt.setInt(2, roleId); 
            int rowsAffected = pstmt.executeUpdate(); 
            return rowsAffected > 0; 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error assigning role " + roleName + " to user " + userId, e); 
            throw e; 
        }
    }
}
