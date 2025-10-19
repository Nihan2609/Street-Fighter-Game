package Client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import db.DatabaseManager;

import java.io.IOException;
import java.sql.*;
import java.util.Objects;

public class LeaderboardUIController {

    @FXML private TableView<PlayerStats> leaderboardTable;
    @FXML private TableColumn<PlayerStats, String> usernameCol;
    @FXML private TableColumn<PlayerStats, Integer> winsCol;
    @FXML private TableColumn<PlayerStats, Integer> lossesCol;
    @FXML private TableColumn<PlayerStats, String> winRateCol; // Changed to String for formatting

    @FXML private Button backBtn;

    private ObservableList<PlayerStats> playerList = FXCollections.observableArrayList();
    private FontManager fontManager = FontManager.getInstance();

    @FXML
    private void initialize() {
        fontManager.initialize();

        // Set up cell value factories
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        winsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        lossesCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        winRateCol.setCellValueFactory(new PropertyValueFactory<>("winRateFormatted"));

        // Apply custom styling to table
        leaderboardTable.setStyle(
                "-fx-background-color: #2c3e50; " +
                        "-fx-font-family: 'Press Start 2P'; " +
                        "-fx-font-size: 10px;"
        );

        // Style table cells
        usernameCol.setStyle("-fx-alignment: CENTER-LEFT;");
        winsCol.setStyle("-fx-alignment: CENTER;");
        lossesCol.setStyle("-fx-alignment: CENTER;");
        winRateCol.setStyle("-fx-alignment: CENTER;");

        // Load data from database
        loadLeaderboardFromDB();
        leaderboardTable.setItems(playerList);

        // Apply custom font to button
        if (backBtn != null) {
            backBtn.setStyle(fontManager.getStyleString(12) +
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        }

        backBtn.setOnAction(e -> goBack());
    }

    private void loadLeaderboardFromDB() {
        String query = "SELECT username, wins, losses, " +
                "CASE " +
                "  WHEN (wins + losses) = 0 THEN 0 " +
                "  ELSE ROUND((wins / (wins + losses)) * 100, 1) " +
                "END AS win_rate " +
                "FROM players " +
                "ORDER BY wins DESC, win_rate DESC " +
                "LIMIT 50";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            playerList.clear(); // Clear existing data

            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                double winRate = rs.getDouble("win_rate");

                playerList.add(new PlayerStats(username, wins, losses, winRate));
                rank++;
            }

            System.out.println("Loaded " + playerList.size() + " players from leaderboard");

        } catch (SQLException e) {
            System.err.println("Error loading leaderboard data: " + e.getMessage());
            e.printStackTrace();

            // Add dummy data if database fails (for testing)
            playerList.add(new PlayerStats("Database Error", 0, 0, 0.0));
        }
    }

    private void goBack() {
        try {
            AudioManager.playSelectSound();

            Stage stage = (Stage) backBtn.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/com/example/game/HomeUI.fxml")));
            Scene scene = new Scene(root, 600, 400);
            stage.setScene(scene);
            stage.setTitle("Street Fighter");
            stage.show();
        } catch (IOException ex) {
            System.err.println("Error going back: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // PlayerStats class with proper getters
    public static class PlayerStats {
        private final String username;
        private final int wins;
        private final int losses;
        private final double winRate;
        private final String winRateFormatted;

        public PlayerStats(String username, int wins, int losses, double winRate) {
            this.username = username;
            this.wins = wins;
            this.losses = losses;
            this.winRate = winRate;
            this.winRateFormatted = String.format("%.1f%%", winRate);
        }

        // Getters
        public String getUsername() {
            return username;
        }

        public int getWins() {
            return wins;
        }

        public int getLosses() {
            return losses;
        }

        public double getWinRate() {
            return winRate;
        }

        public String getWinRateFormatted() {
            return winRateFormatted;
        }

        @Override
        public String toString() {
            return username + " - W:" + wins + " L:" + losses + " (" + winRateFormatted + ")";
        }
    }
}