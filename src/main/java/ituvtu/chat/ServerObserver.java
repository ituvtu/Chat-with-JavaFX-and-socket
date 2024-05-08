package ituvtu.chat;

import java.util.List;

public interface ServerObserver {
    void onMessage(String message);
    void updateChatList(List<String> chats);
}

