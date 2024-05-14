package ituvtu.chat;

public record ChatDisplayData(int chatId, String displayName) {

    @Override
    public String toString() {
        // ListView uses toString() method to display items
        return displayName;
    }
}

