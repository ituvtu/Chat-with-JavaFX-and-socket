package ituvtu.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;


import java.util.List;

public class ServerController implements ServerObserver {

    public ListView<String> chatListView;
    @FXML
    private TextArea messagesArea;
    public void addChatToList(String chatInfo) {
        Platform.runLater(() -> chatListView.getItems().add(chatInfo));
    }

    public void setChatList(List<String> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            chatListView.getItems().addAll(chats);
        });
    }
    @Override
    public void updateChatList(List<String> chats) {
        Platform.runLater(() -> {
            chatListView.getItems().clear();
            chatListView.getItems().addAll(chats);
        });
    }
    @Override
    public void onMessage(String message) {
        System.out.println("Message received in ServerController: " + message);
        Platform.runLater(() -> {
            System.out.println("Updating messageArea with message: " + message);
            messagesArea.appendText(message + "\n");
        });
    }
}