package ituvtu.chat;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.*;
import java.util.*;
import jakarta.xml.bind.JAXBException;

public class Client extends WebSocketClient {
    private static Client instance;
    private final Set<ClientObserver> observers = new HashSet<>();

    public static Client getInstance(String url) throws URISyntaxException {
        if (instance == null) {
            instance = new Client(url);
        }
        return instance;
    }

    Client(String url) throws URISyntaxException {
        super(new URI(url));
    }
    @Override
    public void onOpen(ServerHandshake handshake) {
        int port = getURI().getPort();
        System.out.println("Connected to the server. This client is connected to port: " + port);
        UserConnectionInfo info = new UserConnectionInfo(ClientApp.getUsername(), getURI().getPort());
        try {
            String xmlInfo = XMLUtil.toXML(info);
            send(xmlInfo);
            ClientApp.getController().requestUserChats();
        } catch (JAXBException e) {
            System.err.println("Error serializing connection info: " + e.getMessage());
        }
    }

    public void addObserver(ClientObserver observer) {
        observers.add(observer);
        System.out.println("Observer added: " + observer.getClass().getName());
    }

    private void notifyObservers(String message) {
        System.out.println("Notifying observers...");
        for (ClientObserver observer : observers) {
            System.out.println("\tNotifying observer: " + observer);
            observer.onMessage(message);
        }
    }

    @Override
    public void onMessage(String message) {
        notifyObservers(message);
    }

    public void sendMessage(String from, String recipient, String content) {
        try {
            Message msg = new Message(from, recipient, content);
            String xmlMessage = XMLUtil.toXML(msg);
            send(xmlMessage);
        } catch (JAXBException e) {
            System.err.println("Error serializing message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from the server: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error occurred: " + ex.getMessage());
    }
}