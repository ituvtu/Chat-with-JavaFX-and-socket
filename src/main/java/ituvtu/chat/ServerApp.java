package ituvtu.chat;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class ServerApp extends Application {
    private Server server;
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Server.fxml"));
        Parent root = loader.load();
        ServerController controller = loader.getController();
        Scene scene = new Scene(root);

        primaryStage.setTitle("Server");
        primaryStage.setScene(scene);
        primaryStage.show();
        server = new Server(12345);
        server.addObserver(controller);
        server.start();

    }
    @Override
    public void stop() {
        if (server != null) {
            try {
                server.stop();  // Правильний виклик для зупинки сервера
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();  // Обробка переривання потоку
                System.err.println("Server failed to stop cleanly: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}