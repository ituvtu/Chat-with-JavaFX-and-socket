package ituvtu.chat;

import java.io.*;
import java.sql.*;
import java.util.*;

public class DatabaseConnection {
    private static Connection connection = null;

    private DatabaseConnection() { }  // Private constructor

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Initialize the connection
                InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
                Properties props = new Properties();
                props.load(is);

                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String password = props.getProperty("db.password");
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database properties: " + e);
        }
        return connection;
    }
}

