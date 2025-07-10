package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PositionDAO {

    private static final Logger LOGGER = Logger.getLogger(PositionDAO.class.getName());

    public int getPositionIdByName(String positionName) throws SQLException {
        String sql = "SELECT PositionID FROM position WHERE PositionName = ? AND IsDeleted = 0"; 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setString(1, positionName); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    return rs.getInt("PositionID"); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting PositionID by name: " + positionName, e); 
            throw e; 
        }
        return -1; 
    }


    public String getPositionNameById(int positionId) throws SQLException {
        String sql = "SELECT PositionName FROM position WHERE PositionID = ? AND IsDeleted = 0"; 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setInt(1, positionId); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    return rs.getString("PositionName"); 
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting PositionName by ID: " + positionId, e); 
            throw e; 
        }
        return null;
    }

    public List<String> getAllPositionNames() throws SQLException {
        List<String> positions = new ArrayList<>(); 

        String sql = "SELECT PositionName FROM position WHERE IsDeleted = 0";  
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) { 

            while (rs.next()) { 
                positions.add(rs.getString("PositionName")); 
            }
        }

        return positions; 
    }
}