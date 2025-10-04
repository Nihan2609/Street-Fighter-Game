package Client;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Unified Character Selection Controller
 * Supports both local (same device) and network (LAN) modes
 */
public class ChampSelectController {

    @FXML private AnchorPane mainPane;
    @FXML private ImageView bgView;
    @FXML private ImageView p1ImgView;
    @FXML private ImageView p2ImgView;
    @FXML private Label p1NameLabel;
    @FXML private Label p2NameLabel;
    @FXML private Label statusLabel;
    @FXML private Label networkStatusLabel; // Optional - add to FXML if needed

    private int p1Index = 0;
    private int p2Index = 1;
    private boolean p1Locked = false;
    private boolean p2Locked = false;

    private Image ryuImg;
    private Image kenImg;
    private FadeTransition fadeTransition;

    // Network mode components (null = local mode)
    private NetworkClient networkClient = null;
    private String localPlayerId = null;
    private boolean isNetworkMode = false;

    @FXML
    public void initialize() {
        ryuImg = new Image(getClass().getResource("/images/ryuSelect.png").toExternalForm());
        kenImg = new Image(getClass().getResource("/images/kenSelect.png").toExternalForm());

        fadeTransition = new FadeTransition(Duration.seconds(1.0), statusLabel);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.setCycleCount(FadeTransition.INDEFINITE);
        fadeTransition.setAutoReverse(true);

        updateUI();

        mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> handleKeyPress(e.getCode()));
            }
        });
    }

    /**
     * Enable network mode - called when starting network multiplayer
     */
    public void setNetworkMode(NetworkClient client, String playerId) {
        this.networkClient = client;
        this.localPlayerId = playerId;
        this.isNetworkMode = true;

        // Set up network callback for receiving opponent's selection
        if (networkClient != null) {
            networkClient.setCallback(new NetworkClient.NetworkCallback() {
                @Override
                public void onConnected() {
                    Platform.runLater(() -> updateNetworkStatus("Connected"));
                }

                @Override
                public void onDisconnected() {
                    Platform.runLater(() -> {
                        updateNetworkStatus("Disconnected!");
                        statusLabel.setText("Connection lost!");
                    });
                }

                @Override
                public void onGameStart() {
                    // Handled elsewhere
                }

                @Override
                public void onInputReceived(String playerId, long frameNumber, short inputBits) {
                    handleNetworkSelection(inputBits);
                }

                @Override
                public void onPlayerDisconnected(String playerId) {
                    Platform.runLater(() -> {
                        updateNetworkStatus("Opponent left");
                        statusLabel.setText("Opponent disconnected!");
                    });
                }
            });

            updateNetworkStatus("Connected - Selecting characters...");
        }
    }

    private void handleKeyPress(KeyCode code) {
        boolean selectionChanged = false;

        if (isNetworkMode) {
            // Network mode: Only control your own player
            if (localPlayerId.equals("P1") && !p1Locked) {
                if (code == KeyCode.A || code == KeyCode.D) {
                    p1Index = (p1Index == 0) ? 1 : 0;
                    selectionChanged = true;
                } else if (code == KeyCode.Q) {
                    p1Locked = true;
                    selectionChanged = true;
                    AudioManager.playConfirmSound();
                }
            } else if (localPlayerId.equals("P2") && !p2Locked) {
                if (code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                    p2Index = (p2Index == 0) ? 1 : 0;
                    selectionChanged = true;
                } else if (code == KeyCode.ENTER) {
                    p2Locked = true;
                    selectionChanged = true;
                    AudioManager.playConfirmSound();
                }
            }

            if (selectionChanged) {
                updateUI();
                sendSelectionToNetwork();
            }

            if (p1Locked && p2Locked && isNetworkMode) {
                // Prevent multiple calls
                if (!p1Locked || !p2Locked) return;

                Platform.runLater(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    proceedToMapSelect();
                });
            }

        } else {
            // Local mode: Both players on same device
            if (!p1Locked) {
                if (code == KeyCode.A || code == KeyCode.D) {
                    p1Index = (p1Index == 0) ? 1 : 0;
                } else if (code == KeyCode.Q) {
                    p1Locked = true;
                }
            }

            if (!p2Locked) {
                if (code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                    p2Index = (p2Index == 0) ? 1 : 0;
                } else if (code == KeyCode.ENTER) {
                    p2Locked = true;
                }
            }

            if (p1Locked && p2Locked && code == KeyCode.SPACE) {
                AudioManager.playSelectSound();
                proceedToMapSelect();
            }

            updateUI();
        }
    }

    private void sendSelectionToNetwork() {
        if (networkClient == null || !networkClient.isConnected()) return;

        short selectionData = packSelectionData();
        networkClient.sendInput(selectionData);
    }

    private short packSelectionData() {
        short data = 0;

        if (localPlayerId.equals("P1")) {
            data |= (p1Index & 0x0F);
            data |= (p1Locked ? 1 : 0) << 4;
        } else {
            data |= ((p2Index & 0x0F) << 5);
            data |= (p2Locked ? 1 : 0) << 9;
        }

        return data;
    }

    private void handleNetworkSelection(short selectionData) {
        Platform.runLater(() -> {
            if (localPlayerId.equals("P1")) {
                p2Index = (selectionData >> 5) & 0x0F;
                p2Locked = ((selectionData >> 9) & 0x01) == 1;
            } else {
                p1Index = selectionData & 0x0F;
                p1Locked = ((selectionData >> 4) & 0x01) == 1;
            }

            updateUI();

            // Auto-proceed when both locked
            if (p1Locked && p2Locked) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(this::proceedToMapSelect);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        });
    }

    private void updateUI() {
        // Update Player 1
        p1ImgView.setImage((p1Index == 0) ? ryuImg : kenImg);
        String p1Status = p1Locked ? " ✓" : "";
        p1NameLabel.setText("Player 1: " + (p1Index == 0 ? "Ryu" : "Ken") + p1Status);

        // Update Player 2
        p2ImgView.setImage((p2Index == 0) ? ryuImg : kenImg);
        String p2Status = p2Locked ? " ✓" : "";
        p2NameLabel.setText("Player 2: " + (p2Index == 0 ? "Ryu" : "Ken") + p2Status);

        // Update status label
        if (p1Locked && p2Locked) {
            statusLabel.setText(isNetworkMode ? "Both Players Ready! Starting..." : "Both Players Ready! Press SPACE to Start");
            fadeTransition.play();
        } else if (isNetworkMode) {
            if (localPlayerId.equals("P1") && !p1Locked) {
                statusLabel.setText("P1: A/D to switch, Q to confirm");
            } else if (localPlayerId.equals("P2") && !p2Locked) {
                statusLabel.setText("P2: ←/→ to switch, ENTER to confirm");
            } else {
                statusLabel.setText("Waiting for opponent...");
            }
            fadeTransition.stop();
            statusLabel.setOpacity(1.0);
        } else {
            statusLabel.setText("");
            fadeTransition.stop();
            statusLabel.setOpacity(1.0);
        }
    }

    private void updateNetworkStatus(String status) {
        if (networkStatusLabel != null) {
            networkStatusLabel.setText("Network: " + status);
            networkStatusLabel.setVisible(true);
        }
    }

    private void proceedToMapSelect() {
        String p1Choice = (p1Index == 0 ? "Ryu" : "Ken");
        String p2Choice = (p2Index == 0 ? "Ryu" : "Ken");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/MapSelect.fxml"));
            Parent root = loader.load();

            MapSelectController controller = loader.getController();
            controller.setPlayerChoices(p1Choice, p2Choice);

            // Pass network components if in network mode
            if (isNetworkMode && networkClient != null) {
                controller.setNetworkMode(networkClient, localPlayerId);
            }

            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Select Map");
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}