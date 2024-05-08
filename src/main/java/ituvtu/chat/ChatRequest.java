package ituvtu.chat;

import jakarta.xml.bind.annotation.*;

import java.util.Map;

@XmlRootElement
public class ChatRequest {
    private String action;
    private String username1;
    private String username2;
    private int chatId;
    private Map<String, String> parameters; // Options to update

    public ChatRequest(){
        // JAXB requires a constructor with no arguments
    }
    public ChatRequest(String action, int chatId, String username2){
        this.action = action;
        this.chatId = chatId;
        this.username1 = null;
        this.username2 = username2;

    }
    public ChatRequest(String action, String username1, String username2){
        this.action = action;
        this.username1 = username1;
        this.username2 = username2;

    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUsername1() {
        return username1;
    }

    public void setUsername1(String username1) {
        this.username1 = username1;
    }

    public String getUsername2() {
        return username2;
    }

    public void setUsername2(String username2) {
        this.username2 = username2;
    }

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
