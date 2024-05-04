package ituvtu.chat;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import jakarta.xml.bind.*;

public class Server extends WebSocketServer {
    private final Set<WebSocket> connections;
    private final Set<ServerObserver> observers;  // Set of observers
    final Connection dbConn = DatabaseConnection.getConnection();
    JAXBContext context = JAXBContext.newInstance(Message.class, UserConnectionInfo.class);

    public Server(int port) throws JAXBException {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        observers = new HashSet<>();
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
    public void onMessage(WebSocket conn, String message) {
        try {
            if (message.contains("<userConnectionInfo>")) {
                InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
                int port = remoteAddress.getPort();  // Get the remote connection port
                UserConnectionInfo info = XMLUtil.fromXML(message, UserConnectionInfo.class);
                updateDatabase(info.getUsername(), port);
            } else if (message.contains("<message>")) {
                Message msg = XMLUtil.fromXML(message, Message.class);
                String logMessage = "Message from " + msg.getFrom() + " to "+msg.getTo()+": " + msg.getContent();
                System.out.println(logMessage);
                notifyObservers(logMessage);
                sendDirectMessage(msg);
            }
        } catch (JAXBException e) {
            System.out.println("Error parsing XML: " + e.getMessage());
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

    private int findPortByUsername(String username) {
        int userPort = -1;  // -1 can be used as an indicator that the port was not found

        // SQL query to get user port by name
        String sql = "SELECT userport FROM connection WHERE username = ?";

        // Use try-with-resources to automatically close JDBC resources
        if (dbConn != null) {
            try (
                 PreparedStatement pstmt = dbConn.prepareStatement(sql)) {

                // Setting query parameters
                pstmt.setString(1, username);

               // Execution of the request
                ResultSet rs = pstmt.executeQuery();

                // Reading the results
                if (rs.next()) {
                    userPort = rs.getInt("userport");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }

        // Return the port, or -1 if the port is not found
        return userPort;
    }

    private void updateDatabase(String username, int port) {
        String sql = "INSERT INTO connection (username, userport) VALUES (?, ?) ON DUPLICATE KEY UPDATE userport = ?";
        if (dbConn != null) {
            try (
                 PreparedStatement stmt = dbConn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setInt(2, port);
                stmt.setInt(3, port);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }
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

    @Override
    public void onStart() {
        String logMessage = "Server started successfully on port: " + getPort();
        System.out.println(logMessage);
        notifyObservers(logMessage);
    }
}


