module com.example.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens Client to javafx.graphics,javafx.fxml;
}