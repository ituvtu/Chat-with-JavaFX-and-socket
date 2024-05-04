package ituvtu.chat;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.*;
import jakarta.xml.bind.JAXBException;


public class Server extends WebSocketServer {
    private final Set<WebSocket> connections;
    private final Set<ServerObserver> observers;  // Множина спостерігачів

    public Server(int port) {
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
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
        String logMessage = "New connection: " + port;
        System.out.println("New connection: " + port);
        notifyObservers(logMessage);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
        String logMessage = "Closed connection: " + port;
        System.out.println(logMessage);
        notifyObservers(logMessage);
    }


    public void onMessage1(WebSocket conn, String message) {
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
        String logMessage = "Message from " + port + ": " + message;
        System.out.println(logMessage);
        notifyObservers(logMessage);
        for (WebSocket sock : connections) {
            sock.send("Server received: " + message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = XMLUtil.fromXML(message);
            String logMessage = "Message from " + msg.getFrom() + ": " + msg.getContent();
            System.out.println(logMessage);
            notifyObservers(logMessage);
            // Збережіть повідомлення в базу даних тут
        } catch (JAXBException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            conn.send("Error in message format");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        InetSocketAddress remoteAddress = conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
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


