package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Objects;

public class HomeUIController {

    public ImageView bgImage;
    @FXML private Button multiplayerBtn;
    @FXML private Button vsBotBtn;
    @FXML private Button leaderboardBtn;
    @FXML private Button logoutBtn;

    @FXML
    public void initialize() {
        leaderboardBtn.setOnAction(e -> openLeaderboard());
        logoutBtn.setOnAction(e -> logout());

        addHoverEffect(multiplayerBtn, "#2980b9", "#3498db");
        addHoverEffect(vsBotBtn, "#2980b9", "#3498db");
        addHoverEffect(leaderboardBtn, "#2980b9", "#3498db");
        addHoverEffect(logoutBtn, "#c0392b", "#e74c3c");
    }

    private void addHoverEffect(Button button, String hoverColor, String normalColor) {
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle()
                .replace(normalColor, hoverColor)));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle()
                .replace(hoverColor, normalColor)));
    }

    private void openMultiplayer() {
        System.out.println("Multiplayer selected");
        // TODO: Load Multiplayer lobby screen
    }

    private void openVsBot() {
        System.out.println("Vs Bot selected");
        // TODO: Load game scene with AI logic
    }

    @FXML
    private void openLeaderboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/Leaderboard.fxml"));
            AnchorPane root = loader.load();

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
