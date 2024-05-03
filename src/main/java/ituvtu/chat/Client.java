package ituvtu.chat;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class Client extends WebSocketClient {

    public Client(String url) throws URISyntaxException {
        super(new URI(url));
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        int port = getURI().getPort();
        System.out.println("Connected to the server. This client is connected to port: " + port);
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        ClientApp.getController().updateTextArea(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from the server");
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Error occurred: " + ex.getMessage());
    }

    public void close() {
        super.close();
    }

    public boolean isOpen() {
        return super.isOpen();
    }

}
