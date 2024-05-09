package ituvtu.chat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

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
    public static boolean checkOrCreateUser(String username, String password) {
        if (!checkUserExists(username)) {
            return createUser(username, hashPassword(password));
        } else {
            return checkUserCredentials(username, hashPassword(password));
        }
    }
    private static boolean checkUserExists(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
    private static boolean createUser(String username, String hashedPassword) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
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
    public static boolean checkUserCredentials(String username, String hashedPassword) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
}

