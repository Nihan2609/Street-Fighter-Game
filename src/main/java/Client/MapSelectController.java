package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

public class MapSelectController {

    @FXML private AnchorPane mainPane;
    @FXML private ImageView map1;
    @FXML private ImageView map2;
    @FXML private ImageView map3;
    @FXML private ImageView map4;
    @FXML private ImageView map5;
    @FXML private ImageView map6;
    @FXML private Label instructionsLabel;

    private String p1Choice;
    private String p2Choice;

    private int selectedMapIndex = 0; // 0-5
    private ImageView[] mapViews;
    private String[] mapNames = {"Sunken Sanctuary", "City Street", "Hidden Dojo", "Jungle Judgement", "Night sky ", "Showdown"};

    @FXML
    public void initialize() {
        mapViews = new ImageView[]{map1, map2, map3, map4, map5, map6};

        // Add a listener to handle key presses on the scene
        mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> handleKeyPress(e.getCode()));
            }
        });

        updateSelectionHighlight();
    }

    public void setPlayerChoices(String p1, String p2) {
        this.p1Choice = p1;
        this.p2Choice = p2;
    }

    private void handleKeyPress(KeyCode code) {
        if (code == KeyCode.A) {
            selectedMapIndex = (selectedMapIndex - 1 + mapViews.length) % mapViews.length;
        } else if (code == KeyCode.D) {
            selectedMapIndex = (selectedMapIndex + 1) % mapViews.length;
        } else if (code == KeyCode.ENTER) {
            String mapChoice = getMapName(selectedMapIndex);

            // Now we actually load the game scene and pass the data
            loadGame(mapChoice);
        }

        updateSelectionHighlight();
    }

    private void updateSelectionHighlight() {
        for (int i = 0; i < mapViews.length; i++) {
            ColorAdjust ca = new ColorAdjust();
            if (i == selectedMapIndex) {
                // Brighter highlight for the selected map
                ca.setBrightness(0.5);
            } else {
                ca.setBrightness(0.0);
            }
            mapViews[i].setEffect(ca);
        }
    }

    private String getMapName(int index) {
        if (index >= 0 && index < mapNames.length) {
            return mapNames[index];
        }
        return "Unknown Map";
    }
    private void loadGame(String mapChoice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/GameScene.fxml"));
            Parent root = loader.load();

            GameScene controller = loader.getController();
            controller.startGame(p1Choice, p2Choice, mapChoice);

            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Street Fighter");
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
