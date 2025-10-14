package Client;

import javafx.animation.FadeTransition;
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

public class ChampSelectController {

    @FXML private AnchorPane mainPane;
    @FXML private ImageView bgView;
    @FXML private ImageView p1ImgView;
    @FXML private ImageView p2ImgView;
    @FXML private Label p1NameLabel;
    @FXML private Label p2NameLabel;
    @FXML private Label statusLabel;
    @FXML private Label p1Label;
    @FXML private Label p2Label;
    @FXML private Label p1ControlLabel;
    @FXML private Label p2ControlLabel;

    private int p1Index = 0;
    private int p2Index = 1;
    private boolean p1Locked = false;
    private boolean p2Locked = false;

    private Image ryuImg;
    private Image kenImg;
    private FadeTransition fadeTransition;

    private NetworkClient networkClient = null;
    private boolean isHost = false;
    private FontManager fontManager = FontManager.getInstance();

    @FXML
    public void initialize() {
        fontManager.initialize();

        ryuImg = new Image(getClass().getResource("/images/ryuSelect.png").toExternalForm());
        kenImg = new Image(getClass().getResource("/images/kenSelect.png").toExternalForm());

        fadeTransition = new FadeTransition(Duration.seconds(1.0), statusLabel);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.setCycleCount(FadeTransition.INDEFINITE);
        fadeTransition.setAutoReverse(true);

        applyCustomFont();
        updateUI();

        mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> handleKeyPress(e.getCode()));
            }
        });
    }

    private void applyCustomFont() {
        if (p1Label != null) {
            p1Label.setStyle(fontManager.getStyleString(14, "#FF0000"));
        }
        if (p2Label != null) {
            p2Label.setStyle(fontManager.getStyleString(14, "#0000FF"));
        }
        if (p1NameLabel != null) {
            p1NameLabel.setStyle(fontManager.getStyleString(12, "#FF0000"));
        }
        if (p2NameLabel != null) {
            p2NameLabel.setStyle(fontManager.getStyleString(12, "#0000FF"));
        }
        if (statusLabel != null) {
            statusLabel.setStyle(fontManager.getStyleString(14, "#00FF00"));
        }
        if (p1ControlLabel != null) {
            p1ControlLabel.setStyle(fontManager.getStyleString(10, "yellow"));
        }
        if (p2ControlLabel != null) {
            p2ControlLabel.setStyle(fontManager.getStyleString(10, "yellow"));
        }
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
        this.isHost = client != null && client.isHost();

        if (!isHost && networkClient != null) {
            statusLabel.setText("HOST is selecting...");
            statusLabel.setStyle(fontManager.getStyleString(12, "yellow"));
            statusLabel.setOpacity(1.0);
            fadeTransition.stop();
        }
    }

    private void handleKeyPress(KeyCode code) {
        if (networkClient != null && !isHost) {
            return;
        }

        if (!p1Locked) {
            if (code == KeyCode.A || code == KeyCode.D) {
                p1Index = (p1Index == 0) ? 1 : 0;
                AudioManager.playSelectSound();
            } else if (code == KeyCode.Q) {
                p1Locked = true;
                AudioManager.playConfirmSound();
            }
        }

        if (!p2Locked) {
            if (code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                p2Index = (p2Index == 0) ? 1 : 0;
                AudioManager.playSelectSound();
            } else if (code == KeyCode.ENTER) {
                p2Locked = true;
                AudioManager.playConfirmSound();
            }
        }

        if (p1Locked && p2Locked && code == KeyCode.SPACE) {
            AudioManager.playSelectSound();
            proceedToMapSelect();
        }

        updateUI();
    }

    private void updateUI() {
        p1ImgView.setImage((p1Index == 0) ? ryuImg : kenImg);
        String p1Status = p1Locked ? " ✓" : "";
        p1NameLabel.setText("P1: " + (p1Index == 0 ? "Ryu" : "Ken") + p1Status);

        p2ImgView.setImage((p2Index == 0) ? ryuImg : kenImg);
        String p2Status = p2Locked ? " ✓" : "";
        p2NameLabel.setText("P2: " + (p2Index == 0 ? "Ryu" : "Ken") + p2Status);

        if (networkClient == null || isHost) {
            if (p1Locked && p2Locked) {
                statusLabel.setText("Press SPACE");
                statusLabel.setStyle(fontManager.getStyleString(16, "lime"));
                fadeTransition.play();
            } else {
                statusLabel.setText("Select Characters");
                statusLabel.setStyle(fontManager.getStyleString(14, "white"));
                fadeTransition.stop();
                statusLabel.setOpacity(1.0);
            }
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

            if (networkClient != null) {
                controller.setNetworkClient(networkClient);
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