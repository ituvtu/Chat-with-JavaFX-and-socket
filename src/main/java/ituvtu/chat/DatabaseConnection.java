package ituvtu.chat;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static Connection connection = null;

    private DatabaseConnection() { }  // Private constructor

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Initialize the connection
                InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties");
                Properties props = new Properties();
                props.load(is);

                String url = props.getProperty("db.url");
                String user = props.getProperty("db.user");
                String password = props.getProperty("db.password");
                connection = DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
                System.err.println("Database connection failed: " + e.getMessage());
                return null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }
}

