module ituvtu.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.java_websocket;
    //    requires java.xml.bind;
    requires jakarta.xml.bind;
    requires java.sql;
    opens ituvtu.chat to jakarta.xml.bind, javafx.fxml;
    exports ituvtu.chat;
}