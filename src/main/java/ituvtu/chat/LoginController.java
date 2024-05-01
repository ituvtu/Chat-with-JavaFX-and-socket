package ituvtu.chat;

import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.util.function.Consumer;
import javafx.scene.control.*;
public class LoginController {

    @FXML
    private TextField usernameField;

    private Consumer<String> onLoginSuccess;

    public void setOnLoginSuccess(Consumer<String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    @FXML
    private void handleLoginButton() {
        String username = usernameField.getText().trim();
        if (!username.isEmpty() && onLoginSuccess != null) {
            onLoginSuccess.accept(username);
            closeStage();
        } else {
            System.out.println("Username cannot be empty.");
        }
    }

    private void closeStage() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}
