module com.example.game {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires javafx.media;


    opens Client to javafx.graphics,javafx.fxml,javafx.base;
    opens db to javafx.base, javafx.fxml, javafx.graphics;
}