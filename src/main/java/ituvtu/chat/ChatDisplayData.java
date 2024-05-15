package ituvtu.chat;

public record ChatDisplayData(int chatId, String displayName, String usernameFirst, String usernameSecond) {
    @Override
    public String toString() {
        return displayName;
    }
}
