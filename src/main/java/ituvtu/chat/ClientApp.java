package ituvtu.chat;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.Stage;
import java.net.*;

public class ClientApp extends Application {
    static ClientController controller;
    static Client client;  // Added a field to reference the client's WebSocket
    private static Stage primaryStage; // Save the main scene for later access
    static String username;



    @Override
    public void start(Stage primaryStage) throws Exception {
        ClientApp.primaryStage = primaryStage;
        showLoginScreen();
    }

    public static void initializeClient() throws URISyntaxException {
        if (client == null || !client.isOpen()) {
        client = new Client("ws://localhost:12345");
        client.connect();}
    }

    public void showLoginScreen() throws Exception {
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
        controller = loader.getController();

        initializeClient();

        controller.setClient(client);
        client.addObserver(controller);

        Scene scene = new Scene(root);
        primaryStage.setTitle("Client of " + username);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void setUsername(String user) {
        username = user;
    }

    public static String getUsername() {
        return username;
    }

    public static ClientController getController() {
        return controller;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Application is stopping.");
        if (client != null && client.isOpen()) {
            client.close();
        }
        super.stop();
    }
}