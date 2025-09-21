package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class GameController {

    @FXML private ImageView background;
    @FXML private ProgressBar p1Health, p2Health;
    @FXML private Label roundCounter;
    @FXML private ImageView p1Sprite, p2Sprite;
    @FXML private Pane arena;

    private String p1Selection;
    private String p2Selection;

    private int p1Rounds = 0;
    private int p2Rounds = 0;

    public void initPlayers(String p1, String p2) {
        this.p1Selection = p1;
        this.p2Selection = p2;

        // Load default stage (replace with multiple maps later)
        background.setImage(new Image(getClass().getResource("/images/charSelectBG.jpg").toExternalForm()));

        // Load fighters based on champ select
        if ("Ryu".equalsIgnoreCase(p1)) {
            p1Sprite.setImage(new Image(getClass().getResource("/images/ryuSelect.png").toExternalForm()));
        }
        if ("Ken".equalsIgnoreCase(p2)) {
            p2Sprite.setImage(new Image(getClass().getResource("/images/kenSelect.png").toExternalForm()));
        }

        // Reset HUD
        p1Health.setProgress(1.0);
        p2Health.setProgress(1.0);
        roundCounter.setText("0 - 0");
    }

    // Example attack system (placeholder)
    public void player1Hit() {
        double hp = p2Health.getProgress() - 0.1;
        p2Health.setProgress(Math.max(hp, 0));
        checkRoundEnd();
    }

    public void player2Hit() {
        double hp = p1Health.getProgress() - 0.1;
        p1Health.setProgress(Math.max(hp, 0));
        checkRoundEnd();
    }

    private void checkRoundEnd() {
        if (p1Health.getProgress() <= 0) {
            p2Rounds++;
            resetRound();
        } else if (p2Health.getProgress() <= 0) {
            p1Rounds++;
            resetRound();
        }
    }

    private void resetRound() {
        p1Health.setProgress(1.0);
        p2Health.setProgress(1.0);
        roundCounter.setText(p1Rounds + " - " + p2Rounds);

        if (p1Rounds == 2 || p2Rounds == 2) {
            System.out.println("Game Over! Winner: " + (p1Rounds == 2 ? "Player 1" : "Player 2"));
            // TODO: End game / return to menu
        }
    }
}
