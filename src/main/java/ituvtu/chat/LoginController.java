package ituvtu.chat;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    public void handleLoginButton() {
        String username = usernameField.getText().trim();
        if (!username.isEmpty()) {
            ClientApp.setUsername(username); // Save the username
            System.out.println("Logged in as: " + username);

            try {
                ClientApp.showMainScreen(); // Go to the main window
                ClientApp.connectToServer(); // Now we connect to the server
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }
}