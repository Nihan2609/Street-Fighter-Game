package Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;

import java.io.IOException;

public class MapSelectController {

    private String player1Choice;
    private String player2Choice;

    @FXML private ImageView map1;
    @FXML private ImageView map2;
    @FXML private ImageView map3;
    @FXML private ImageView map4;
    @FXML private ImageView map5;
    @FXML private ImageView map6;
    @FXML private Label titleLabel;

    private String selectedMap = null;
    private int currentIndex = 0; // tracks highlighted map
    private ImageView[] maps;

    @FXML
    private void initialize() {
        maps = new ImageView[]{map1, map2, map3, map4, map5, map6};

        // Start by highlighting the first map
        highlightMap(maps[currentIndex]);

        // Add key handling AFTER scene is ready
        titleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.A) {
                        moveLeft();
                    } else if (event.getCode() == KeyCode.D) {
                        moveRight();
                    } else if (event.getCode() == KeyCode.SPACE) {
                        selectMap(maps[currentIndex]);
                    }
                });
            }
        });
    }

    // Highlight map with hover-like effect
    private void highlightMap(ImageView map) {
        resetMapStyles();
        map.setStyle("-fx-effect: dropshadow(gaussian, yellow, 10, 0.5, 0, 0); -fx-cursor: hand;");
        map.setScaleX(1.1);
        map.setScaleY(1.1);
    }

    private void resetMapStyles() {
        for (ImageView map : maps) {
            map.setStyle("-fx-effect: null;");
            map.setScaleX(1.0);
            map.setScaleY(1.0);
        }
    }

    private void moveLeft() {
        currentIndex = (currentIndex - 1 + maps.length) % maps.length;
        highlightMap(maps[currentIndex]);
    }

    private void moveRight() {
        currentIndex = (currentIndex + 1) % maps.length;
        highlightMap(maps[currentIndex]);
    }

    private void selectMap(ImageView selectedImageView) {
        resetMapStyles();
        highlightMap(selectedImageView);

        String mapChoice = getMapName(selectedImageView);
        String mapFile = getMapFile(selectedImageView);

        this.selectedMap = mapFile;

        startFightScene(mapChoice, mapFile);
    }

    // Pass player choices
    public void setPlayerChoices(String p1, String p2) {
        this.player1Choice = p1;
        this.player2Choice = p2;
    }

    private String getMapName(ImageView imageView) {
        if (imageView == map1) return "Forest";
        if (imageView == map2) return "Apocalypse";
        if (imageView == map3) return "Autumn";
        if (imageView == map4) return "Moon Blessed";
        if (imageView == map5) return "Revenge";
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
        return "map1";
    }

    private void startFightScene(String mapName, String mapFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/GameScene.fxml"));
            Parent root = loader.load();

            GameSceneController gameController = loader.getController();
            gameController.setGameData(player1Choice, player2Choice, mapFile);

            Scene gameScene = new Scene(root, 800, 400);
            Stage stage = (Stage) map1.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("Street Fighter");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            System.err.println("Error loading fight scene:");
            e.printStackTrace();
        }
    }
}
