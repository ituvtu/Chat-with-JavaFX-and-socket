package ituvtu.chat;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement
public class MessagesResponse {
    private List<Message> messages;

    // JAXB вимагає конструктора без аргументів
    public MessagesResponse() {
    }

    public MessagesResponse(List<Message> messages) {
        this.messages = messages;
    }

    @XmlElement
    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
