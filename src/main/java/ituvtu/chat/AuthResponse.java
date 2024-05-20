package ituvtu.chat;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AuthResponse {
    private boolean authenticated;
    private String username; // Add this field

    public AuthResponse() {
    }

    public AuthResponse(boolean authenticated, String username) {
        this.authenticated = authenticated;
        this.username = username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

