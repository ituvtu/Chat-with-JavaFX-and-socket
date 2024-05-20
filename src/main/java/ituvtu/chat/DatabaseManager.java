package ituvtu.chat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private static final DatabaseManager instance = new DatabaseManager();

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        return instance;
    }

    private Connection getConnection() {
        return DatabaseConnection.getInstance().getConnection();
    }

    public void updateConnectionInfo(String username, int port) {
        String sql = "INSERT INTO connection (username, userport) VALUES (?, ?) ON DUPLICATE KEY UPDATE userport = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, port);
            stmt.setInt(3, port);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
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
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
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
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
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
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                userPort = rs.getInt("userport");
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return userPort;
    }

    public static boolean checkUserCredentials(String username, String hashedPassword) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
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

    public List<ChatDisplayData> getAllChats() {
        List<ChatDisplayData> chats = new ArrayList<>();
        String sql = "SELECT chat_id, username_first, username_second FROM chat";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int chatId = rs.getInt("chat_id");
                String userFirst = rs.getString("username_first");
                String userSecond = rs.getString("username_second");
                String displayName = userFirst + " - " + userSecond;
                chats.add(new ChatDisplayData(chatId, displayName, userFirst, userSecond));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return chats;
    }

    public List<Chat> getUserChats(String username) {
        List<Chat> chats = new ArrayList<>();
        String sql = "SELECT chat_id, username_first, username_second FROM chat WHERE username_first = ? OR username_second = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Chat chat = new Chat(rs.getInt("chat_id"), rs.getString("username_first"), rs.getString("username_second"));
                chats.add(chat);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return chats;
    }

    public void recordMessage(Message msg) {
        Integer chatId = getChatIdByUsernames(msg.getFrom(), msg.getTo());
        if (chatId != null) {
            String sql = "INSERT INTO chat_messages (chat_id, message, username_from, timestamp) VALUES (?, ?, ?, ?)";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, chatId);
                stmt.setString(2, msg.getContent());
                stmt.setString(3, msg.getFrom());
                stmt.setTimestamp(4, Timestamp.valueOf(msg.getTimestamp()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        } else {
            System.err.println("Chat ID not found for users: " + msg.getFrom() + " and " + msg.getTo());
        }
    }

    public List<Message> getMessagesFromDatabase(Integer chatId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getString("username_from"),
                        null,
                        rs.getString("message"),
                        rs.getInt("chat_id")
                );
                message.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                messages.add(message);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return messages;
    }

    public Integer getChatIdByUsernames(String username1, String username2) {
        String sql = "SELECT chat_id FROM chat WHERE (username_first = ? AND username_second = ?) OR (username_first = ? AND username_second = ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username1);
            stmt.setString(2, username2);
            stmt.setString(3, username2);
            stmt.setString(4, username1);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("chat_id");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving chat ID: " + e.getMessage());
        }
        return null;
    }

    public boolean chatExists(String username1, String username2) {
        String sql = "SELECT chat_id FROM chat WHERE (username_first = ? AND username_second = ?) OR (username_first = ? AND username_second = ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username1);
            stmt.setString(2, username2);
            stmt.setString(3, username2);
            stmt.setString(4, username1);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public boolean createChat(String username1, String username2) {
        String sqlInsert = "INSERT INTO chat (username_first, username_second) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(sqlInsert)) {
            insertStmt.setString(1, username1);
            insertStmt.setString(2, username2);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateChat(int chatId, Map<String, String> parameters) {
        String sql = "UPDATE chat SET username_first = ?, username_second = ? WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, parameters.get("username_first"));
            stmt.setString(2, parameters.get("username_second"));
            stmt.setInt(3, chatId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Database error during chat update: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteChat(int chatId) {
        String sql = "DELETE FROM chat WHERE chat_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Database error during chat deletion: " + e.getMessage());
            return false;
        }
    }

    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Database error while checking user existence: " + e.getMessage());
        }
        return false;
    }
}
