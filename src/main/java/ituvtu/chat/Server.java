package ituvtu.chat;

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class Server extends WebSocketServer {

    private Set<WebSocket> connections;
    private Set<ServerObserver> observers;  // Множина спостерігачів

    public Server(int port) {
        super(new InetSocketAddress(port));
        connections = new HashSet<>();
        observers = new HashSet<>();
    }

    public void addObserver(ServerObserver observer) {
        observers.add(observer);
        System.out.println("Observer added: " + observer.getClass().getName());
    }


    public void removeObserver(ServerObserver observer) {
        observers.remove(observer);
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
        InetSocketAddress remoteAddress = (InetSocketAddress) conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
        String logMessage = "New connection: " + port;
        System.out.println("New connection: " + port);
        notifyObservers(logMessage);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        InetSocketAddress remoteAddress = (InetSocketAddress) conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
        String logMessage = "Closed connection: " + port;
        System.out.println(logMessage);
        notifyObservers(logMessage);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        InetSocketAddress remoteAddress = (InetSocketAddress) conn.getRemoteSocketAddress();
        int port = remoteAddress.getPort();  // Отримуємо порт віддаленого з'єднання
        String logMessage = "Message from " + port + ": " + message;
        System.out.println(logMessage);
        notifyObservers(logMessage);
        for (WebSocket sock : connections) {
            sock.send("Server received: " + message);
        }
    }


    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
        }
        assert conn != null;
        InetSocketAddress remoteAddress = (InetSocketAddress) conn.getRemoteSocketAddress();
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


