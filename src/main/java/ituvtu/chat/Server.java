package ituvtu.chat;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import jakarta.xml.bind.*;

public class Server extends WebSocketServer {
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

    @Override
    public void onStart() {
        String logMessage = "Server started successfully on port: " + getPort();
        System.out.println(logMessage);
        notifyObservers(logMessage);
        updateChatList();
    }

    private void updateChatList() {
        List<String> chats = getAllChats();
        notifyObserversAboutChats(chats);
    }

    private List<String> getAllChats() {
        List<String> chats = new ArrayList<>();
        String sql = "SELECT chat_id, username_first, username_second FROM chat";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String chatInfo = "Chat " + rs.getInt("chat_id") + " between " + rs.getString("username_first") + " and " + rs.getString("username_second");
                    chats.add(chatInfo);
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }
        return chats;
    }

    private void notifyObserversAboutChats(List<String> chats) {
        for (ServerObserver observer : observers) {
            observer.updateChatList(chats);
        }
    }

    public void addObserver(ServerObserver observer) {
        observers.add(observer);
        System.out.println("Observer added: " + observer.getClass().getName());
    }

    private void notifyObservers(String message) {
        System.out.println("Notifying observers...");
        for (ServerObserver observer : observers) {
            System.out.println("\tNotifying observer: " + observer);
            observer.onMessage(message);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Get the remote connection port
        connections.add(conn);
        String logMessage = "New connection: " + port;
        System.out.println("New connection: " + port);
        notifyObservers(logMessage);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Get the remote connection port
        String logMessage = "Closed connection: " + port;
        System.out.println(logMessage);
        notifyObservers(logMessage);
    }

    @Override
    public void onMessage(WebSocket conn, String input) {
        try {
            if (input.contains("<userConnectionInfo>")) {
                handleUserConnectionInfo(conn, input);
            } else if (input.contains("<message>")) {
                handleMessage(conn, input);
            } else if (input.contains("<chatRequest>")) {
                handleChatRequest(conn, input);
            } else {
                System.out.println("Input is not supported format");
                conn.send("Unsupported input format");
            }
        } catch (JAXBException e) {
            System.out.println("Error parsing XML: " + e.getMessage());
            conn.send("Error processing your request due to XML parsing error");
        }
    }

    private void handleUserConnectionInfo(WebSocket conn, String input) throws JAXBException {
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();
        UserConnectionInfo info = XMLUtil.fromXML(input, UserConnectionInfo.class);
        updateDatabase(info.getUsername(), port);
    }

    private void handleMessage(WebSocket conn, String input) {
        try {
            Message msg = XMLUtil.fromXML(input, Message.class);
            String logMessage = "Message from " + msg.getFrom() + " to " + msg.getTo() + ": " + msg.getContent();
            System.out.println(logMessage);
            notifyObservers(logMessage);
            sendDirectMessage(msg);
            recordMessageInDatabase(msg);
        } catch (JAXBException e) {
            System.err.println("Error parsing XML: " + e.getMessage());
            conn.send("Error processing your XML input");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            conn.send("An unexpected error occurred");
        }
    }

    private void recordMessageInDatabase(Message msg) {
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
                    System.err.println("Database error: " + e.getMessage());
                }
            }
        } else {
            System.out.println("Chat ID not found for users: " + msg.getFrom() + " and " + msg.getTo());
        }
    }

    private void handleChatRequest(WebSocket conn, String input) throws JAXBException {
        ChatRequest chatRequest = XMLUtil.fromXML(input, ChatRequest.class);

        switch (chatRequest.getAction()) {
            case "createChat":
                processChatCreationRequest(conn, chatRequest);
                break;
            case "updateChat":
                processChatUpdateRequest(conn, chatRequest);
                break;
            case "deleteChat":
                processChatDeletionRequest(conn, chatRequest);
                break;
            case "getChats":
                processGetChatsRequest(conn, chatRequest);
                break;
            case "getMessages":
                processGetMessagesRequest(conn, chatRequest);
                break;
            default:
                conn.send("Unsupported chat request action: " + chatRequest.getAction());
                break;
        }
    }

    private void processGetMessagesRequest(WebSocket conn, ChatRequest chatRequest) {
        List<Message> messages = getMessagesFromDatabase(chatRequest.getChatId());
        try {
            String messagesXml = XMLUtil.toXML(new MessagesResponse(messages));
            System.out.println("Sending XML: " + messagesXml);
            conn.send(messagesXml);

            conn.send(messagesXml);
        } catch (JAXBException e) {
            conn.send("Error serializing messages");
        }
    }

    private List<Message> getMessagesFromDatabase(Integer chatId) {
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
                System.err.println("Database error: " + e.getMessage());
            }
        }
        return messages;
    }

    private void handleChatMessage(WebSocket conn, Message message) {
        // Тут код для вставки повідомлення в базу даних
        String sql = "INSERT INTO chat_messages (chat_id, message, username_from) VALUES (?, ?, ?)";
        if (dbConn != null) {
            try (PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                stmt.setInt(1, Integer.parseInt(message.getTo())); // chat_id
                stmt.setString(2, message.getContent()); // message
                stmt.setString(3, message.getFrom()); // username_from
                stmt.executeUpdate();
                conn.send("Message sent successfully");
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                conn.send("Failed to send message");
            }
        }
    }

    private void processChatUpdateRequest(WebSocket conn, ChatRequest chatRequest) {
        // Логіка для оновлення чату
        boolean success = chatManager.updateChat(chatRequest.getChatId(), chatRequest.getParameters());
        if (success) {
            conn.send("Chat updated successfully.");
        } else {
            conn.send("Failed to update chat.");
        }
    }

    private void processChatDeletionRequest(WebSocket conn, ChatRequest chatRequest) {
        // Логіка для видалення чату
        boolean success = chatManager.deleteChat(chatRequest.getChatId());
        if (success) {
            conn.send("Chat deleted successfully.");
        } else {
            conn.send("Failed to delete chat.");
        }
    }

    private void processGetChatsRequest(WebSocket conn, ChatRequest chatRequest) {
        // Отримання списку чатів для користувача
        List<Chat> chats = chatManager.getUserChats(chatRequest.getUsername1());
        try {
            String responseXml = XMLUtil.toXML(new ChatListResponse(chats));
            conn.send(responseXml);
        } catch (JAXBException e) {
            System.err.println("Error serializing chat list: " + e.getMessage());
            conn.send("Error processing your request for chat list");
        }
    }

    private void processChatCreationRequest(WebSocket conn, ChatRequest chatRequest) {
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
    }


    private void sendDirectMessage(Message msg) {
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
                        System.out.println("Message sent to "+ ws.getRemoteSocketAddress().getPort());
                    } catch (JAXBException e) {
                        System.out.println("Message NOT sent");
                        throw new RuntimeException(e);
                    }
                });

    }

    private List<Chat> getUserChats(String username) {
        return chatManager.getUserChats(username);
    }

    private int findPortByUsername(String username) {
        return dbManager.findPortByUsername(username);
    }

    private void updateDatabase(String username, int port) {
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
        notifyObservers(logMessage);
    }


}


