package ituvtu.chat;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;
import java.net.*;

public class ClientApp extends Application {
    private static ClientController controller;
private static Client client;  // Added a field to reference the client's WebSocket
    private static Stage primaryStage; // Save the main scene for later access
    private static String username;
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Start");
        ClientApp.primaryStage = primaryStage; // Remember the main scene
        showLoginScreen();
    }
    public void showLoginScreen() throws Exception {
        System.out.println("Login");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void showMainScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("Client.fxml"));
        Parent root = loader.load();

        // Passing the controller to a static variable
        ClientApp.controller = loader.getController();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Client: "+username);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void setUsername(String user) {
        System.out.println("username set");
        username = user;  // Method to set username
    }
    public static String getUsername() {
        return username;  // Method to get username
    }
    public static void main(String[] args) {
        System.out.println("method main");
        launch(args);
    }

    public static void connectToServer() throws URISyntaxException {
        if (controller != null) {
            Client client = new Client("ws://localhost:12345"); // URL сервера
            controller.setClient(client); // Встановлення клієнта
            client.connect();
        } else {
            System.out.println("Controller is not initialized yet.");
        }
    }
    @Override
    public void stop() {
        System.out.println("stop");
        if (client != null) {
            client.close();  // Closing the connection with the server
        }
    }
    public static ClientController getController() {
        System.out.println("get controller");
        return controller;
    }
}