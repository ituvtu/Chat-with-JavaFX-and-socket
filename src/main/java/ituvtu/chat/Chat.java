package ituvtu.chat;

import jakarta.xml.bind.annotation.*;

@XmlRootElement
public class Chat {
    private int chat_id;
    private String usernameFirst;
    private String usernameSecond;

    public Chat() {
        // JAXB requires a constructor with no arguments
    }

    public Chat(int chat_id, String usernameFirst, String usernameSecond) {
        this.chat_id = chat_id;
        this.usernameFirst = usernameFirst;
        this.usernameSecond = usernameSecond;
    }

    @XmlElement
    public int getChat_id() {
        return chat_id;
    }

    public void setChat_id(int chat_id) {
        this.chat_id = chat_id;
    }

    @XmlElement
    public String getUsernameFirst() {
        return usernameFirst;
    }

    public void setUsernameFirst(String usernameFirst) {
        this.usernameFirst = usernameFirst;
    }

    @XmlElement
    public String getUsernameSecond() {
        return usernameSecond;
    }
    public String getDisplayName(String currentUsername) {
        // Returns the name of the chat depending on the current user
        if (usernameFirst.equals(currentUsername)) {
            return usernameSecond;
        } else {
            return usernameFirst;
        }
    }
    public void setUsernameSecond(String usernameSecond) {
        this.usernameSecond = usernameSecond;
    }
    public String getChatDisplayName(String currentUser) {
        if (usernameFirst.equals(currentUser)) {
            return usernameSecond;
        } else {
            return usernameFirst;
        }
    }

    @Override
    public String toString() {
        return "Chat between " + usernameFirst + " and " + usernameSecond;
    }
}
