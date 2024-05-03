module ituvtu.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.java_websocket;

    opens ituvtu.chat to javafx.fxml;
    exports ituvtu.chat;
}