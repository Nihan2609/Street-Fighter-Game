package Client;

import javafx.beans.property.*;

public class PlayerRecord {
    private final StringProperty username = new SimpleStringProperty();
    private final IntegerProperty wins = new SimpleIntegerProperty();
    private final IntegerProperty losses = new SimpleIntegerProperty();
    private final DoubleProperty winRate = new SimpleDoubleProperty();

    public PlayerRecord(String u, int w, int l, double r) {
        username.set(u);
        wins.set(w);
        losses.set(l);
        winRate.set(r);
    }

    public StringProperty usernameProperty() { return username; }
    public IntegerProperty winsProperty() { return wins; }
    public IntegerProperty lossesProperty() { return losses; }
    public DoubleProperty winRateProperty() { return winRate; }
}
