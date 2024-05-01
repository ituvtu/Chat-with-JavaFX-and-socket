module ituvtu.chat {
    requires javafx.controls;
    requires javafx.fxml;

    opens ituvtu.chat to javafx.fxml;
    exports ituvtu.chat;
}