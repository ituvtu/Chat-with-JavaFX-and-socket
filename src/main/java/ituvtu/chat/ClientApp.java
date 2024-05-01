package ituvtu.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    private Stage loginStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.loginStage = primaryStage;
        showLogin(primaryStage);
    }

    private void showLogin(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Parent root = loader.load();
        LoginController loginController = loader.getController();
        loginController.setOnLoginSuccess(this::onUserLoggedIn);

        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Login");
        primaryStage.show();
    }

    private void onUserLoggedIn(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Client.fxml"));
            Parent root = loader.load();

            ClientController clientController = loader.getController();
            clientController.setClientName(username);  // transfer username from Login to Client

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Client - " + username);
            stage.show();

            // Close the login window
            loginStage.close();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
