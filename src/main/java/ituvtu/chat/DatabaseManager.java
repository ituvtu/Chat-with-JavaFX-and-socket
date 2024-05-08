package ituvtu.chat;

import java.sql.*;

public class DatabaseManager {
    final Connection dbConn;
    private static final DatabaseManager instance = new DatabaseManager();
    public DatabaseManager() {
        this.dbConn = DatabaseConnection.getConnection();
    }
    public static DatabaseManager getInstance() {
        return instance;
    }
    public void updateConnectionInfo(String username, int port) {
        String sql = "INSERT INTO connection (username, userport) VALUES (?, ?) ON DUPLICATE KEY UPDATE userport = ?";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setInt(2, port);
                stmt.setInt(3, port);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }
    }

    public int findPortByUsername(String username) {
        int userPort = -1;
        String sql = "SELECT userport FROM connection WHERE username = ?";
        if (dbConn != null) {
            try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    userPort = rs.getInt("userport");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }
        return userPort;
    }
    public boolean checkUserCredentials(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
}

