package Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class MapSelectController {

    // Player choices from ChampSelectController
    private String player1Choice;
    private String player2Choice;

    @FXML private ImageView map1;
    @FXML private ImageView map2;
    @FXML private ImageView map3;
    @FXML private ImageView map4;
    @FXML private ImageView map5;
    @FXML private ImageView map6;
    @FXML private Label titleLabel;
    @FXML private Label instructionsLabel;

    private String selectedMap = null;

    @FXML
    private void initialize() {
        // Set up click handlers for all maps
        map1.setOnMouseClicked(this::handleMapSelection);
        map2.setOnMouseClicked(this::handleMapSelection);
        map3.setOnMouseClicked(this::handleMapSelection);
        map4.setOnMouseClicked(this::handleMapSelection);
        map5.setOnMouseClicked(this::handleMapSelection);
        map6.setOnMouseClicked(this::handleMapSelection);

        // Add hover effects
        addHoverEffects();
    }

    private void addHoverEffects() {
        ImageView[] maps = {map1, map2, map3, map4, map5, map6};

        for (ImageView map : maps) {
            map.setOnMouseEntered(e -> {
                map.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.5, 0, 0); -fx-cursor: hand;");
                map.setScaleX(1.1);
                map.setScaleY(1.1);
            });

            map.setOnMouseExited(e -> {
                if (!map.equals(getSelectedImageView())) {
                    map.setStyle("-fx-effect: null;");
                    map.setScaleX(1.0);
                    map.setScaleY(1.0);
                }
            });
        }
    }

    // Method called by ChampSelectController to set player choices
    public void setPlayerChoices(String p1, String p2) {
        this.player1Choice = p1;
        this.player2Choice = p2;
        System.out.println("Received player choices: " + p1 + " vs " + p2);
    }

    @FXML
    private void handleMapSelection(MouseEvent event) {
        // Reset all map styles first
        resetMapStyles();

        // Determine which map was selected
        ImageView selectedImageView = (ImageView) event.getSource();
        String mapChoice = getMapName(selectedImageView);
        String mapFile = getMapFile(selectedImageView);

        // Highlight selected map
        selectedImageView.setStyle("-fx-effect: dropshadow(gaussian, lime, 15, 0.8, 0, 0);");
        selectedImageView.setScaleX(1.15);
        selectedImageView.setScaleY(1.15);

        this.selectedMap = mapFile;

        // Update UI
        instructionsLabel.setText("Selected: " + mapChoice + " - Click again to start fight!");
        instructionsLabel.setStyle("-fx-text-fill: lime;");

        System.out.println("✅ P1 selected: " + player1Choice);
        System.out.println("✅ P2 selected: " + player2Choice);
        System.out.println("✅ Map selected: " + mapChoice + " (" + mapFile + ")");

        // Start the game immediately (or you can require double-click)
        startFightScene(mapChoice, mapFile);
    }

    private void resetMapStyles() {
        ImageView[] maps = {map1, map2, map3, map4, map5, map6};
        for (ImageView map : maps) {
            map.setStyle("-fx-effect: null;");
            map.setScaleX(1.0);
            map.setScaleY(1.0);
        }
    }

    private String getMapName(ImageView imageView) {
        if (imageView == map1) return "Dojo";
        if (imageView == map2) return "Street";
        if (imageView == map3) return "Beach";
        if (imageView == map4) return "Mountain";
        if (imageView == map5) return "Factory";
        if (imageView == map6) return "Temple";
        return "Unknown";
    }

    private String getMapFile(ImageView imageView) {
        if (imageView == map1) return "map1";
        if (imageView == map2) return "map2";
        if (imageView == map3) return "map3";
        if (imageView == map4) return "map4";
        if (imageView == map5) return "map5";
        if (imageView == map6) return "map6";
        return "map1"; // default
    }

    private ImageView getSelectedImageView() {
        if ("map1".equals(selectedMap)) return map1;
        if ("map2".equals(selectedMap)) return map2;
        if ("map3".equals(selectedMap)) return map3;
        if ("map4".equals(selectedMap)) return map4;
        if ("map5".equals(selectedMap)) return map5;
        if ("map6".equals(selectedMap)) return map6;
        return null;
    }

    private void startFightScene(String mapName, String mapFile) {
        try {
            // Load the GameScene.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/GameScene.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the game data
            GameSceneController gameController = loader.getController();

            // Pass the selected characters and map to the fight scene
            gameController.setGameData(player1Choice, player2Choice, mapFile);

            // Create the scene
            Scene gameScene = new Scene(root, 800, 600);

            // Set up key event handling for the fight scene
            gameScene.setOnKeyPressed(event -> {
                // Forward key events to the game canvas
                root.lookup("#gameCanvas").fireEvent(event);
            });
            gameScene.setOnKeyReleased(event -> {
                // Forward key events to the game canvas
                root.lookup("#gameCanvas").fireEvent(event);
            });

            // Get current stage and set the new scene
            Stage stage = (Stage) map1.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("Street Fighter - " + mapName + " (" + player1Choice + " vs " + player2Choice + ")");
            stage.show();

            // Important: Request focus on the canvas for input handling
            javafx.application.Platform.runLater(() -> {
                root.lookup("#gameCanvas").requestFocus();
            });

            System.out.println("🚀 Fight scene launched!");

        } catch (IOException e) {
            System.err.println("Error loading fight scene:");
            e.printStackTrace();

            // Fallback: show error message
            instructionsLabel.setText("Error loading fight scene! Check console for details.");
            instructionsLabel.setStyle("-fx-text-fill: red;");
        }
    }
}