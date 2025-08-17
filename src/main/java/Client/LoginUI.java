package Client;

import db.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class LoginUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Username & Password
        Label userLabel = new Label("Username:");
        userLabel.setTextFill(Color.WHITE); // White text
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        Label passLabel = new Label("Password:");
        passLabel.setTextFill(Color.WHITE); // White text
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        // Buttons
        Button loginBtn = new Button("Login");
        Button signupBtn = new Button("Sign Up");
        Button leaderboardBtn = new Button("View Leaderboard");

        loginBtn.setMaxWidth(Double.MAX_VALUE);
        signupBtn.setMaxWidth(Double.MAX_VALUE);
        leaderboardBtn.setMaxWidth(Double.MAX_VALUE);

        // Layout inside transparent panel
        VBox formBox = new VBox(12,
                userLabel, usernameField,
                passLabel, passwordField,
                loginBtn, signupBtn, leaderboardBtn
        );
        formBox.setAlignment(Pos.CENTER);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 12;");

        // Background Image
        BackgroundImage bg = new BackgroundImage(
                new Image(getClass().getResource("/images/LogBack.jpeg").toExternalForm()),
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(400, 500, true, true, true, false)
        );

        StackPane root = new StackPane();
        root.setBackground(new Background(bg));
        root.getChildren().add(formBox);

        Scene scene = new Scene(root, 400, 500);

        primaryStage.setTitle("Street Fighter Login");
        primaryStage.setScene(scene);
        primaryStage.show();

        // --- Login Handler ---
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
                return;
            }
            if (DatabaseManager.loginPlayer(user, pass)) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Login Success!");
                a.showAndWait();
                showGameModeDialog(user);
                primaryStage.close();
            } else {
                new Alert(Alert.AlertType.ERROR, "Invalid login!").showAndWait();
            }
        });

        // --- Signup Handler ---
        signupBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText().trim();
            if (user.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
                return;
            }
            if (DatabaseManager.registerPlayer(user, pass)) {
                new Alert(Alert.AlertType.INFORMATION, "Signup Success! You can now login.").showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Username already exists!").showAndWait();
            }
        });

        // --- Leaderboard Button ---
        leaderboardBtn.setOnAction(e -> LeaderboardUI.display());
    }

    private void showGameModeDialog(String username) {
        Stage dialog = new Stage();
        dialog.setTitle("Select Game Mode");

        Button playPcBtn = new Button("Play vs PC");
        Button playMultiBtn = new Button("Play Multiplayer");

        playPcBtn.setMaxWidth(Double.MAX_VALUE);
        playMultiBtn.setMaxWidth(Double.MAX_VALUE);

        VBox dialogLayout = new VBox(15,
                new Label("Choose a game mode:"),
                playPcBtn,
                playMultiBtn
        );
        dialogLayout.setAlignment(Pos.CENTER);
        dialogLayout.setPadding(new Insets(20));

        playPcBtn.setOnAction(ev -> {
            dialog.close();
            SinglePlayerGame.username = username;
            SinglePlayerGame.openSinglePlayer(username);
        });

        playMultiBtn.setOnAction(ev -> {
            dialog.close();
            MultiplayerGame.openMultiplayer(username);
        });

        Scene scene = new Scene(dialogLayout, 250, 180);
        dialog.setScene(scene);
        dialog.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
