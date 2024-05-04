package ituvtu.chat;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.*;
import jakarta.xml.bind.JAXBException;



public class Client extends WebSocketClient {
    public Client(String url) throws URISyntaxException {
        super(new URI(url));
    }
    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to the server. This client is connected to port: " + getURI().getPort());
    }
    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }
    public void sendMessage(String from, String content) {
        try {
            Message msg = new Message(from, content);
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