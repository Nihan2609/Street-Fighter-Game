package Client;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class LeaderboardUI {

    public static void display() {
        Stage stage = new Stage();
        stage.setTitle("Leaderboard");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        TableView<PlayerRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No data found."));

        TableColumn<PlayerRecord, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<PlayerRecord, Integer> winsCol = new TableColumn<>("Wins");
        winsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));

        TableColumn<PlayerRecord, Integer> lossesCol = new TableColumn<>("Losses");
        lossesCol.setCellValueFactory(new PropertyValueFactory<>("losses"));

        TableColumn<PlayerRecord, Double> winRateCol = new TableColumn<>("Win Rate (%)");
        winRateCol.setCellValueFactory(new PropertyValueFactory<>("winRate"));

        table.getColumns().addAll(usernameCol, winsCol, lossesCol, winRateCol);

        // Load data
        try (ResultSet rs = LeaderboardDAO.getLeaderboard()) {
            while (rs != null && rs.next()) {
                String username = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                double winRate = rs.getDouble("win_rate");
                table.getItems().add(new PlayerRecord(username, wins, losses, winRate));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        root.getChildren().add(table);

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
    }
}
