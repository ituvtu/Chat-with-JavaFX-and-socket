package ituvtu.chat;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ClientController implements ClientObserver {
    private static ClientController instance;
    @FXML
    private ListView<ChatDisplayData> chatListView;
    @FXML
    private VBox messagesArea;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField inputField;
    private Client client;
    public TextField newChatUsername;
    public TextArea logMessagesArea;
    private LocalDate currentDisplayedDate = null;
    private int currentChatId = -1; // Зберігання ID поточного чату

    public ClientController() {}

    public static synchronized ClientController getInstance() {
        if (instance == null) {
            instance = new ClientController();
        }
        return instance;
    }

    @FXML
    public void initialize() {
        chatListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<ChatDisplayData> call(ListView<ChatDisplayData> listView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(ChatDisplayData item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            setText(item.toString());
                        }
                    }
                };
            }
        });

        // Загрузка стилів для всіх елементів
        String stylesheet = Objects.requireNonNull(getClass().getResource("client-styles.css")).toExternalForm();
        scrollPane.getStylesheets().add(stylesheet);
        chatListView.getStylesheets().add(stylesheet);
        logMessagesArea.getStylesheets().add(stylesheet);

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentChatId = newSelection.chatId(); // Оновлення ID поточного чату
                loadChatMessages(newSelection.chatId());
            }
        });

        // Make the ScrollPane always scroll to the bottom when new messages are added
        messagesArea.heightProperty().addListener((observable, oldValue, newValue) -> scrollPane.setVvalue(1.0));
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
        } else if (xmlMessage.contains("<messagesResponse/>")) {
            // We don't need to output empty messages
        } else if (xmlMessage.contains("<chatListResponse/>")) {
            // We don't need to output empty chat list
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
            ChatDisplayData selectedChat = chatListView.getSelectionModel().getSelectedItem();
            if (selectedChat != null && message.getChatId() == selectedChat.chatId()) {
                displayMessage(message);
            }
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
                chatListView.getItems().add(new ChatDisplayData(chat.getChat_id(), chat.getChatDisplayName(ClientApp.getUsername()), chat.getUsernameFirst(), chat.getUsernameSecond()));
            }
        });
    }

    private void updateMessagesArea(List<Message> messages) {
        Platform.runLater(() -> {
            messagesArea.getChildren().clear();
            currentDisplayedDate = null;
            for (Message message : messages) {
                // Перевірка чи повідомлення належить до поточного чату
                if (currentChatId == message.getChatId()) {
                    displayMessage(message);
                }
            }
        });
    }

    private void displayMessage(Message message) {
        Platform.runLater(() -> {
            LocalDateTime timestamp = message.getTimestamp();
            LocalDate messageDate = timestamp.toLocalDate();

            // Додавання лейбла з датою, якщо дата змінилася
            if (currentDisplayedDate == null || !currentDisplayedDate.equals(messageDate)) {
                currentDisplayedDate = messageDate;
                String formattedDate = formatDate(messageDate);
                Label dateLabel = new Label(formattedDate);
                dateLabel.setAlignment(Pos.CENTER);
                dateLabel.getStyleClass().add("date-label");
                HBox dateBox = new HBox();
                dateBox.setAlignment(Pos.CENTER);
                dateBox.getChildren().add(dateLabel);
                messagesArea.getChildren().add(dateBox);
            }

            VBox messageBox = new VBox();
            Label senderLabel = new Label(message.getFrom());
            senderLabel.getStyleClass().add("sender-label");

            Text messageText = new Text(message.getContent());
            messageText.setWrappingWidth(300);
            messageText.getStyleClass().add("message-text");

            TextFlow messageFlow = new TextFlow(messageText);
            messageFlow.setMaxWidth(300);
            messageFlow.getStyleClass().add("message-text-flow");

            Label timeLabel = new Label(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.getStyleClass().add("time-label");

            HBox messageContainer = new HBox();
            messageContainer.setMaxWidth(300);

            StackPane textContainer = new StackPane(messageFlow);
            textContainer.setMaxWidth(300);

            messageContainer.getChildren().add(textContainer);

            if (message.getFrom().equals(ClientApp.getUsername())) {
                messageBox.setAlignment(Pos.CENTER_LEFT);
                messageContainer.setAlignment(Pos.CENTER_LEFT);
                textContainer.getStyleClass().add("text-container-left");
            } else {
                messageBox.setAlignment(Pos.CENTER_RIGHT);
                messageContainer.setAlignment(Pos.CENTER_RIGHT);
                textContainer.getStyleClass().add("text-container-right");
            }

            messageBox.getChildren().addAll(senderLabel, messageContainer, timeLabel);
            messagesArea.getChildren().add(messageBox);
        });
    }


    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault());
        return date.format(formatter);
    }

    private void displayLogMessage(String text) {
        Platform.runLater(() -> logMessagesArea.appendText(text + "\n"));
    }

    public void setChatList(List<String> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            int chatId = 1;
            for (String chat : chats) {
                chatListView.getItems().add(new ChatDisplayData(chatId++, chat, null, null));
            }
        });
    }

    @FXML
    void requestUserChats() {
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
                ChatDisplayData selectedChat = chatListView.getSelectionModel().getSelectedItem();
                if (selectedChat != null) {
                    int chatId = selectedChat.chatId();  // Отримання chatId
                    Message message = new Message(ClientApp.getUsername(), selectedChat.displayName(), messageContent, chatId);
                    client.sendMessage(ClientApp.getUsername(), selectedChat.displayName(), messageContent, chatId);
                    inputField.clear();
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
        ChatDisplayData newChat = new ChatDisplayData(chatId, username, null, null);
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
