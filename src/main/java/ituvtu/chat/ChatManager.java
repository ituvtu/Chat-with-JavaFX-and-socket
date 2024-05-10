package ituvtu.chat;

import java.sql.*;
import java.util.*;

public class ChatManager {
    private final Connection dbConn;
    public ChatManager(Connection dbConn) {
        this.dbConn=dbConn;
    }
    public boolean createChat(String username1, String username2) {
        String sqlExists = "SELECT chat_id FROM chat WHERE (username_first = ? AND username_second = ?) OR (username_first = ? AND username_second = ?)";
        String sqlInsert = "INSERT INTO chat (username_first, username_second) VALUES (?, ?)";
        try (PreparedStatement checkStmt = dbConn.prepareStatement(sqlExists);
             PreparedStatement insertStmt = dbConn.prepareStatement(sqlInsert)) {
            checkStmt.setString(1, username1);
            checkStmt.setString(2, username2);
            checkStmt.setString(3, username2);
            checkStmt.setString(4, username1);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) { return false; }
            insertStmt.setString(1, username1);
            insertStmt.setString(2, username2);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    boolean chatExists(String username1, String username2) {
        String sql = "SELECT chat_id FROM chat WHERE (username_first = ? AND username_second = ?) OR (username_first = ? AND username_second = ?)";
        if (dbConn != null) {
            try (PreparedStatement checkStmt = dbConn.prepareStatement(sql)) {
                checkStmt.setString(1, username1);
                checkStmt.setString(2, username2);
                checkStmt.setString(3, username2);
                checkStmt.setString(4, username1);
                ResultSet rs = checkStmt.executeQuery();
                return rs.next(); // Returns true if the chat exists
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                return false;
            }
        }
        else {
            System.out.println("dbConn is null");
            return false;
        }
    }

    public List<String> getAllChats() {
        List<String> chats = new ArrayList<>();
        String sql = "SELECT username_first, username_second FROM chat";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String chatInfo = "Chat between " + rs.getString("username_first") + " and " + rs.getString("username_second");
                chats.add(chatInfo);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return chats;
    }

    public List<Chat> getUserChats(String username) {
        List<Chat> chats = new ArrayList<>();
        String sql = "SELECT chat_id, username_first, username_second FROM chat WHERE username_first = ? OR username_second = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
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

    public boolean updateChat(int chatId, Map<String, String> parameters) {
        String sql = "UPDATE chat SET username_first = ?, username_second = ? WHERE chat_id = ?";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
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
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Database error during chat deletion: " + e.getMessage());
            return false;
        }
    }


    public Integer getChatIdByUsernames(String username1, String username2) {
        String sql = "SELECT chat_id FROM chat WHERE (username_first = ? AND username_second = ?) OR (username_first = ? AND username_second = ?)";
        try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
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
}