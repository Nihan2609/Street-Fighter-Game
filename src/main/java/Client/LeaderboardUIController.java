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

    private void loadLeaderboardFromDB() {
        String url = "jdbc:mysql://localhost:3306/street_fighter_game";
        String user = "root";     // change if you have another username
        String password = "1122";     // your MySQL password here

        String query = "SELECT username, wins, losses, " +
                "ROUND((wins / NULLIF((wins + losses), 0)) * 100, 2) AS win_rate " +
                "FROM players ORDER BY wins DESC, win_rate DESC";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                double winRate = rs.getDouble("win_rate");

                playerList.add(new PlayerStats(username, wins, losses, winRate));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(" Error loading leaderboard data from database.");
        }
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
