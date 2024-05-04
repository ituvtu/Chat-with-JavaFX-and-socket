package ituvtu.chat;

import jakarta.xml.bind.annotation.*;

@XmlRootElement
public class Message {
    private String from;
    private String to;
    private String content;

    public Message() {
       // JAXB requires a constructor with no arguments
    }
    public Message(String from, String to, String content) {
        this.from = from;
        this.to = to;
        this.content = content;
    }
    public String getFrom() {
        return from;
    }

    @XmlElement
    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    @XmlElement
    public void setTo(String to) {
        this.to = to;
    }

    public String getContent() {
        return content;
    }

    @XmlElement
    public void setContent(String content) {
        this.content = content;
    }
}
