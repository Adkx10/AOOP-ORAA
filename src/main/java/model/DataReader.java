package model;

import java.sql.SQLException; // Now throws SQLException for database interactions

public interface DataReader {

    boolean readData(String empNo) throws SQLException;
}
