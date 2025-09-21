package Client;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class LeaderboardUIController {

    @FXML private TableView<PlayerStats> leaderboardTable;
    @FXML private TableColumn<PlayerStats, String> usernameCol;
    @FXML private TableColumn<PlayerStats, Integer> winsCol;
    @FXML private TableColumn<PlayerStats, Integer> lossesCol;
    @FXML private TableColumn<PlayerStats, Double> winRateCol;

    @FXML private Button backBtn;

    @FXML
    private void initialize() {
        // Set up columns
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        winsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        lossesCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        winRateCol.setCellValueFactory(new PropertyValueFactory<>("winRate"));

        backBtn.setOnAction(e -> goBack());
    }

    private void goBack() {
        try {
            Stage stage = (Stage) backBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/example/game/HomeUI.fxml")));
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            stage.setTitle("Street Fighter");
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Class to hold player stats
    public static class PlayerStats {
        private String username;
        private int wins;
        private int losses;
        private double winRate;

        public PlayerStats(String username, int wins, int losses, double winRate) {
            this.username = username;
            this.wins = wins;
            this.losses = losses;
            this.winRate = winRate;
        }

        public String getUsername() { return username; }
        public int getWins() { return wins; }
        public int getLosses() { return losses; }
        public double getWinRate() { return winRate; }
    }
}
