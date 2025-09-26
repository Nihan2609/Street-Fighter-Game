package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/LoginUI.fxml"));
        Scene scene = new Scene(loader.load(), 600, 400);
        Font font = Font.loadFont(getClass().getResourceAsStream("/fonts/PressStart2P-Regular.ttf"), 16);

        primaryStage.setTitle("Street Fighter");
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.jpg")));
        primaryStage.getIcons().add(logo);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
