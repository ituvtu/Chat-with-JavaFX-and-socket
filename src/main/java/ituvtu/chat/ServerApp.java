package ituvtu.chat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class ServerApp extends Application {
    private static Server server;

    public static void initializeServer() throws Exception {
        if (server == null) {
            InputStream is = ServerApp.class.getClassLoader().getResourceAsStream("srv.properties");
            Properties props = new Properties();
            props.load(is);
            int port = Integer.parseInt(props.getProperty("srv.port"));
            server = Server.getInstance(port);
            server.start();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Server.fxml"));
        loader.setControllerFactory(c -> ServerController.getInstance(server));
        Parent root = loader.load();
        ServerController controller = loader.getController();

        initializeServer();
        controller.setServer(server);
        server.addObserver(controller);


        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("server-styles.css")).toExternalForm());
        primaryStage.setTitle("Server");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Server failed to stop cleanly: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
