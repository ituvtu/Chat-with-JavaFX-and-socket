package ituvtu.chat;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import jakarta.xml.bind.*;

public class Server extends WebSocketServer {
    private static Server instance;
    final Set<WebSocket> connections;
    final Set<ServerObserver> observers;
    final ChatManager chatManager;
    final DatabaseManager dbManager;
    final Connection dbConn = DatabaseConnection.getConnection();
    JAXBContext context = JAXBContext.newInstance(Message.class, UserConnectionInfo.class);

    public Server(int port) throws JAXBException {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        observers = new HashSet<>();
        this.chatManager = new ChatManager(dbConn);
        dbManager = DatabaseManager.getInstance();
    }

    public static synchronized Server getInstance(int port) throws JAXBException {
        if (instance == null) {
            instance = new Server(port);
        }
        return instance;
    }

    @Override
    public void onStart() {
        String logMessage = "Server started successfully on port: " + getPort();
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage);
        updateChatList();
    }

    void updateChatList() {
        List<ChatDisplayData> chats = getAllChats();
        notifyObserversAboutChats(chats);
    }

    List<ChatDisplayData> getAllChats() {
        List<ChatDisplayData> chats = new ArrayList<>();
        String sql = "SELECT chat_id, username_first, username_second FROM chat";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    int chatId = rs.getInt("chat_id");
                    String userFirst = rs.getString("username_first");
                    String userSecond = rs.getString("username_second");
                    String displayName = userFirst + " - " + userSecond;
                    chats.add(new ChatDisplayData(chatId, displayName));
                }
            } catch (SQLException e) {
                String logMessage ="Database error: " + e.getMessage();
                System.err.println(logMessage);
                notifyObserversWithLog(logMessage);
            }
        }
        return chats;
    }

    void notifyObserversAboutChats(List<ChatDisplayData> chats) {
        for (ServerObserver observer : observers) {
            observer.updateChatList(chats);
        }
    }

    public void addObserver(ServerObserver observer) {
        observers.add(observer);
        System.out.println("Observer added: " + observer.getClass().getName());
    }

    void notifyObservers(WebSocket conn,String message) {
        observers.forEach(observer -> {
            try {
                observer.onMessage(conn, message);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void notifyObserversWithMessage(String message) {
        observers.forEach(observer -> observer.displayMessage(message));
    }
    void notifyObserversWithLog(String message){
        observers.forEach(observer -> observer.displayLogMessage(message));
    }
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String logMessage = "New connection: " + getPortConn(conn);
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage);
    }
    int getPortConn(WebSocket conn){
        return conn.getRemoteSocketAddress().getPort();
    }
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String logMessage = "Closed connection: " +getPortConn(conn);
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage);
    }

    @Override
    public void onMessage(WebSocket conn, String input) {
       notifyObservers(conn,input);
    }

    void handleUserConnectionInfo(WebSocket conn, String input) throws JAXBException {
        UserConnectionInfo info = XMLUtil.fromXML(input, UserConnectionInfo.class);
        updateDatabase(info.getUsername(), getPortConn(conn));
    }

    void recordMessageInDatabase(Message msg) {
        Integer chatId = chatManager.getChatIdByUsernames(msg.getFrom(), msg.getTo());
        if (chatId != null) {
            String sql = "INSERT INTO chat_messages (chat_id, message, username_from) VALUES (?, ?, ?)";
            if (dbConn != null) {
                try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                    stmt.setInt(1, chatId);
                    stmt.setString(2, msg.getContent());
                    stmt.setString(3, msg.getFrom());
                    int rowsAffected = stmt.executeUpdate();
                    System.out.println("Message recorded: " + rowsAffected + " row(s) affected.");
                } catch (SQLException e) {
                    String logMessage ="Database error: " + e.getMessage();
                    System.err.println(logMessage);
                    notifyObserversWithLog(logMessage);
                }
            }
        } else {
            String logMessage="Chat ID not found for users: " + msg.getFrom() + " and " + msg.getTo();
            System.out.println(logMessage);
            notifyObserversWithLog(logMessage);
        }
    }

    public WebSocket getConnection() {
        for (WebSocket conn : this.connections) {
            if (conn != null && conn.isOpen()) {
                return conn;
            }
        }
        return null;
    }

    void processGetMessagesRequest(WebSocket conn, ChatRequest chatRequest) {
        List<Message> messages = getMessagesFromDatabase(chatRequest.getChatId());
        try {
            String messagesXml = XMLUtil.toXML(new MessagesResponse(messages));
            conn.send(messagesXml);
        } catch (JAXBException e) {
            conn.send("Error serializing messages");
        }
    }
    String processServerGetMessagesRequest(int chat_id) throws JAXBException {
        return XMLUtil.toXML(new MessagesResponse(getMessagesFromDatabase(chat_id)));
    }

    List<Message> getMessagesFromDatabase(Integer chatId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM chat_messages WHERE chat_id = ?";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                stmt.setInt(1, chatId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    messages.add(new Message(rs.getString("username_from"), null, rs.getString("message")));
                }
            } catch (SQLException e) {
                String logMessage="Database error: " + e.getMessage();
                System.out.println(logMessage);
                notifyObserversWithLog(logMessage);
            }
        }
        return messages;
    }

    void handleChatMessage(WebSocket conn, Message message) {
        String sql = "INSERT INTO chat_messages (chat_id, message, username_from) VALUES (?, ?, ?)";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(message.getTo())); // chat_id
                stmt.setString(2, message.getContent()); // message
                stmt.setString(3, message.getFrom()); // username_from
                stmt.executeUpdate();
                conn.send("Message sent successfully");
            } catch (SQLException e) {
                String logMessage="Database error: " + e.getMessage();
                System.out.println(logMessage);
                notifyObserversWithLog(logMessage);
                conn.send("Failed to send message");
            }
        }
        updateChatList();
    }

    void processChatUpdateRequest(WebSocket conn, ChatRequest chatRequest) {
        boolean success = chatManager.updateChat(chatRequest.getChatId(), chatRequest.getParameters());
        if (success) {
            conn.send("Chat updated successfully.");
        } else {
            conn.send("Failed to update chat.");
        }
        updateChatList();
    }

    void processChatDeletionRequest(WebSocket conn, ChatRequest chatRequest) {
        boolean success = chatManager.deleteChat(chatRequest.getChatId());
        if (success) {
            conn.send("Chat deleted successfully.");
        } else {
            conn.send("Failed to delete chat.");
        }
        updateChatList();
    }

    void processGetChatsRequest(WebSocket conn, ChatRequest chatRequest) {
        List<Chat> chats = chatManager.getUserChats(chatRequest.getUsername1());
        try {
            String responseXml = XMLUtil.toXML(new ChatListResponse(chats));
            conn.send(responseXml);
        } catch (JAXBException e) {
            System.err.println("Error serializing chat list: " + e.getMessage());
            conn.send("Error processing your request for chat list");
        }
        updateChatList();
    }

    void processChatCreationRequest(WebSocket conn, ChatRequest chatRequest) {
        if(userExists(chatRequest.getUsername1()) && userExists(chatRequest.getUsername2())) {
            boolean chatExists = chatManager.chatExists(chatRequest.getUsername1(), chatRequest.getUsername2());
            if (chatExists) {
                conn.send("Chat already exists between " + chatRequest.getUsername1() + " and " + chatRequest.getUsername2() + ". Please find it in your chat list.");
            } else {
                boolean chatCreated = chatManager.createChat(chatRequest.getUsername1(), chatRequest.getUsername2());
                if (chatCreated) {
                    conn.send("Chat created successfully between " + chatRequest.getUsername1() + " and " + chatRequest.getUsername2());
                } else {
                    conn.send("Failed to create chat or chat already exists.");
                }
            }
            updateChatList();
        }
        else{
            conn.send("Problems with usernames");
        }

    }
    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Повертає true, якщо користувач знайдений
                }
            } catch (SQLException e) {
                System.err.println("Database error while checking user existence: " + e.getMessage());
            }
        }
        return false;

    }

    void sendDirectMessage(Message msg) {
        String recipientUsername= msg.getTo();
        int recipientPort = findPortByUsername(recipientUsername);
        System.out.println("Port: " + recipientPort);
        System.out.println(connections);
        connections.stream()
                .peek(ws -> System.out.println("Checking port: " + ws.getRemoteSocketAddress().getPort()))
                .filter(ws -> ws.getRemoteSocketAddress().getPort() == recipientPort)
                .findFirst()
                .ifPresent(ws -> {
                    try {
                        ws.send(XMLUtil.toXML(msg));
                        String logMessage="Message sent to "+ ws.getRemoteSocketAddress().getPort();
                        System.out.println(logMessage);
                        notifyObserversWithLog(logMessage);
                    } catch (JAXBException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    List<Chat> getUserChats(String username) {
        return chatManager.getUserChats(username);
    }

    int findPortByUsername(String username) {
        return dbManager.findPortByUsername(username);
    }

    void updateDatabase(String username, int port) {
        dbManager.updateConnectionInfo(username, port);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Get the remote connection port
        String logMessage = "Error from " + port + ": " + ex.getMessage();
        System.out.println(logMessage);
        notifyObserversWithMessage(logMessage);
    }


}