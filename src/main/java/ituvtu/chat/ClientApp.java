package ituvtu.chat;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private static ClientController controller;
    private Client client;  // Додано поле для посилання на WebSocket клієнта

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("client.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void connectToServer() {
        try {
            client = new Client("ws://localhost:12345");  // Створення нового об'єкта клієнта
            client.connect();
            controller.setClient(client);  // Надсилання посилання на клієнта до контролера
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (client != null) {
            client.close();  // Закриття з'єднання з сервером
        }
    }

    public static ClientController getController() {
        return controller;
    }
}

