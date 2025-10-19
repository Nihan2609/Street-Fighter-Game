package db;

import java.sql.*;

public class DatabaseManager {
    // IMPORTANT: Update this password to match your MySQL setup
    private static final String DB_URL = "jdbc:mysql://localhost:3306/street_fighter_game";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "212001";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // Test connection
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✓ Database connection successful!");
            return true;
        } catch (SQLException e) {
            System.err.println("✗ Database connection failed: " + e.getMessage());
            return false;
        }
    }

    public static int getPlayerId(String username) {
        String sql = "SELECT id FROM players WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (SQLException e) {
            System.err.println("Error getting player ID: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private static int getPlayerId(Connection conn, String username) throws SQLException {
        String query = "SELECT id FROM players WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }

    public static boolean registerPlayer(String username, String password) {
        String sql = "INSERT INTO players (username, password, wins, losses) VALUES (?, ?, 0, 0)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // In production, use password hashing!
            stmt.executeUpdate();

            System.out.println("✓ Player registered successfully: " + username);
            return true;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Duplicate entry
                System.err.println("✗ Username already exists: " + username);
            } else {
                System.err.println("✗ Error registering player: " + e.getMessage());
            }
            return false;
        }
    }

    public static boolean loginPlayer(String username, String password) {
        String sql = "SELECT * FROM players WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("✓ Login successful: " + username);
                return true;
            } else {
                System.out.println("✗ Login failed: Invalid credentials");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("✗ Error during login: " + e.getMessage());
            return false;
        }
    }

    public static void recordMatch(String winnerUsername, String loserUsername) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                int winnerId = getPlayerId(conn, winnerUsername);
                int loserId = getPlayerId(conn, loserUsername);

                if (winnerId == -1 || loserId == -1) {
                    System.err.println("✗ Player not found in database");
                    conn.rollback();
                    return;
                }

                // Insert into matches table
                String insertMatch = "INSERT INTO matches (player1_id, player2_id, winner_id, match_date) VALUES (?, ?, ?, NOW())";
                try (PreparedStatement stmt = conn.prepareStatement(insertMatch)) {
                    stmt.setInt(1, winnerId);
                    stmt.setInt(2, loserId);
                    stmt.setInt(3, winnerId);
                    stmt.executeUpdate();
                }

                // Update winner stats
                updateStats(conn, winnerId, true);

                // Update loser stats
                updateStats(conn, loserId, false);

                conn.commit();
                System.out.println("✓ Match recorded: " + winnerUsername + " defeated " + loserUsername);

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("✗ Error recording match: " + e.getMessage());
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void recordMatch(int player1Id, int player2Id, int winnerId) {
        String matchSql = "INSERT INTO matches (player1_id, player2_id, winner_id, match_date) VALUES (?, ?, ?, NOW())";
        String updateWinSql = "UPDATE players SET wins = wins + 1 WHERE id = ?";
        String updateLoseSql = "UPDATE players SET losses = losses + 1 WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement matchStmt = conn.prepareStatement(matchSql);
                    PreparedStatement winStmt = conn.prepareStatement(updateWinSql);
                    PreparedStatement loseStmt = conn.prepareStatement(updateLoseSql)
            ) {
                // Record match
                matchStmt.setInt(1, player1Id);
                matchStmt.setInt(2, player2Id);
                matchStmt.setInt(3, winnerId);
                matchStmt.executeUpdate();

                // Update winner
                winStmt.setInt(1, winnerId);
                winStmt.executeUpdate();

                // Update loser
                int loserId = (winnerId == player1Id) ? player2Id : player1Id;
                loseStmt.setInt(1, loserId);
                loseStmt.executeUpdate();

                conn.commit();
                System.out.println("✓ Match recorded successfully (by ID)");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("✗ Error recording match: " + e.getMessage());
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void updateStats(Connection conn, int playerId, boolean won) throws SQLException {
        String sql = won
                ? "UPDATE players SET wins = wins + 1 WHERE id = ?"
                : "UPDATE players SET losses = losses + 1 WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✓ Updated stats for player ID " + playerId + " (" + (won ? "WIN" : "LOSS") + ")");
            }
        }
    }

    public static void showLeaderboard() {
        String sql = "SELECT username, wins, losses, " +
                "CASE " +
                "  WHEN (wins + losses) = 0 THEN 0 " +
                "  ELSE ROUND((wins / (wins + losses)) * 100, 1) " +
                "END AS win_rate " +
                "FROM players " +
                "ORDER BY wins DESC, win_rate DESC " +
                "LIMIT 10";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n🏆 ===== LEADERBOARD ===== 🏆");
            System.out.println(String.format("%-20s %-8s %-8s %-10s", "Player", "Wins", "Losses", "Win Rate"));
            System.out.println("─".repeat(50));

            int rank = 1;
            while (rs.next()) {
                String name = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                double winRate = rs.getDouble("win_rate");

                String medal = "";
                if (rank == 1) medal = "🥇";
                else if (rank == 2) medal = "🥈";
                else if (rank == 3) medal = "🥉";
                else medal = rank + ".";

                System.out.println(String.format("%-3s %-20s %-8d %-8d %.1f%%",
                        medal, name, wins, losses, winRate));
                rank++;
            }
            System.out.println("─".repeat(50));

        } catch (SQLException e) {
            System.err.println("✗ Error displaying leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Clear all player data (for testing)
    public static void clearAllPlayers() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM matches");
                stmt.executeUpdate("DELETE FROM players");
                conn.commit();
                System.out.println("✓ All player data cleared");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("✗ Error clearing data: " + e.getMessage());
        }
    }

    // Test method
    public static void main(String[] args) {
        System.out.println("Testing Database Connection...\n");

        if (testConnection()) {
            showLeaderboard();
        }
    }
}