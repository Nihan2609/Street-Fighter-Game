package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

public class MapSelectController {

    @FXML private ComboBox<String> mapChoice;
    @FXML private Button startBtn;

    private String p1;
    private String p2;
    private Stage stage;

    @FXML
    public void initialize() {
        // Add maps here (names must match your image/stage files later)
        mapChoice.getItems().addAll("Stage1", "Stage2", "Stage3");
        mapChoice.getSelectionModel().selectFirst();

        startBtn.setOnAction(e -> startGame());
    }

    public void setPlayerChoices(String p1, String p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void startGame() {
        String chosenMap = mapChoice.getValue();
        System.out.println("🌍 Map chosen: " + chosenMap);
        System.out.println("🎮 Starting game with P1 = " + p1 + " vs P2 = " + p2);

        // TODO: Replace with your actual GameScene initialization
        new GameScene(stage, p1, p2, chosenMap);
    }
}
