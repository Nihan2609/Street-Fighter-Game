package Client;

public class PlayerRecord {
    private String username;
    private int wins;
    private int losses;
    private double winRate;

    public PlayerRecord(String username, int wins, int losses, double winRate) {
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
