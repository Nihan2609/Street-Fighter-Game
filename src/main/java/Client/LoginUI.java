package Client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LoginUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/LoginUI.fxml"));
        Scene scene = new Scene(loader.load(), 400, 500);

        primaryStage.setTitle("Street Fighter Login");
        Image logo = new Image(getClass().getResourceAsStream("/images/logo.jpg"));
        primaryStage.getIcons().add(logo);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
