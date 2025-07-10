package model;

import java.sql.SQLException; 

public interface DataReader {

    boolean readData(String empNo) throws SQLException;
}
