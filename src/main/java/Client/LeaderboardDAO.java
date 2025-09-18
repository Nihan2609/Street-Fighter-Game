package Client;

import db.DatabaseManager;

import java.sql.*;

public class LeaderboardDAO {

    // Update player's wins or losses
    public static void updateRecord(String username, boolean win) {
        try (Connection conn = DatabaseManager.getConnection()) {

            // Make sure player exists
            String insertSql = "INSERT IGNORE INTO players (username, password, wins, losses) VALUES (?, '', 0, 0)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.executeUpdate();
            }

            // Update wins or losses
            String updateSql = win
                    ? "UPDATE players SET wins = wins + 1 WHERE username = ?"
                    : "UPDATE players SET losses = losses + 1 WHERE username = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, username);
                updateStmt.executeUpdate();
            }

            System.out.println("Record updated for: " + username);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Retrieve leaderboard dynamically with calculated win_rate
    public static ResultSet getLeaderboard() {
        String sql = "SELECT username, wins, losses, " +
                "IF((wins + losses)=0, 0, wins*100/(wins+losses)) AS win_rate " +
                "FROM players ORDER BY wins DESC";
        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
