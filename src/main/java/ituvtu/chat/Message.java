package ituvtu.chat;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
@XmlRootElement
public class Message {
    private String from;
    private String content;

    public Message() {
        // JAXB потрібен конструктор без аргументів
    }

    public Message(String from, String content) {
        this.from = from;
        this.content = content;
    }

    @XmlElement
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @XmlElement
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

