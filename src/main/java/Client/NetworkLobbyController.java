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

/**
 * Simple Network Lobby - Just Host or Join
 */
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
        // Display local IP for host
        try {
            String localIP = InetAddress.getLocalHost().getHostAddress();
            ipLabel.setText("Your IP: " + localIP);
        } catch (Exception e) {
            ipLabel.setText("Your IP: Unable to detect");
        }

        // Set default localhost for testing
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
                // Start server
                gameServer = new NetworkGameServer();
                gameServer.start();

                Thread.sleep(500); // Let server start

                // Connect as P1
                networkClient = new NetworkClient("P1", "Player 1");
                setupNetworkCallback(true);
                networkClient.connect("localhost", 5555);

                Platform.runLater(() -> {
                    statusLabel.setText("Waiting for Player 2 to join...");
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
                // Connect as P2
                networkClient = new NetworkClient("P2", "Player 2");
                setupNetworkCallback(false);
                networkClient.connect(serverIP, 5555);

                Thread.sleep(1000); // Wait for connection

                if (networkClient.isConnected()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Connected! Waiting for host...");
                        statusLabel.setStyle("-fx-text-fill: lime;");
                    });
                } else {
                    Platform.runLater(() -> {
                        statusLabel.setText("Connection failed. Check IP and try again.");
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

    private void setupNetworkCallback(boolean isHost) {
        networkClient.setCallback(new NetworkClient.NetworkCallback() {
            @Override
            public void onConnected() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connected!");
                    statusLabel.setStyle("-fx-text-fill: lime;");
                });
            }

            @Override
            public void onDisconnected() {
                Platform.runLater(() -> {
                    statusLabel.setText("Connection lost!");
                    statusLabel.setStyle("-fx-text-fill: red;");
                    hostButton.setDisable(false);
                    joinButton.setDisable(false);
                });
            }

            @Override
            public void onGameStart() {
                // Both players connected - show confirmation and proceed
                Platform.runLater(() -> {
                    showConnectionConfirmation(isHost);
                });
            }

            @Override
            public void onInputReceived(String playerId, long frameNumber, short inputBits) {
                // Handled in game controllers
            }

            @Override
            public void onPlayerDisconnected(String playerId) {
                Platform.runLater(() -> {
                    statusLabel.setText("Opponent disconnected!");
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        });
    }

    private void showConnectionConfirmation(boolean isHost) {
        statusLabel.setText("✓ Both players connected! Starting game...");
        statusLabel.setStyle("-fx-text-fill: lime; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Wait 2 seconds to show confirmation, then proceed
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> proceedToCharacterSelect(isHost));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void proceedToCharacterSelect(boolean isHost) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/ChampSelect.fxml"));
            Parent root = loader.load();

            ChampSelectController controller = loader.getController();
            controller.setNetworkMode(networkClient, isHost ? "P1" : "P2");

            Stage stage = (Stage) hostButton.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle(isHost ? "Character Select - Host (P1)" : "Character Select - Client (P2)");
            stage.show();

        } catch (Exception e) {
            statusLabel.setText("Error loading character select");
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    private void goBack() {
        try {
            // Cleanup network connections
            if (networkClient != null) {
                networkClient.disconnect();
            }
            if (gameServer != null) {
                gameServer.stop();
            }

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