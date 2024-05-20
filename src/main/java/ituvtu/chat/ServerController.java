package ituvtu.chat;

import jakarta.xml.bind.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.util.Callback;
import org.java_websocket.WebSocket;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ServerController implements ServerObserver {
    private static ServerController instance;

    public ListView<ChatDisplayData> chatListView;
    @FXML
    public VBox messagesArea;
    @FXML
    private ScrollPane messageScrollPane;
    @FXML
    private ScrollPane logScrollPane;
    @FXML
    private VBox logMessagesArea;
    private Server server;
    private int currentChatId = -1;
    private LocalDate currentDisplayedDate = null;
    private String usernameFirst;
    private String usernameSecond;

    private ServerController(Server server) {
        this.server = server;
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

        logScrollPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("server-styles.css")).toExternalForm());
        logScrollPane.getStyleClass().add("vbox-with-border");
        chatListView.getStylesheets().add(Objects.requireNonNull(getClass().getResource("server-styles.css")).toExternalForm());
        chatListView.getStyleClass().add("vbox-with-border");
        messageScrollPane.getStylesheets().add(Objects.requireNonNull(getClass().getResource("server-styles.css")).toExternalForm());
        messageScrollPane.getStyleClass().add("vbox-with-border");

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                currentChatId = newSelection.chatId();
                usernameFirst = newSelection.usernameFirst();
                usernameSecond = newSelection.usernameSecond();
                try {
                    loadMessagesForChat(newSelection.chatId());
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
            } else {
                currentChatId = -1;
                usernameFirst = null;
                usernameSecond = null;
            }
        });

        messageScrollPane.setFitToWidth(true);
        logScrollPane.setFitToWidth(true);
    }

    private void loadMessagesForChat(int chatId) throws JAXBException {
        messagesArea.getChildren().clear();
        processMessagesResponse(server.processServerGetMessagesRequest(chatId));
    }

    private void processMessagesResponse(String xmlMessage) {
        try {
            JAXBContext context = JAXBContext.newInstance(MessagesResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlMessage);
            MessagesResponse response = (MessagesResponse) unmarshaller.unmarshal(reader);
            updateMessagesArea(response.getMessages());
        } catch (Exception e) {
            displayLogMessage("Error parsing messages: " + e.getMessage(), "tan");
        }
    }

    private void updateMessagesArea(List<Message> messages) {
        Platform.runLater(() -> {
            messagesArea.getChildren().clear();
            currentDisplayedDate = null;

            if (messages != null) {
                for (Message message : messages) {
                    displayMessage(message);
                }
            }
        });
    }

    public void displayMessage(Message message) {
        Platform.runLater(() -> {
            if (isCurrentChat(message.getChatId())) {
                LocalDateTime timestamp = message.getTimestamp();
                LocalDate messageDate = timestamp.toLocalDate();

                addDateLabelIfNecessary(messageDate);
                addMessageBox(message, timestamp);
            }
        });
    }

    private void addDateLabelIfNecessary(LocalDate messageDate) {
        if (currentDisplayedDate == null || !currentDisplayedDate.equals(messageDate)) {
            currentDisplayedDate = messageDate;
            String formattedDate = formatDate(messageDate);
            Label dateLabel = createDateLabel(formattedDate);
            HBox dateBox = createDateBox(dateLabel);
            messagesArea.getChildren().add(dateBox);
        }
    }

    private void addMessageBox(Message message, LocalDateTime timestamp) {
        VBox messageBox = new VBox();
        Label senderLabel = new Label(message.getFrom());
        senderLabel.getStyleClass().add("sender-label");

        // Create a text node for a message
        Text messageText = new Text(message.getContent());
        messageText.setWrappingWidth(300);  // Maximum text width
        messageText.getStyleClass().add("message-text");

        // Creating a TextFlow for a message
        TextFlow messageFlow = new TextFlow(messageText);
        messageFlow.setMaxWidth(300);
        messageFlow.getStyleClass().add("message-text-flow");

        Label timeLabel = new Label(timestamp.format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("time-label");

        // Message container
        HBox messageContainer = new HBox();
        messageContainer.setMaxWidth(300);

        StackPane textContainer = new StackPane(messageFlow);
        textContainer.setMaxWidth(300);

        messageContainer.getChildren().add(textContainer);

        if (message.getFrom().equals(usernameFirst)) {
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
    }

    private void addGeneralBox(VBox messageBox, boolean isLeft) {
        HBox generalBox = new HBox();
        if (isLeft) {
            generalBox.setAlignment(Pos.CENTER_LEFT);
        } else {
            generalBox.setAlignment(Pos.CENTER_RIGHT);
        }
        generalBox.getChildren().add(messageBox);
        messagesArea.getChildren().add(generalBox);
    }

    private Label createDateLabel(String formattedDate) {
        Label dateLabel = new Label(formattedDate);
        dateLabel.setAlignment(Pos.CENTER);
        dateLabel.getStyleClass().add("date-label");
        return dateLabel;
    }

    private HBox createDateBox(Label dateLabel) {
        HBox dateBox = new HBox();
        dateBox.setAlignment(Pos.CENTER);
        dateBox.getChildren().add(dateLabel);
        return dateBox;
    }

    private String formatDate(LocalDate date) {
        String day = String.valueOf(date.getDayOfMonth());
        String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return day + " - " + month;
    }

    public static synchronized ServerController getInstance(Server server) {
        if (instance == null) {
            instance = new ServerController(server);
        }
        return instance;
    }

    public void addChatToList(ChatDisplayData chatInfo) {
        Platform.runLater(() -> chatListView.getItems().add(chatInfo));
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public void updateChatList(List<ChatDisplayData> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            chatListView.getItems().addAll(chats);
        });
    }
    public void setChatList(List<ChatDisplayData> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            chatListView.getItems().addAll(chats);
        });
    }
    @Override
    public void onMessage(WebSocket conn, String input) throws JAXBException {
        if (input.contains("<userConnectionInfo>")) {
            handleUserConnectionInfo(conn, input);
        } else if (input.contains("<message>")) {
            handleMessage(conn, input);
        } else if (input.contains("<chatRequest>")) {
            handleChatRequest(conn, input);
        } else if (input.contains("<authRequest>")) {
            handleAuthRequest(conn, input);
        } else {
            conn.send("Unsupported input format");
        }
    }

    void handleAuthRequest(WebSocket conn, String input) {
        try {
            JAXBContext context = JAXBContext.newInstance(AuthRequest.class, AuthResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(input);
            AuthRequest authRequest = (AuthRequest) unmarshaller.unmarshal(reader);
            boolean authenticated = DatabaseManager.checkOrCreateUser(authRequest.getUsername(), authRequest.getPassword());
            AuthResponse authResponse = new AuthResponse(authenticated, authRequest.getUsername());
            String authResponseXml = XMLUtil.toXML(authResponse);
            conn.send(authResponseXml);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }


    private void handleChatRequest(WebSocket conn, String input) throws JAXBException {
        ChatRequest chatRequest = XMLUtil.fromXML(input, ChatRequest.class);

        switch (chatRequest.getAction()) {
            case "createChat":
                processChatCreationRequest(conn, chatRequest);
                break;
            case "updateChat":
                processChatUpdateRequest(conn, chatRequest);
                break;
            case "deleteChat":
                processChatDeletionRequest(conn, chatRequest);
                break;
            case "getChats":
                processGetChatsRequest(conn, chatRequest);
                break;
            case "getMessages":
                processGetMessagesRequest(conn, chatRequest);
                break;
            default:
                conn.send("Unsupported chat request action: " + chatRequest.getAction());
                break;
        }
    }

    private void processGetMessagesRequest(WebSocket conn, ChatRequest cR) {
        server.processGetMessagesRequest(conn, cR);
    }

    private void processGetChatsRequest(WebSocket conn, ChatRequest cR) {
        server.processGetChatsRequest(conn, cR);
    }

    private void processChatDeletionRequest(WebSocket conn, ChatRequest cR) {
        server.processChatDeletionRequest(conn, cR);
    }

    private void processChatUpdateRequest(WebSocket conn, ChatRequest cR) {
        server.processChatUpdateRequest(conn, cR);
    }

    private void processChatCreationRequest(WebSocket conn, ChatRequest cR) {
        server.processChatCreationRequest(conn, cR);
    }

    private void handleMessage(WebSocket conn, String input) {
        try {
            Message msg = XMLUtil.fromXML(input, Message.class);
            String logMessage = "Message from " + msg.getFrom() + " to " + msg.getTo() + ": " + msg.getContent();
            server.notifyObserversWithMessage(msg);
            server.sendDirectMessage(msg);
            server.recordMessageInDatabase(msg);
        } catch (JAXBException e) {
            System.err.println("Error parsing XML: " + e.getMessage());
            conn.send("Error processing your XML input");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            conn.send("An unexpected error occurred");
        }
    }

    public boolean isCurrentChat(int chatId) {
        return chatId == currentChatId;
    }

    private void handleUserConnectionInfo(WebSocket conn, String input) throws JAXBException {
        UserConnectionInfo info = XMLUtil.fromXML(input, UserConnectionInfo.class);
        server.updateDatabase(info.getUsername(), server.getPortConn(conn));
    }

    @Override
    public void displayLogMessage(String text, String styleClass) {
        Platform.runLater(() -> {
            Label logLabel = new Label(text);
            logLabel.getStyleClass().addAll("log-message", styleClass);
            logMessagesArea.getChildren().add(logLabel);
        });
    }
}
