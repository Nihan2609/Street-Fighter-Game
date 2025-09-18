package Client;

import db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginUIController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn, signupBtn, leaderboardBtn;

    @FXML
    private void initialize() {
        loginBtn.setOnAction(e -> handleLogin());
        signupBtn.setOnAction(e -> handleSignup());
        leaderboardBtn.setOnAction(e -> LeaderboardUI.display());
    }

    private void handleLogin() {
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
            ((Stage) loginBtn.getScene().getWindow()).close();
        } else {
            new Alert(Alert.AlertType.ERROR, "Invalid login!").showAndWait();
        }
    }

    private void handleSignup() {
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
    }

    private void showGameModeDialog(String username) {
        Stage dialog = new Stage();
        dialog.setTitle("Select Game Mode");

        Button playPcBtn = new Button("Play vs PC");
        Button playMultiBtn = new Button("Play Multiplayer");

        playPcBtn.setMaxWidth(Double.MAX_VALUE);
        playMultiBtn.setMaxWidth(Double.MAX_VALUE);

        playPcBtn.setOnAction(ev -> {
            dialog.close();
            SinglePlayerGame.username = username;
            SinglePlayerGame.openSinglePlayer(username);
        });

        playMultiBtn.setOnAction(ev -> {
            dialog.close();
            MultiplayerGame.openMultiplayer(username);
        });

        VBox dialogLayout = new VBox(15,
                new Label("Choose a game mode:"),
                playPcBtn,
                playMultiBtn
        );
        dialogLayout.setAlignment(javafx.geometry.Pos.CENTER);
        dialogLayout.setPadding(new javafx.geometry.Insets(20));

        dialog.setScene(new javafx.scene.Scene(dialogLayout, 250, 180));
        dialog.show();
    }
}
