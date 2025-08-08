package Client;

import db.DatabaseManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class LeaderboardUI {

    public static void display() {
        Stage stage = new Stage();
        stage.setTitle("Leaderboard");

        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        TableView<PlayerRecord> table = new TableView<>();
        table.setPlaceholder(new Label("No data found."));

        TableColumn<PlayerRecord, String> col1 = new TableColumn<>("Username");
        col1.setCellValueFactory(data -> data.getValue().usernameProperty());

        TableColumn<PlayerRecord, Integer> col2 = new TableColumn<>("Wins");
        col2.setCellValueFactory(data -> data.getValue().winsProperty().asObject());

        TableColumn<PlayerRecord, Integer> col3 = new TableColumn<>("Losses");
        col3.setCellValueFactory(data -> data.getValue().lossesProperty().asObject());

        TableColumn<PlayerRecord, Double> col4 = new TableColumn<>("Win Rate (%)");
        col4.setCellValueFactory(data -> data.getValue().winRateProperty().asObject());

        table.getColumns().addAll(col1, col2, col3, col4);

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM leaderboard")) {
            while (rs.next()) {
                String u = rs.getString("username");
                int w = rs.getInt("wins");
                int l = rs.getInt("losses");
                double r = rs.getDouble("win_rate");
                table.getItems().add(new PlayerRecord(u, w, l, r));
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database error: " + e.getMessage()).showAndWait();
        }

        root.getChildren().add(table);
        stage.setScene(new Scene(root, 500, 400));
        stage.show();
    }
}
