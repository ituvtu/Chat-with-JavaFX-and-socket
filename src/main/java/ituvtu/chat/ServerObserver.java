package ituvtu.chat;

import jakarta.xml.bind.JAXBException;
import org.java_websocket.WebSocket;

import java.util.List;

public interface ServerObserver {
    void onMessage(WebSocket conn, String input) throws JAXBException;
    void updateChatList(List<ChatDisplayData> chats);
    void displayMessage(String message);
    void displayLogMessage(String message);
}

