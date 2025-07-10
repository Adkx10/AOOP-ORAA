package dao;

import data.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddressDAO {

    private static final Logger LOGGER = Logger.getLogger(AddressDAO.class.getName());

    public int getOrCreateAddressId(Connection conn, String unitOrHouseStreet, String barangay, String cityMunicipality, 
            String province, String region, String postalCode) throws SQLException { 

        String selectSql = "SELECT AddressID FROM address " + 
                "WHERE UnitOrHouseStreet = ? AND Barangay = ? AND CityMunicipality = ? " + 
                "AND Province = ? AND Region = ? AND PostalCode = ? AND IsDeleted = 0"; 

        String insertSql = "INSERT INTO address (UnitOrHouseStreet, Barangay, CityMunicipality, Province, Region, PostalCode) " + 
                "VALUES (?, ?, ?, ?, ?, ?)"; 

        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) { 
            pstmt.setString(1, unitOrHouseStreet); 
            pstmt.setString(2, barangay); 
            pstmt.setString(3, cityMunicipality); 
            pstmt.setString(4, province); 
            pstmt.setString(5, region); 
            pstmt.setString(6, postalCode); 

            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    return rs.getInt("AddressID"); 
                }
            }
        }

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) { 
            pstmt.setString(1, unitOrHouseStreet); 
            pstmt.setString(2, barangay); 
            pstmt.setString(3, cityMunicipality); 
            pstmt.setString(4, province); 
            pstmt.setString(5, region); 
            pstmt.setString(6, postalCode); 

            int rowsAffected = pstmt.executeUpdate(); 
            if (rowsAffected > 0) { 
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) { 
                    if (generatedKeys.next()) { 
                        return generatedKeys.getInt(1); 
                    }
                }
            }
            throw new SQLException("Failed to create new address, no ID obtained."); 

        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error checking or inserting address.", e); 
            throw e; 
        }
    }

    public AddressComponents getAddressComponentsById(int addressId) throws SQLException {
        String sql = "SELECT UnitOrHouseStreet, Barangay, CityMunicipality, Province, Region, PostalCode " + 
                "FROM address WHERE AddressID = ? AND IsDeleted = 0"; 
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { 

            pstmt.setInt(1, addressId); 
            try (ResultSet rs = pstmt.executeQuery()) { 
                if (rs.next()) { 
                    return new AddressComponents( 
                            rs.getString("UnitOrHouseStreet"), 
                            rs.getString("Barangay"), 
                            rs.getString("CityMunicipality"), 
                            rs.getString("Province"), 
                            rs.getString("Region"), 
                            rs.getString("PostalCode") 
                    );
                }
            }
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error getting address components by ID: " + addressId, e); 
            throw e;
        }
        return null;
    }

    public boolean updateAddress(Connection conn, int addressId, String unitOrHouseStreet, String barangay, 
            String cityMunicipality, String province, String region, String postalCode) throws SQLException { 
        String sql = "UPDATE address SET UnitOrHouseStreet = ?, Barangay = ?, CityMunicipality = ?, " + 
                "Province = ?, Region = ?, PostalCode = ? WHERE AddressID = ?"; 
        // Use the provided connection, do NOT close it here
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) { 
            pstmt.setString(1, unitOrHouseStreet); 
            pstmt.setString(2, barangay); 
            pstmt.setString(3, cityMunicipality); 
            pstmt.setString(4, province); 
            pstmt.setString(5, region); 
            pstmt.setString(6, postalCode); 
            pstmt.setInt(7, addressId); 
            int rowsAffected = pstmt.executeUpdate(); 
            return rowsAffected > 0; 
        } catch (SQLException e) { 
            LOGGER.log(Level.SEVERE, "Error updating address ID: " + addressId, e); 
            throw e; 
        }
    }

    //POJO to hold all components of an address.
    public static class AddressComponents { 

        public String unitOrHouseStreet; 
        public String barangay; 
        public String cityMunicipality; 
        public String province; 
        public String region; 
        public String postalCode; 

        public AddressComponents(String unitOrHouseStreet, String barangay, String cityMunicipality, 
                String province, String region, String postalCode) { 
            this.unitOrHouseStreet = unitOrHouseStreet; 
            this.barangay = barangay; 
            this.cityMunicipality = cityMunicipality; 
            this.province = province; 
            this.region = region; 
            this.postalCode = postalCode; 
        }

        // Helper to reconstruct a common display format, or return individual components
        @Override
        public String toString() { 
            StringBuilder fullAddress = new StringBuilder(); 
            if (unitOrHouseStreet != null && !unitOrHouseStreet.isEmpty()) { 
                fullAddress.append(unitOrHouseStreet); 
            }
            if (barangay != null && !barangay.isEmpty()) { 
                if (fullAddress.length() > 0) { 
                    fullAddress.append(", "); 
                }
                fullAddress.append(barangay); 
            }
            if (cityMunicipality != null && !cityMunicipality.isEmpty()) { 
                if (fullAddress.length() > 0) { 
                    fullAddress.append(", "); 
                }
                fullAddress.append(cityMunicipality); 
            }
            if (province != null && !province.isEmpty()) { 
                if (fullAddress.length() > 0) { 
                    fullAddress.append(", "); 
                }
                fullAddress.append(province); 
            }
            if (region != null && !region.isEmpty()) { 
                if (fullAddress.length() > 0) { 
                    fullAddress.append(", "); 
                }
                fullAddress.append(region); 
            }
            if (postalCode != null && !postalCode.isEmpty()) { 
                if (fullAddress.length() > 0) { 
                    fullAddress.append(", ");
                }
                fullAddress.append(postalCode); 
            }
            return fullAddress.toString();
        }
    }
}