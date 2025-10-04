package Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

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

    private int currentIndex = 0;
    private ImageView[] maps;
    private NetworkClient networkClient = null;

    @FXML
    private void initialize() {
        maps = new ImageView[]{map1, map2, map3, map4, map5, map6};
        highlightMap(maps[currentIndex]);

        titleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.A) {
                        moveLeft();
                    } else if (event.getCode() == KeyCode.D) {
                        moveRight();
                    } else if (event.getCode() == KeyCode.SPACE) {
                        AudioManager.playSelectSound();
                        selectMap(maps[currentIndex]);
                    }
                });
            }
        });
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
    }

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
        String mapFile = getMapFile(selectedImageView);

        // If network mode, send config to server
        if (networkClient != null) {
            networkClient.sendGameConfig(player1Choice, player2Choice, mapFile);
        }

        startFightScene(mapFile);
    }

    public void setPlayerChoices(String p1, String p2) {
        this.player1Choice = p1;
        this.player2Choice = p2;
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

    private void startFightScene(String mapFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/GameScene.fxml"));
            Parent root = loader.load();

            GameSceneController gameController = loader.getController();
            gameController.setGameData(player1Choice, player2Choice, mapFile);

            if (networkClient != null) {
                gameController.setNetworkMode(networkClient, "P1");
            }

            Scene gameScene = new Scene(root, 800, 400);
            Stage stage = (Stage) map1.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("Street Fighter - Network Match");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}