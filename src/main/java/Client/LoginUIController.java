package Client;

import db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

        addHoverEffect(loginBtn, "#2980b9", "#3498db");
        addHoverEffect(signupBtn, "#2980b9", "#3498db");

        AudioManager.playBGM("home.wav");
    }

    private void addHoverEffect(Button button, String hoverColor, String normalColor) {
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle()
                .replace(normalColor, hoverColor)));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle()
                .replace(hoverColor, normalColor)));
    }

    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please fill in both fields.").showAndWait();
            return;
        }
        if (DatabaseManager.loginPlayer(user, pass)) {
            try {

                AudioManager.playSelectSound();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/HomeUI.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) loginBtn.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Street Fighter");
                stage.show();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
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


}
