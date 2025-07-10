package data;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());

    private static String URL;
    private static String USER;
    private static String PASSWORD;
    private static boolean propertiesLoaded = false; 

    
    static {
        try {
            InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("config/securedb.properties");
            if (input == null) {
                LOGGER.log(Level.SEVERE, "Sorry, unable to find config/securedb.properties. Please check its location!");
            } else {
                Properties prop = new Properties();
                prop.load(input);

                URL = prop.getProperty("securedb.url");
                USER = prop.getProperty("securedb.user");
                PASSWORD = prop.getProperty("securedb.password");

                propertiesLoaded = true; 
                LOGGER.info("Database connection properties loaded successfully!");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while loading database properties:", e);
        }
    }


    public static Connection getConnection() throws SQLException {
        if (!propertiesLoaded) {  
            throw new SQLException("Database connection details are missing or failed to load. Check securedb.properties and logs for errors.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static Connection getTransactionalConnection() throws SQLException {
        if (!propertiesLoaded) {  
            throw new SQLException("Database connection details are missing or failed to load. Check securedb.properties and logs for errors.");
        }
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        conn.setAutoCommit(false);  
        LOGGER.fine("Transactional connection obtained with auto-commit OFF.");
        return conn;
    }
}
