package ituvtu.chat;

import jakarta.xml.bind.*;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Server extends WebSocketServer {
    private static Server instance;
    final Set<WebSocket> connections;
    final Set<ServerObserver> observers;
    final DatabaseManager dbManager;

    public Server(int port) {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        observers = new HashSet<>();
        dbManager = DatabaseManager.getInstance();
    }

    public static synchronized Server getInstance(int port){
        if (instance == null) {
            instance = new Server(port);
        }
        return instance;
    }

    void notifyObserversWithLog(String message, String styleClass) {
        observers.forEach(observer -> observer.displayLogMessage(message, styleClass));
    }

    @Override
    public void onStart() {
        String logMessage = "Server started successfully on port: " + getPort();
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage, "log-message-color-success");
        updateChatList();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        String logMessage = "New connection: " + getPortConn(conn);
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage, "log-message-color-success");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        String logMessage = "Closed connection: " + getPortConn(conn);
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage, "log-message-color-warning");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        int port = conn.getRemoteSocketAddress().getPort();
        String logMessage = "Error from " + port + ": " + ex.getMessage();
        System.out.println(logMessage);
        notifyObserversWithLog(logMessage, "log-message-color-info");
    }

    void updateChatList() {
        List<ChatDisplayData> chats = dbManager.getAllChats();
        notifyObserversAboutChats(chats);
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

    void notifyObservers(WebSocket conn, String message) {
        observers.forEach(observer -> {
            try {
                observer.onMessage(conn, message);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void notifyObserversWithMessage(Message message) {
        observers.forEach(observer -> {
            if (observer instanceof ServerController controller) {
                if (controller.isCurrentChat(message.getChatId())) {
                    controller.displayMessage(message);
                }
            }
        });
    }

    int getPortConn(WebSocket conn) {
        return conn.getRemoteSocketAddress().getPort();
    }

    @Override
    public void onMessage(WebSocket conn, String input) {
        notifyObservers(conn, input);
    }

    void handleUserConnectionInfo(WebSocket conn, String input) throws JAXBException {
        UserConnectionInfo info = XMLUtil.fromXML(input, UserConnectionInfo.class);
        dbManager.updateConnectionInfo(info.getUsername(), getPortConn(conn));
    }

    void recordMessageInDatabase(Message msg) {
        dbManager.recordMessage(msg);
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
        List<Message> messages = dbManager.getMessagesFromDatabase(chatRequest.getChatId());
        try {
            String messagesXml = XMLUtil.toXML(new MessagesResponse(messages));
            conn.send(messagesXml);
        } catch (JAXBException e) {
            conn.send("Error serializing messages");
        }
    }

    String processServerGetMessagesRequest(int chatId) throws JAXBException {
        return XMLUtil.toXML(new MessagesResponse(dbManager.getMessagesFromDatabase(chatId)));
    }

    void handleChatMessage(WebSocket conn, Message message) {
        dbManager.recordMessage(message);
        conn.send("Message sent successfully");
        updateChatList();
    }

    void processChatUpdateRequest(WebSocket conn, ChatRequest chatRequest) {
        boolean success = dbManager.updateChat(chatRequest.getChatId(), chatRequest.getParameters());
        if (success) {
            conn.send("Chat updated successfully.");
        } else {
            conn.send("Failed to update chat.");
        }
        updateChatList();
    }

    void processChatDeletionRequest(WebSocket conn, ChatRequest chatRequest) {
        boolean success = dbManager.deleteChat(chatRequest.getChatId());
        if (success) {
            conn.send("Chat deleted successfully.");
        } else {
            conn.send("Failed to delete chat.");
        }
        updateChatList();
    }

    void processGetChatsRequest(WebSocket conn, ChatRequest chatRequest) {
        List<Chat> chats = dbManager.getUserChats(chatRequest.getUsername1());
        try {
            String responseXml = XMLUtil.toXML(new ChatListResponse(chats));
            conn.send(responseXml);
        } catch (JAXBException e) {
            String logmessage = "Error serializing chat list: " + e.getMessage();
            notifyObserversWithLog(logmessage, "log-message-color-info");
            conn.send("Error processing your request for chat list");
        }
        updateChatList();
    }

    void processChatCreationRequest(WebSocket conn, ChatRequest chatRequest) {
        if (dbManager.userExists(chatRequest.getUsername1()) && dbManager.userExists(chatRequest.getUsername2())) {
            boolean chatExists = dbManager.chatExists(chatRequest.getUsername1(), chatRequest.getUsername2());
            if (chatExists) {
                conn.send("Chat already exists between " + chatRequest.getUsername1() + " and " + chatRequest.getUsername2() + ". Please find it in your chat list.");
            } else {
                boolean chatCreated = dbManager.createChat(chatRequest.getUsername1(), chatRequest.getUsername2());
                if (chatCreated) {
                    conn.send("Chat created successfully between " + chatRequest.getUsername1() + " and " + chatRequest.getUsername2());
                } else {
                    conn.send("Failed to create chat or chat already exists.");
                }
            }
            updateChatList();
        } else {
            conn.send("Problems with usernames");
        }
    }

    public boolean userExists(String username) {
        return dbManager.userExists(username);
    }

    void sendDirectMessage(Message msg) {
        String recipientUsername = msg.getTo();
        int recipientPort = dbManager.findPortByUsername(recipientUsername);

        connections.stream()
                .peek(ws -> System.out.println("Checking port: " + ws.getRemoteSocketAddress().getPort()))
                .filter(ws -> ws.getRemoteSocketAddress().getPort() == recipientPort)
                .findFirst()
                .ifPresent(ws -> {
                    try {
                        ws.send(XMLUtil.toXML(msg));
                        String logMessage = "Message sent to " + ws.getRemoteSocketAddress().getPort();
                        notifyObserversWithLog(logMessage, "log-message-color-success");
                    } catch (JAXBException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    List<Chat> getUserChats(String username) {
        return dbManager.getUserChats(username);
    }

    int findPortByUsername(String username) {
        return dbManager.findPortByUsername(username);
    }

    void updateDatabase(String username, int port) {
        dbManager.updateConnectionInfo(username, port);
    }
}
