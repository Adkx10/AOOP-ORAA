package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// This DAO handles operations for the 'leavetype' table.
public class LeaveTypeDAO {

    private static final Logger LOGGER = Logger.getLogger(LeaveTypeDAO.class.getName());

    private Map<String, Integer> leaveTypeNamesToIds;
    private Map<Integer, String> leaveTypeIdsToNames;

    public LeaveTypeDAO() {
        leaveTypeNamesToIds = new HashMap<>();
        leaveTypeIdsToNames = new HashMap<>();
        try {
            loadLeaveTypes(); 
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load leave types during initialization.", e);
           
        }
    }

    private void loadLeaveTypes() throws SQLException {
        String sql = "SELECT LeaveTypeID, LeaveName FROM leavetype WHERE IsDeleted = 0"; 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql); 
             ResultSet rs = pstmt.executeQuery()) { 
            while (rs.next()) { 
                int id = rs.getInt("LeaveTypeID"); 
                String name = rs.getString("LeaveName"); 
                leaveTypeNamesToIds.put(name, id); 
                leaveTypeIdsToNames.put(id, name); 
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error loading leave types from database.", e); 
            throw e; 
        }
    }


    public int getLeaveTypeId(String leaveName) { 

        return leaveTypeNamesToIds.getOrDefault(leaveName, -1); 
    }

    public String getLeaveTypeName(int leaveTypeId) { 
        
        return leaveTypeIdsToNames.get(leaveTypeId); 
    }
    

    public List<String> getAllLeaveTypeNames() throws SQLException {
        // This is useful for populating JComboBoxes in the GUI
        List<String> names = new ArrayList<>(leaveTypeNamesToIds.keySet()); //
        return names; //
    }
}