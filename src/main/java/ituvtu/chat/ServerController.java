package ituvtu.chat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import ituvtu.chat.ServerApp;
import java.util.EventListener;

public class ServerController implements ServerObserver {
    @FXML
    private TextArea messagesArea;

    @Override
    public void onMessage(String message) {
        System.out.println("Message received in ServerController: " + message);
        Platform.runLater(() -> {
            System.out.println("Updating messageArea with message: " + message);
            messagesArea.appendText(message + "\n");
        });
    }



}
