package ituvtu.chat;

import javafx.fxml.*;
import javafx.scene.control.*;
import java.io.*;
import java.net.*;

public class ClientController {

    @FXML
    private TextArea inputField;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea messageArea;
    @FXML
    private Button sendButton;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private String clientName;

    public void initialize() {
        try {
            inputField.setPrefRowCount(1);
            this.socket = new Socket("localhost", 5000);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Thread for reading messages
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
    @FXML
    private Label usernameLabel;

    public void setUsername(String username) {
        usernameLabel.setText(username);  // Set the username on a label or other component
    }
    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                String finalMessage = message;
                javafx.application.Platform.runLater(() -> messageArea.appendText(finalMessage + "\n"));
            }
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }
    public void setClientName(String name) {
        clientName = name;  // Setting the username received from the login form
    }

    public void sendMessage() {
        if (!inputField.getText().isEmpty()) {
            writer.println(clientName + ": " + inputField.getText());
            inputField.clear();
        }
    }

    // Closing resources
    public void shutdown() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

}



