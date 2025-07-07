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

    private Map<String, Integer> leaveTypeNamesToIds; // Cache for name-to-ID mapping
    private Map<Integer, String> leaveTypeIdsToNames; // Cache for ID-to-name mapping

    public LeaveTypeDAO() {
        leaveTypeNamesToIds = new HashMap<>();
        leaveTypeIdsToNames = new HashMap<>();
        try {
            loadLeaveTypes(); // Load types on initialization
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load leave types during initialization.", e);
            // Consider throwing a runtime exception or handling gracefully if types are critical
        }
    }

    // Loads all leave types from the database into caches.
    private void loadLeaveTypes() throws SQLException {
        String sql = "SELECT LeaveTypeID, LeaveName FROM leavetype WHERE IsDeleted = 0"; //
        try (Connection conn = DBConnection.getConnection(); // Gets its own connection
             PreparedStatement pstmt = conn.prepareStatement(sql); //
             ResultSet rs = pstmt.executeQuery()) { //
            while (rs.next()) { //
                int id = rs.getInt("LeaveTypeID"); //
                String name = rs.getString("LeaveName"); //
                leaveTypeNamesToIds.put(name, id); //
                leaveTypeIdsToNames.put(id, name); //
            }
        } catch (SQLException e) { //
            LOGGER.log(Level.SEVERE, "Error loading leave types from database.", e); //
            throw e; //
        }
    }

    /**
     * Gets the LeaveTypeID for a given LeaveName.
     * @param leaveName The name of the leave type (e.g., "Vacation Leave").
     * @return The LeaveTypeID, or -1 if not found.
     */
    public int getLeaveTypeId(String leaveName) { //
        // Use cached value, or reload if not found (in case DB updated)
        return leaveTypeNamesToIds.getOrDefault(leaveName, -1); //
    }

    /**
     * Gets the LeaveName for a given LeaveTypeID.
     * @param leaveTypeId The ID of the leave type.
     * @return The LeaveName, or null if not found.
     */
    public String getLeaveTypeName(int leaveTypeId) { //
        // Use cached value, or reload if not found
        return leaveTypeIdsToNames.get(leaveTypeId); //
    }
    
    /**
     * Retrieves all active leave type names.
     * @return A list of all active leave type names.
     * @throws SQLException if a database access error occurs.
     */
    public List<String> getAllLeaveTypeNames() throws SQLException {
        // This is useful for populating JComboBoxes in the GUI
        List<String> names = new ArrayList<>(leaveTypeNamesToIds.keySet()); //
        // If caching is strict, you might want to re-query here for freshest data.
        // For simplicity, returning from cache.
        return names; //
    }
}