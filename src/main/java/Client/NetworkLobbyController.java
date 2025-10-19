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
    private FontManager fontManager = FontManager.getInstance();

    @FXML
    private void initialize() {
        fontManager.initialize();

        try {
            String localIP = InetAddress.getLocalHost().getHostAddress();
            ipLabel.setText("Your IP: " + localIP);
            ipLabel.setStyle(fontManager.getStyleString(12, "lime"));
        } catch (Exception e) {
            ipLabel.setText("IP: N/A");
            ipLabel.setStyle(fontManager.getStyleString(10, "yellow"));
        }

        applyCustomFont();

        ipField.setText("localhost");
        ipField.setStyle(fontManager.getStyleString(12) +
                "-fx-background-color: white; -fx-background-radius: 5; -fx-padding: 5;");

        hostButton.setOnAction(e -> startHost());
        joinButton.setOnAction(e -> joinGame());
        backButton.setOnAction(e -> goBack());
    }

    private void applyCustomFont() {
        String btnStyle = fontManager.getStyleString(14) +
                "-fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;";

        if (hostButton != null)
            hostButton.setStyle(btnStyle + "-fx-background-color: #27ae60;");

        if (joinButton != null)
            joinButton.setStyle(btnStyle + "-fx-background-color: #3498db;");

        if (backButton != null)
            backButton.setStyle(fontManager.getStyleString(12) +
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");

        if (statusLabel != null)
            statusLabel.setStyle(fontManager.getStyleString(12, "white"));
    }

    private void startHost() {
        statusLabel.setText("Starting server...");
        statusLabel.setStyle(fontManager.getStyleString(12, "yellow"));
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
                    statusLabel.setText("Waiting for P2...");
                    statusLabel.setStyle(fontManager.getStyleString(12, "yellow"));
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle(fontManager.getStyleString(10, "red"));
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            }
        }).start();
    }

    private void joinGame() {
        String serverIP = ipField.getText().trim();

        if (serverIP.isEmpty()) {
            statusLabel.setText("Enter server IP");
            statusLabel.setStyle(fontManager.getStyleString(10, "red"));
            return;
        }

        statusLabel.setText("Connecting...");
        statusLabel.setStyle(fontManager.getStyleString(12, "yellow"));
        hostButton.setDisable(true);
        joinButton.setDisable(true);

        new Thread(() -> {
            try {
                networkClient = new NetworkClient("P2", "Player 2");
                networkClient.setLobbyController(this);
                setupClientCallback();
                networkClient.connect(serverIP, 5555);

                Thread.sleep(1000);

                if (networkClient.isConnected()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Connected! Waiting for host...");
                        statusLabel.setStyle(fontManager.getStyleString(14, "lime"));
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("Failed");
                        statusLabel.setStyle(fontManager.getStyleString(12, "red"));
                        hostButton.setDisable(false);
                        joinButton.setDisable(false);
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setStyle(fontManager.getStyleString(10, "red"));
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            }
        }).start();
    }

    private void setupHostCallback() {
        networkClient.setCallback(new NetworkClient.NetworkCallback() {
            @Override
            public void onConnected() {
                System.out.println("Connected");
            }

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection lost!");
                    statusLabel.setStyle(fontManager.getStyleString(12, "red"));
                });
            }

            @Override
            public void onGameStart() {
                Platform.runLater(() -> {
                    statusLabel.setText("P2 connected!");
                    statusLabel.setStyle(fontManager.getStyleString(12, "lime"));

                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            Platform.runLater(() -> proceedToCharacterSelect());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                });
            }

            @Override
            public void onInputReceived(String playerId, long frameNumber, short inputBits) {}

            @Override
            public void onPlayerDisconnected(String playerId) {}

            @Override
            public void onGameConfig(String p1Char, String p2Char, String mapFile) {}

            @Override
            public void onPauseGame(String pausedBy) {}

            @Override
            public void onResumeGame() {}

            @Override
            public void onRematchRequest() {}

            @Override
            public void onNextRound(int round, int p1Wins, int p2Wins) {}

            @Override
            public void onWaitingForHost() {}
        });
    }

    private void setupClientCallback() {
        networkClient.setCallback(new NetworkClient.NetworkCallback() {
            @Override
            public void onConnected() {
                System.out.println("Connected");
                Platform.runLater(() -> {
                    statusLabel.setText("Connected! Waiting for host...");
                    statusLabel.setStyle(fontManager.getStyleString(14, "lime"));
                });
            }

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection lost!");
                    statusLabel.setStyle(fontManager.getStyleString(12, "red"));
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            }

            @Override
            public void onGameStart() {
                System.out.println("Client callback: onGameStart - Both players connected");
                Platform.runLater(() -> {
                    statusLabel.setText("Host is selecting characters...");
                    statusLabel.setStyle(fontManager.getStyleString(12, "yellow"));
                });
            }

            @Override
            public void onInputReceived(String playerId, long frameNumber, short inputBits) {}

            @Override
            public void onPlayerDisconnected(String playerId) {
                Platform.runLater(() -> {
                    statusLabel.setText("Host disconnected!");
                    statusLabel.setStyle(fontManager.getStyleString(12, "red"));
                });
            }

            @Override
            public void onGameConfig(String p1Char, String p2Char, String mapFile) {
                System.out.println("Client callback: onGameConfig - " + p1Char + " vs " + p2Char + " on " + mapFile);
                Platform.runLater(() -> {
                    statusLabel.setText("Starting game...");
                    statusLabel.setStyle(fontManager.getStyleString(12, "lime"));
                });
            }

            @Override
            public void onPauseGame(String pausedBy) {}

            @Override
            public void onResumeGame() {}

            @Override
            public void onRematchRequest() {}

            @Override
            public void onNextRound(int round, int p1Wins, int p2Wins) {}

            @Override
            public void onWaitingForHost() {}
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
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public void launchGame(String p1Char, String p2Char, String mapFile) {
        System.out.println("launchGame called: " + p1Char + " vs " + p2Char + " on " + mapFile);

        Platform.runLater(() -> {
            try {
                statusLabel.setText("Loading game...");
                statusLabel.setStyle(fontManager.getStyleString(12, "yellow"));

                Thread.sleep(100);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/GameScene.fxml"));
                Parent root = loader.load();

                GameSceneController gameController = loader.getController();
                gameController.setNetworkMode(networkClient, "P2");
                gameController.setGameData(p1Char, p2Char, mapFile);

                Stage stage = (Stage) statusLabel.getScene().getWindow();
                Scene scene = new Scene(root, 800, 400);
                stage.setScene(scene);
                stage.setTitle("Street Fighter - Network Game (CLIENT)");
                stage.setResizable(false);
                stage.sizeToScene();
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
                statusLabel.setText("Error loading game!");
                statusLabel.setStyle(fontManager.getStyleString(10, "red"));
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
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}