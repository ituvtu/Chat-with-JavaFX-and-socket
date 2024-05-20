package ituvtu.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    public TextField passwordField;
    @FXML
    private TextField usernameField;

    private ClientController controller;
    public void handleLoginButton() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (!username.isEmpty() && !password.isEmpty()&&controller!= null) {
            controller.sendAuthenticationInfo(username,password);
        }}
    public void setController(ClientController controller) {
        this.controller = controller;
    }
}


