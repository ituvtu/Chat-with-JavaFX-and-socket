package ituvtu.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerController {

    @FXML
    private TextArea messageArea; // Assuming this is correctly defined in your FXML and linked

    private final Set<PrintWriter> clientWriters = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void initialize() {
        new Thread(this::startServer).start();
    }

    private void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            Platform.runLater(() -> messageArea.appendText("Server error: " + e.getMessage() + "\n"));
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

                clientWriters.add(writer);

                String message;
                while ((message = reader.readLine()) != null) {
                    final String finalMessage = message; // Ensure message is effectively final for use in lambda
                    clientWriters.forEach(w -> w.println(finalMessage));
                    Platform.runLater(() -> messageArea.appendText(finalMessage + "\n"));
                }
                clientWriters.remove(writer);
            } catch (IOException e) {
                Platform.runLater(() -> messageArea.appendText("Read error: " + e.getMessage() + "\n"));
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Platform.runLater(() -> messageArea.appendText("Socket close error: " + e.getMessage() + "\n"));
                }
            }
        }
    }
}

