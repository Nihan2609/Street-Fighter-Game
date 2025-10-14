package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Objects;

public class HomeUIController {

    public ImageView bgImage;
    @FXML private Button multiplayerBtn;
    @FXML private Button networkMultiplayerBtn;
    @FXML private Button leaderboardBtn;
    @FXML private Button logoutBtn;

    private FontManager fontManager = FontManager.getInstance();

    @FXML
    public void initialize() {
        fontManager.initialize();

        leaderboardBtn.setOnAction(e -> openLeaderboard());
        logoutBtn.setOnAction(e -> logout());
        multiplayerBtn.setOnAction(e -> openMultiplayer());
        networkMultiplayerBtn.setOnAction(e -> openNetworkMultiplayer());

        // Apply custom font to buttons
        applyCustomFont(multiplayerBtn, "#2980b9", "#3498db");
        applyCustomFont(networkMultiplayerBtn, "#8e44ad", "#9b59b6");
        applyCustomFont(leaderboardBtn, "#2980b9", "#3498db");
        applyCustomFont(logoutBtn, "#c0392b", "#e74c3c");
    }

    private void applyCustomFont(Button button, String hoverColor, String normalColor) {
        String baseStyle = fontManager.getStyleString(11) +
                "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;";

        button.setStyle(baseStyle + "-fx-background-color: " + normalColor + "; -fx-text-fill: white;");

        button.setOnMouseEntered(e ->
                button.setStyle(baseStyle + "-fx-background-color: " + hoverColor + "; -fx-text-fill: white;"));

        button.setOnMouseExited(e ->
                button.setStyle(baseStyle + "-fx-background-color: " + normalColor + "; -fx-text-fill: white;"));
    }

    @FXML
    private void openMultiplayer() {
        try {
            AudioManager.playSelectSound();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/ChampSelect.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) multiplayerBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Character Select");
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void openNetworkMultiplayer() {
        try {
            AudioManager.playSelectSound();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/NetworkLobby.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) networkMultiplayerBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Network Multiplayer");
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void openLeaderboard() {
        try {
            AudioManager.playSelectSound();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/Leaderboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) leaderboardBtn.getScene().getWindow();
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            stage.setTitle("Leaderboard");
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void logout() {
        try {
            AudioManager.playSelectSound();
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/game/LoginUI.fxml")));
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Street Fighter");
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}