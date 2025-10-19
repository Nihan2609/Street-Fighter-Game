package Client;

import db.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginUIController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button signupBtn;

    private FontManager fontManager = FontManager.getInstance();

    // leaderboard track korar jonno
    public static String currentLoggedInUser = null;

    @FXML
    private void initialize() {
        fontManager.initialize();
        applyCustomFont();

        loginBtn.setOnAction(e -> handleLogin());
        signupBtn.setOnAction(e -> handleSignup());

        addHoverEffect(loginBtn, "#2980b9", "#3498db");
        addHoverEffect(signupBtn, "#2980b9", "#3498db");

        AudioManager.playBGM("home.wav");
    }

    private void applyCustomFont() {
        if (usernameField != null) {
            usernameField.setStyle(fontManager.getStyleString(11) +
                    "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 8; " +
                    "-fx-text-fill: black;");
        }

        if (passwordField != null) {
            passwordField.setStyle(fontManager.getStyleString(11) +
                    "-fx-background-color: rgba(255, 255, 255, 0.95); " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 8; " +
                    "-fx-text-fill: black;");
        }

        String buttonStyle = fontManager.getStyleString(12) +
                "-fx-background-color: #3498db; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10 20; " +
                "-fx-cursor: hand;";

        if (loginBtn != null) loginBtn.setStyle(buttonStyle);
        if (signupBtn != null) signupBtn.setStyle(buttonStyle);
    }

    private void addHoverEffect(Button button, String hoverColor, String normalColor) {
        String baseStyle = fontManager.getStyleString(12) +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10 20; " +
                "-fx-cursor: hand;";

        button.setOnMouseEntered(e ->
                button.setStyle(baseStyle + "-fx-background-color: " + hoverColor + ";"));
        button.setOnMouseExited(e ->
                button.setStyle(baseStyle + "-fx-background-color: " + normalColor + ";"));
    }

    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Warning", "Fill in both fields", Alert.AlertType.WARNING);
            return;
        }

        if (DatabaseManager.loginPlayer(user, pass)) {
            try {
                AudioManager.playSelectSound();

                currentLoggedInUser = user;

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/HomeUI.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) loginBtn.getScene().getWindow();
                stage.setScene(new Scene(root, 600, 400));
                stage.setTitle("Street Fighter - Welcome " + user);
                stage.show();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            showAlert("Error", "Invalid credentials!", Alert.AlertType.ERROR);
        }
    }

    private void handleSignup() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Warning", "Fill in both fields", Alert.AlertType.WARNING);
            return;
        }

        if (DatabaseManager.registerPlayer(user, pass)) {
            showAlert("Success", "Signup successful!", Alert.AlertType.INFORMATION);
            usernameField.clear();
            passwordField.clear();
        } else {
            showAlert("Error", "Username exists!", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(fontManager.getStyleString(10) +
                "-fx-background-color: white;");

        alert.showAndWait();
    }
}