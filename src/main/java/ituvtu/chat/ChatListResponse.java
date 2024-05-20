package ituvtu.chat;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlRootElement
public class ChatListResponse {
    private List<Chat> chats;

    public ChatListResponse() {
        // JAXB requires a constructor with no arguments
    }

    public ChatListResponse(List<Chat> chats) {
        this.chats = chats;
    }

    @XmlElement
    public List<Chat> getChats() {
        return chats;
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
    }
}