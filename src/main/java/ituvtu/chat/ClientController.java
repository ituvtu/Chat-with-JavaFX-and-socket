package ituvtu.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ClientController {

    public Button sendButton;
    @FXML
    private TextArea messageArea;
    @FXML
    private TextField inputField;

    private Client client;  // Variable to hold a reference to the client

    @FXML
    public void onSend() {
        if (client != null && client.isOpen()) {
            client.send(inputField.getText());
            inputField.clear();
        }
    }

    public void setClient(Client client) {
        this.client = client;  // Method to install the client
    }

    public void updateTextArea(String message) {
        messageArea.appendText(message + "\n");
    }
}

