package ituvtu.chat;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.control.*;
import jakarta.xml.bind.*;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;


public class ClientController implements ClientObserver {
    private static ClientController instance;

    @FXML
    private ListView<ChatDisplayData> chatListView;

    @FXML
    private TextArea messagesArea;

    @FXML
    private TextField inputField;

    private Client client;

    public Button sendButton;

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
                loadChatMessages(newSelection.getChatId());
            }
        });
    }
    public void setClient(Client client) {
        this.client = client;
    }
    private void loadChatMessages(int chatId) {
        messagesArea.clear();  // Очистка повідомлень
        try {
            ChatRequest request = new ChatRequest("getMessages", chatId, ClientApp.getUsername());
            String requestXml = XMLUtil.toXML(request);
            client.send(requestXml);
        } catch (JAXBException e) {
            displayMessage("Error requesting chat messages: " + e.getMessage());
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
        } else {
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
            displayMessage(message.getFrom() + ": " + message.getContent());
        } catch (Exception e) {
            displayLogMessage("Error parsing XML: " + e.getMessage());
        }
    }
    private void processMessagesResponse(String xmlMessage) {
        try {
            JAXBContext context = JAXBContext.newInstance(MessagesResponse.class, Message.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlMessage);
            MessagesResponse response = (MessagesResponse) unmarshaller.unmarshal(reader);
            updateMessagesArea(response.getMessages());
        } catch (Exception e) {
            displayMessage("Error parsing messages: " + e.getMessage());
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
            messagesArea.clear();
            for (Message message : messages) {
                String displayText = Objects.equals(message.getFrom(), ClientApp.getUsername()) ? "You: " + message.getContent() : message.getFrom() + ": " + message.getContent();
                displayMessage(displayText);
            }
        });
    }

    private void displayMessage(String text) {
        Platform.runLater(() -> messagesArea.appendText(text + "\n"));
    }

    private void displayLogMessage(String text) {
        Platform.runLater(() -> messagesArea.appendText(text + "\n"));
    }

    public void setChatList(List<String> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            chatListView.getItems().addAll((ChatDisplayData) chats);
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
                try {
                    // Отримання вибраного чату
                    String selectedChat = String.valueOf(chatListView.getSelectionModel().getSelectedItem());
                    if (selectedChat != null) {
                        // Ідентифікатор чату може бути вилучений або оброблений відповідним чином тут
                        // Наприклад, ви могли б зберігати id чату замість назви

                        // Створення та відправка повідомлення
                        Message message = new Message(ClientApp.getUsername(), selectedChat, messageContent);
                        String xmlMessage = XMLUtil.toXML(message);
                        client.send(xmlMessage);

                        // Очистка поля введення після відправки
                        inputField.clear();

                        // Опціонально: Додати повідомлення до вікна чату
                        displayMessage("You: " + messageContent);
                    } else {
                        displayLogMessage("Select a chat to send the message.");
                    }
                } catch (JAXBException e) {
                    displayLogMessage("Error creating XML for message: " + e.getMessage());
                }
            } else {
                displayLogMessage("Message cannot be empty.");
            }
        } else {
            displayLogMessage("No client connected.");
        }
    }


    public void closeWindow(ActionEvent actionEvent) {
    }

    public void maximizeWindow(ActionEvent actionEvent) {
    }

    public void minimizeWindow(ActionEvent actionEvent) {
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
                    displayLogMessage("Chat request sent");
                    displayLogMessage(chatRequestXml);
                    displayLogMessage("Request to create chat with " + username2 + " sent.\n");
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

    private boolean userExists(String username) {
        // Here, the logic for checking the existence of the user should be implemented
        return true; // cap
    }

    private void initiateNewChat(int chatId, String username) {
        ChatDisplayData newChat = new ChatDisplayData(chatId, username);
        Platform.runLater(() -> {
            chatListView.getItems().add(newChat);
            messagesArea.appendText("New chat started with " + username + ".\n");
        });
    }

    @FXML
    private void deleteChat() {
        final int selectedIdx = chatListView.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            String itemToRemove = String.valueOf(chatListView.getItems().get(selectedIdx));

            // The logic for deleting a chat from the database can be added here
            chatListView.getItems().remove(selectedIdx);
            displayLogMessage("Chat with " + itemToRemove + " has been removed.\n");
        } else {
            displayLogMessage("Please select a chat to delete.\n");
        }
    }

}
