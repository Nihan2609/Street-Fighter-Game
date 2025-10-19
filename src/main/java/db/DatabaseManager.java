package db;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/street_fighter_game";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1122";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    public static int getPlayerId(String username) {
        String sql = "SELECT id FROM players WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("id");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void recordMatch(String winnerUsername, String loserUsername) {
        try (Connection conn = getConnection()) {
            int winnerId = getPlayerId(conn, winnerUsername);
            int loserId = getPlayerId(conn, loserUsername);

            if (winnerId == -1 || loserId == -1) {
                System.out.println(" Player not found.");
                return;
            }

            // Insert into matches table
            String insertMatch = "INSERT INTO matches (player1_id, player2_id, winner_id) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertMatch)) {
                stmt.setInt(1, winnerId);
                stmt.setInt(2, loserId);
                stmt.setInt(3, winnerId);
                stmt.executeUpdate();
            }


            updateStats(conn, winnerId, true);
            updateStats(conn, loserId, false);

            System.out.println(" Match recorded successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    private static void updateStats(Connection conn, int playerId, boolean won) throws SQLException {
        String sql = won
                ? "UPDATE players SET wins = wins + 1 WHERE id = ?"
                : "UPDATE players SET losses = losses + 1 WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
        }
    }



    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }


    public static boolean registerPlayer(String username, String password) {
        String sql = "INSERT INTO players (username, password) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password); 
            stmt.executeUpdate();
            System.out.println("Player registered successfully!");
            return true;

        } catch (SQLException e) {
            System.out.println("Error registering player.");
            e.printStackTrace();
            return  false;
        }
    }


    public static boolean loginPlayer(String username, String password) {
        String sql = "SELECT * FROM players WHERE username = ? AND password = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            return rs.next(); // 

        } catch (SQLException e) {
            System.out.println("Error during login.");
            e.printStackTrace();
            return false;
        }
    }


    public static void recordMatch(int player1Id, int player2Id, int winnerId) {
        String matchSql = "INSERT INTO matches (player1_id, player2_id, winner_id) VALUES (?, ?, ?)";
        String updateWinSql = "UPDATE players SET wins = wins + 1 WHERE id = ?";
        String updateLoseSql = "UPDATE players SET losses = losses + 1 WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement matchStmt = conn.prepareStatement(matchSql);
                    PreparedStatement winStmt = conn.prepareStatement(updateWinSql);
                    PreparedStatement loseStmt = conn.prepareStatement(updateLoseSql)
            ) {
                matchStmt.setInt(1, player1Id);
                matchStmt.setInt(2, player2Id);
                matchStmt.setInt(3, winnerId);
                matchStmt.executeUpdate();

                winStmt.setInt(1, winnerId);
                winStmt.executeUpdate();

                int loserId = (winnerId == player1Id) ? player2Id : player1Id;
                loseStmt.setInt(1, loserId);
                loseStmt.executeUpdate();

                conn.commit();
                System.out.println("Match recorded successfully!");
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Error recording match.");
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //  Show leaderboard
    public static void showLeaderboard() {
        String sql = "SELECT username, wins, losses FROM players ORDER BY wins DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n Leaderboard:");
            while (rs.next()) {
                String name = rs.getString("username");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                System.out.println("- " + name + " | Wins: " + wins + " | Losses: " + losses);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


//    public static void main(String[] args) {
//        try (Connection conn = getConnection()) {
//            System.out.println(" Connected to MySQL database successfully!");
//            showLeaderboard();
//        } catch (SQLException e) {
//            System.out.println("Connection failed!");
//            e.printStackTrace();
//        }
//    }


}
