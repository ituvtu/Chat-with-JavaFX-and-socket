package ituvtu.chat;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;


public class ClientController implements ClientObserver {
    private static ClientController instance;
    @FXML
    private ListView<ChatDisplayData> chatListView;
    @FXML
    private VBox messagesArea;
    @FXML
    private TextField inputField;
    private Client client;
    public TextField newChatUsername;
    public TextArea logMessagesArea;

    public ClientController() {}

    public static synchronized ClientController getInstance() {
        if (instance == null) {
            instance = new ClientController();
        }
        return instance;
    }

    @FXML
    public void initialize() {
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadChatMessages(newSelection.chatId());
            }
        });
    }
    public void setClient(Client client) {
        this.client = client;
    }

    private void loadChatMessages(int chatId) {
        messagesArea.getChildren().clear();
        try {
            ChatRequest request = new ChatRequest("getMessages", chatId, ClientApp.getUsername());
            String requestXml = XMLUtil.toXML(request);
            client.send(requestXml);
        } catch (JAXBException e) {
            displayLogMessage("Error requesting chat messages: " + e.getMessage());
        }
    }

    @Override
    public void onMessage(String xmlMessage) {
        if (xmlMessage.contains("<chatListResponse>")) {
            processChatListResponse(xmlMessage);
        } else if (xmlMessage.contains("<message>")) {
            processMessage(xmlMessage);
        } else if (xmlMessage.contains("<messagesResponse>")) {
            processMessagesResponse(xmlMessage);
        } else //noinspection StatementWithEmptyBody
            if(xmlMessage.contains("<messagesResponse/>")) {
            //We don't need to output empty messages
        } else //noinspection StatementWithEmptyBody
                if(xmlMessage.contains("<chatListResponse/>")){
            //We don't need to output empty chatlist
        }
        else {
            displayLogMessage(xmlMessage);
        }
    }

    private void processChatListResponse(String xmlMessage) {
        try {
            ChatListResponse response = XMLUtil.fromXML(xmlMessage, ChatListResponse.class);
            updateChatList(response.getChats());
        } catch (Exception e) {
            displayLogMessage("Error parsing chat list: " + e.getMessage());
        }
    }

    private void processMessage(String xmlMessage) {
        try {
            Message message = XMLUtil.fromXML(xmlMessage, Message.class);
            displayMessage(message);
        } catch (Exception e) {
            displayLogMessage("Error parsing XML: " + e.getMessage());
        }
    }

    private void processMessagesResponse(String xmlMessage) {
        try {
            JAXBContext context = JAXBContext.newInstance(MessagesResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlMessage);
            MessagesResponse response = (MessagesResponse) unmarshaller.unmarshal(reader);
            updateMessagesArea(response.getMessages());
        } catch (Exception e) {
            displayLogMessage("Error parsing messages: " + e.getMessage());
        }
    }
    private void updateChatList(List<Chat> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            for (Chat chat : chats) {
                chatListView.getItems().add(new ChatDisplayData(chat.getChat_id(), chat.getChatDisplayName(ClientApp.getUsername())));
            }
        });
    }
    private void updateMessagesArea(List<Message> messages) {
        Platform.runLater(() -> {
            messagesArea.getChildren().clear();
            for (Message message : messages) {
                String displayText = Objects.equals(message.getFrom(), ClientApp.getUsername()) ? "You: " + message.getContent() : message.getFrom() + ": " + message.getContent();
                displayMessage(message);
            }
        });
    }

    private void displayMessage(Message message) {
        Platform.runLater(() -> {
            HBox messageBox = new HBox();
            Label messageLabel = new Label();

            if (Objects.equals(message.getFrom(), ClientApp.getUsername())) {
                messageLabel.setText("You: " + message.getContent());
                messageBox.setAlignment(Pos.CENTER_RIGHT);
                messageLabel.setStyle("-fx-background-color: lightblue; -fx-padding: 10; -fx-background-radius: 10;");
            } else {
                messageLabel.setText(message.getFrom() + ": " + message.getContent());
                messageBox.setAlignment(Pos.CENTER_LEFT);
                messageLabel.setStyle("-fx-background-color: lightgray; -fx-padding: 10; -fx-background-radius: 10;");
            }

            messageBox.getChildren().add(messageLabel);
            messagesArea.getChildren().add(messageBox);
        });
    }

    private void displayLogMessage(String text) {
        Platform.runLater(() -> logMessagesArea.appendText(text + "\n"));
    }

    public void setChatList(List<String> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            int chatId = 1; // Приклад генерації ідентифікатора для кожного чату
            for (String chat : chats) {
                chatListView.getItems().add(new ChatDisplayData(chatId++, chat));
            }
        });
    }


    @FXML
    void requestUserChats() {
        System.out.println(ClientApp.getUsername());
        if (client != null && client.isOpen()) {
            try {
                ChatRequest request = new ChatRequest("getChats", ClientApp.getUsername(), null);
                String requestXml = XMLUtil.toXML(request);
                client.send(requestXml);
            } catch (JAXBException e) {
                displayLogMessage("Error creating chat list request: " + e.getMessage());
            }
        } else {
            displayLogMessage("Connection is not established. Please connect to the server first.");
        }
    }

    @FXML
    public void onSend() {
        if (client != null && client.isOpen()) {
            String messageContent = inputField.getText().trim();
            if (!messageContent.isEmpty()) {
                String selectedChat = String.valueOf(chatListView.getSelectionModel().getSelectedItem());
                if (selectedChat != null) {
                    client.sendMessage(ClientApp.getUsername(), selectedChat, messageContent);
                    inputField.clear();
                    Message message = new Message(ClientApp.getUsername(), selectedChat, messageContent);
                    displayMessage(message);
                } else {
                    displayLogMessage("Select a chat to send the message.");
                }
            } else {
                displayLogMessage("Message cannot be empty.");
            }
        } else {
            displayLogMessage("No client connected.");
        }
    }


    @FXML
    private void createNewChat() {
        System.out.println("Creating new chat");
        String username2 = newChatUsername.getText().trim();
        if (!username2.isEmpty()) {
            try {
                ChatRequest chatRequest = new ChatRequest("createChat", ClientApp.getUsername(), username2);
                String chatRequestXml = XMLUtil.toXML(chatRequest);
                if (chatRequestXml != null) {
                    client.send(chatRequestXml);
                    requestUserChats();
                } else {
                    displayLogMessage("Failed to create XML request.\n");
                }
            } catch (Exception e) {
                displayLogMessage("Error creating XML request: " + e.getMessage() + "\n");
            }
            newChatUsername.clear();
        } else {
            displayLogMessage("Please enter a valid username.\n");
        }

    }

    private void initiateNewChat(int chatId, String username) {
        ChatDisplayData newChat = new ChatDisplayData(chatId, username);
        Platform.runLater(() -> {
            chatListView.getItems().add(newChat);
            logMessagesArea.appendText("New chat started with " + username + ".\n");
        });
    }

    @FXML
    private void deleteChat() {
        final int selectedIdx = chatListView.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            ChatDisplayData selectedChat = chatListView.getItems().get(selectedIdx);
            try {
                ChatRequest deleteRequest = new ChatRequest("deleteChat", selectedChat.chatId());
                String requestXml = XMLUtil.toXML(deleteRequest);
                client.send(requestXml);
                chatListView.getItems().remove(selectedIdx);
                displayLogMessage("Request to delete chat with " + selectedChat.displayName() + " sent.\n");
            } catch (JAXBException e) {
                displayLogMessage("Error creating XML for delete chat request: " + e.getMessage());
            }

        } else {
            displayLogMessage("Please select a chat to delete.\n");
        }
    }
}
