package Client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

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
    private int currentIndex = 0;
    private ImageView[] maps;

    // Network components
    private NetworkClient networkClient = null;
    private String localPlayerId = null;
    private boolean isNetworkMode = false;
    private boolean mapConfirmed = false;

    @FXML
    private void initialize() {
        maps = new ImageView[]{map1, map2, map3, map4, map5, map6};
        highlightMap(maps[currentIndex]);

        titleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> handleKeyPress(event.getCode()));
            }
        });
    }

    public void setNetworkMode(NetworkClient client, String playerId) {
        this.networkClient = client;
        this.localPlayerId = playerId;
        this.isNetworkMode = true;

        // P2 just waits - update UI to show this
        if (playerId.equals("P2")) {
            titleLabel.setText("Host is selecting map...");
        }

        if (networkClient != null) {
            networkClient.setCallback(new NetworkClient.NetworkCallback() {
                @Override
                public void onConnected() {}

                @Override
                public void onDisconnected() {}

                @Override
                public void onGameStart() {}

                @Override
                public void onInputReceived(String pid, long frameNumber, short inputBits) {
                    handleNetworkMapSelection(inputBits);
                }

                @Override
                public void onPlayerDisconnected(String pid) {}
            });
        }
    }

    private void handleKeyPress(KeyCode code) {
        // Only P1 (host) can select in network mode
        if (isNetworkMode && localPlayerId.equals("P2")) {
            return; // P2 does nothing
        }

        if (code == KeyCode.A) {
            moveLeft();
            if (isNetworkMode) sendMapSelection(false);
        } else if (code == KeyCode.D) {
            moveRight();
            if (isNetworkMode) sendMapSelection(false);
        } else if (code == KeyCode.SPACE) {
            AudioManager.playSelectSound();
            if (isNetworkMode) {
                sendMapSelection(true); // Send confirmed selection
            }
            selectMap(maps[currentIndex]);
        }
    }

    private void sendMapSelection(boolean confirmed) {
        if (networkClient != null && networkClient.isConnected()) {
            short mapData = (short) ((currentIndex & 0x0F) | (confirmed ? 1 : 0) << 4);
            networkClient.sendInput(mapData);
        }
    }

    private void handleNetworkMapSelection(short mapData) {
        Platform.runLater(() -> {
            int receivedIndex = mapData & 0x0F;
            boolean confirmed = ((mapData >> 4) & 0x01) == 1;

            System.out.println("P2 received: index=" + receivedIndex + ", confirmed=" + confirmed);

            // Bounds check
            if (receivedIndex < 0 || receivedIndex >= maps.length) {
                System.out.println("Invalid index, ignoring");
                return;
            }

            // Update display
            if (receivedIndex != currentIndex) {
                currentIndex = receivedIndex;
                highlightMap(maps[currentIndex]);
                System.out.println("Updated to map " + receivedIndex);
            }

            // If confirmed, proceed
            if (confirmed && !mapConfirmed) {
                System.out.println("Map confirmed! Proceeding to fight...");
                mapConfirmed = true;
                selectMap(maps[currentIndex]);
            }
        });
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

        System.out.println("selectMap called, mapConfirmed=" + mapConfirmed);

        if (mapConfirmed) {
            System.out.println("Already confirmed, returning");
            return;
        }

        mapConfirmed = true;

        if (mapConfirmed) return;
        mapConfirmed = true;

        resetMapStyles();
        highlightMap(selectedImageView);

        String mapFile = getMapFile(selectedImageView);
        this.selectedMap = mapFile;

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

            // Pass network mode
            if (isNetworkMode && networkClient != null) {
                gameController.setNetworkMode(networkClient, localPlayerId);
            }

            Scene gameScene = new Scene(root, 800, 400);
            Stage stage = (Stage) map1.getScene().getWindow();
            stage.setScene(gameScene);
            stage.setTitle("Street Fighter - " + (isNetworkMode ? "Network Match" : "Local"));
            stage.setResizable(false);
            stage.sizeToScene();
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}