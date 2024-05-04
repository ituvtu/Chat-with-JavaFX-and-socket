package ituvtu.chat;

import javafx.fxml.*;
import javafx.scene.control.*;
import jakarta.xml.bind.*;


import java.io.StringReader;

import static ituvtu.chat.ClientApp.username;

public class ClientController implements ClientObserver {
    private static ClientController instance;

    @FXML
    public TextField recipientField;
    public Button sendButton;
    @FXML
    private TextArea messagesArea;
    @FXML
    private TextField inputField;

    private Client client;  // Variable to hold a reference to the client

    public ClientController() {
    }

    public static ClientController getInstance() {
        if (instance == null) {
            instance = new ClientController();
        }
        return instance;
    }

    public void onMessage(String xmlMessage) {
        try {
            JAXBContext context = JAXBContext.newInstance(Message.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlMessage);
            Message message = (Message) unmarshaller.unmarshal(reader);

            // Тепер message об'єкт містить дані з XML
            displayMessage("Message from " + message.getFrom() + ": " + message.getContent());
        } catch (JAXBException e) {
            displayMessage("Error parsing XML: " + e.getMessage());
        }
    }

    private void displayMessage(String text) {
        messagesArea.appendText(text + "\n");
    }
    public void onMessage1(String message) {
        System.out.println("onMessage called");
        try {
            if (message.contains("<message>")) {
                Message msg = XMLUtil.fromXML(message, Message.class);
                String displayMessage = "Message from " + msg.getFrom() + ": " + msg.getContent();
                //updateTextArea(displayMessage);
                client.onMessage(message);  // Якщо ще потрібно передати повідомлення нижче по ланцюгу
            } else if (message.contains("<userConnectionInfo>")) {
                UserConnectionInfo info = XMLUtil.fromXML(message, UserConnectionInfo.class);
                //updateTextArea("Connection info received: " + info.getUsername() + " on port " + info.getPort());
            }
        } catch (JAXBException e) {
            //updateTextArea("Error parsing XML: " + e.getMessage());
        }
    }
    @FXML
    public void onSend() {
        if (client != null && client.isOpen()) {
            String recipient = recipientField.getText();
            String message = inputField.getText();
            client.sendMessage(username, recipient, message);
            inputField.clear();
            recipientField.clear();
        }
    }

    public void setClient(Client client) {
        this.client=client;
    }
}
