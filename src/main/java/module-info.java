module ituvtu.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.java_websocket;
    requires jakarta.xml.bind;
    requires java.sql;
    requires java.desktop;
    opens ituvtu.chat to jakarta.xml.bind, javafx.fxml;
    exports ituvtu.chat;
}