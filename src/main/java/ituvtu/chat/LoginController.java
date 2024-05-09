package ituvtu.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LoginController {

    public TextField passwordField;
    @FXML
    private TextField usernameField;

    public void handleLoginButton() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (!username.isEmpty() && !password.isEmpty()) {
            if (DatabaseManager.checkOrCreateUser(username, password)) {
                ClientApp.setUsername(username);
                try {
                    ClientApp.showMainScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Error logging in or creating account.");
            }
        }
    }
}