package ituvtu.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    public TextField passwordField;
    @FXML
    private TextField usernameField;

    public void handleLoginButton() throws Exception {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (!username.isEmpty() && !password.isEmpty()) {
            if (DatabaseManager.checkOrCreateUser(username, password)) {
                ClientApp.setUsername(username);
                ClientApp.showMainScreen();
            } else {
                System.out.println("Error logging in or creating account.");
            }
        }
    }
}