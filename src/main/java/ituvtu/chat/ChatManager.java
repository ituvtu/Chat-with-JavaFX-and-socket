package ituvtu.chat;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class ChatManager {
    private final DatabaseManager dbManager;

    public ChatManager(Connection connection) {
        this.dbManager = DatabaseManager.getInstance();
    }

    public boolean createChat(String username1, String username2) {
        return dbManager.createChat(username1, username2);
    }

    public boolean chatExists(String username1, String username2) {
        return dbManager.chatExists(username1, username2);
    }

    public List<ChatDisplayData> getAllChats() {
        return dbManager.getAllChats();
    }

    public List<Chat> getUserChats(String username) {
        return dbManager.getUserChats(username);
    }

    public boolean updateChat(int chatId, Map<String, String> parameters) {
        return dbManager.updateChat(chatId, parameters);
    }

    public boolean deleteChat(int chatId) {
        return dbManager.deleteChat(chatId);
    }

    public Integer getChatIdByUsernames(String username1, String username2) {
        return dbManager.getChatIdByUsernames(username1, username2);
    }
}
