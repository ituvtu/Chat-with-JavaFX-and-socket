package ituvtu.chat;

public class ChatDisplayData {
    private final int chatId;
    private final String displayName;

    public ChatDisplayData(int chatId, String displayName) {
        this.chatId = chatId;
        this.displayName = displayName;
    }

    public int getChatId() {
        return chatId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        // ListView uses toString() method to display items
        return displayName;
    }
}

