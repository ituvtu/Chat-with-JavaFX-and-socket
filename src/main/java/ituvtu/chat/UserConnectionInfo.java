package ituvtu.chat;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserConnectionInfo {
    private String username;
    private int port;

    public UserConnectionInfo() {
    }

    public UserConnectionInfo(String username, int port) {
        this.username = username;
        this.port = port;
    }

    @XmlElement
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @XmlElement
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}

