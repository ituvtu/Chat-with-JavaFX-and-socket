package ituvtu.chat;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDateTime;

@XmlRootElement
public class Message {
    private String from;
    private String to;
    private String content;
    private LocalDateTime timestamp;
    private int chatId;

    // Конструктор
    public Message() {}

    public Message(String from, String to, String content, int chatId) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.chatId = chatId;
    }

    // Геттери і сеттери
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }
}
