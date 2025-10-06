package Client;

import Server.NetworkGameServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.InetAddress;

public class NetworkLobbyController {

    @FXML private Button hostButton;
    @FXML private Button joinButton;
    @FXML private TextField ipField;
    @FXML private Label statusLabel;
    @FXML private Label ipLabel;
    @FXML private Button backButton;

    private NetworkGameServer gameServer;
    private NetworkClient networkClient;

    @FXML
    private void initialize() {
        try {
            String localIP = InetAddress.getLocalHost().getHostAddress();
            ipLabel.setText("Your IP: " + localIP);
        } catch (Exception e) {
            ipLabel.setText("Your IP: Unable to detect");
        }

        ipField.setText("localhost");
        hostButton.setOnAction(e -> startHost());
        joinButton.setOnAction(e -> joinGame());
        backButton.setOnAction(e -> goBack());
    }

    private void startHost() {
        statusLabel.setText("Starting server...");
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        new Thread(() -> {
            try {
                gameServer = new NetworkGameServer();
                gameServer.start();
                Thread.sleep(500);

                networkClient = new NetworkClient("P1", "Player 1");
                setupHostCallback();
                networkClient.connect("localhost", 5555);

                Platform.runLater(() -> {
                    statusLabel.setText("Waiting for Player 2...");
                    statusLabel.setStyle("-fx-text-fill: yellow;");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void joinGame() {
        String serverIP = ipField.getText().trim();

        if (serverIP.isEmpty()) {
            statusLabel.setText("Please enter server IP");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        statusLabel.setText("Connecting to " + serverIP + "...");
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        new Thread(() -> {
            try {
                networkClient = new NetworkClient("P2", "Player 2");
                networkClient.setLobbyController(this); // ADD THIS LINE
                setupClientCallback();
                networkClient.connect(serverIP, 5555);

                Thread.sleep(1000);

                if (networkClient.isConnected()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Connected! Waiting for host to start game...");
                        statusLabel.setStyle("-fx-text-fill: lime; -fx-font-size: 16px;");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("Connection failed");
                        statusLabel.setStyle("-fx-text-fill: red;");
                        hostButton.setDisable(false);
                        joinButton.setDisable(false);
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void setupHostCallback() {
        networkClient.setCallback(new NetworkClient.NetworkCallback() {
            @Override
            public void onConnected() {}

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection lost!");
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
            }

            @Override
            public void onGameStart() {
                Platform.runLater(() -> {
                    statusLabel.setText("Player 2 connected! Starting...");
                    statusLabel.setStyle("-fx-text-fill: lime;");

                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(() -> proceedToCharacterSelect());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }

            @Override
            public void onInputReceived(String playerId, long frameNumber, short inputBits) {}

            @Override
            public void onPlayerDisconnected(String playerId) {}
        });
    }

    private void setupClientCallback() {
        networkClient.setCallback(new NetworkClient.NetworkCallback() {
            @Override
            public void onConnected() {}

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection lost!");
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
            }

            @Override
            public void onGameStart() {
                // Client waits for GAME_CONFIG
            }

            @Override
            public void onInputReceived(String playerId, long frameNumber, short inputBits) {}

            @Override
            public void onPlayerDisconnected(String playerId) {}

            @Override
            public void onGameConfig(String p1Char, String p2Char, String mapFile) {
                System.out.println("Client callback received game config");
                // launchGame will be called via reflection from NetworkClient
            }
        });
    }

    private void proceedToCharacterSelect() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/ChampSelect.fxml"));
            Parent root = loader.load();

            ChampSelectController controller = loader.getController();
            controller.setNetworkClient(networkClient);

            Stage stage = (Stage) hostButton.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Character Select");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void launchGame(String p1Char, String p2Char, String mapFile) {
        Platform.runLater(() -> {
            try {
                System.out.println("Client launching game: " + p1Char + " vs " + p2Char + " on " + mapFile);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/GameScene.fxml"));
                Parent root = loader.load();

                GameSceneController gameController = loader.getController();
                gameController.setGameData(p1Char, p2Char, mapFile);
                gameController.setNetworkMode(networkClient, "P2");

                Stage stage = (Stage) statusLabel.getScene().getWindow();
                stage.setScene(new Scene(root, 800, 400));
                stage.setTitle("Street Fighter");
                stage.setResizable(false);
                stage.sizeToScene();
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void goBack() {
        try {
            if (networkClient != null) networkClient.disconnect();
            if (gameServer != null) gameServer.stop();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/HomeUI.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Street Fighter");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}