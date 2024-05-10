package ituvtu.chat;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.java_websocket.WebSocket;
import java.io.*;
import java.util.*;

public class ServerController implements ServerObserver {
    private static ServerController instance;
    public ListView<ChatDisplayData> chatListView;
    public TextArea messagesArea;
    private Server server;
    @FXML
    private TextArea logMessagesArea;

    private ServerController(Server server) {
        this.server = server;
    }
    @FXML
    public void initialize() {
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                try {
                    loadMessagesForChat(newSelection.getChatId());
                } catch (JAXBException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    private void loadMessagesForChat(int chatId) throws JAXBException {
        messagesArea.clear();
        processMessagesResponse(server.processServerGetMessagesRequest(chatId));
    }
    private void processMessagesResponse(String xmlMessage) {
        //noinspection DuplicatedCode
        try {
            JAXBContext context = JAXBContext.newInstance(MessagesResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StringReader reader = new StringReader(xmlMessage);
            MessagesResponse response = (MessagesResponse) unmarshaller.unmarshal(reader);
            updateMessagesArea(response.getMessages());
        } catch (Exception e) {
            displayMessage("Error parsing messages: " + e.getMessage());
        }
    }
    private void updateMessagesArea(List<Message> messages) {
        Platform.runLater(() -> {
            messagesArea.clear();
            if (messages != null) {  // Додано перевірку на null
                for (Message message : messages) {
                    String displayText = message.getFrom() + ": " + message.getContent();
                    displayMessage(displayText);
                }
            }

        });
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

    public void setChatList(List<ChatDisplayData> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            chatListView.getItems().addAll(chats);
        });
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
    @Override
    public void onMessage(WebSocket conn, String input) throws JAXBException {
        if (input.contains("<userConnectionInfo>")) {
            handleUserConnectionInfo(conn, input);
        } else if (input.contains("<message>")) {
            handleMessage(conn, input);
        } else if (input.contains("<chatRequest>")) {
            handleChatRequest(conn, input);
        }
        else {
            System.out.println("Input is not supported format");
            conn.send("Unsupported input format");
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
            System.out.println(logMessage);
            server.notifyObserversWithMessage(logMessage);
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

    private void handleUserConnectionInfo(WebSocket conn, String input) throws JAXBException {

        UserConnectionInfo info = XMLUtil.fromXML(input, UserConnectionInfo.class);
        server.updateDatabase(info.getUsername(), server.getPortConn(conn));
    }

    public void displayMessage(String text) {
        Platform.runLater(() -> messagesArea.appendText(text + "\n"));
    }
    public void displayLogMessage(String text){
        Platform.runLater(() -> logMessagesArea.appendText(text + "\n"));
    }
}