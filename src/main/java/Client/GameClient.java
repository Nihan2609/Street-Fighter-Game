package Client;

import db.DatabaseManager;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class GameClient {

    private Stage stage;
    private Rectangle self, enemy;
    private Set<KeyCode> keysPressed = new HashSet<>();
    private BufferedWriter out;
    private BufferedReader in;
    private double selfHealth = 1.0;
    private double enemyHealth = 1.0;
    private ProgressBar selfBar, enemyBar;
    private Label statusLabel;
    public static String username = "Player" + (int) (Math.random() * 1000);

    public GameClient() {
        // default constructor
    }

    public void start(Stage stage, String username) {
        this.username = username;
        this.stage = stage;

        Pane root = new Pane();
        root.setPrefSize(800, 400);

        self = new Rectangle(40, 80, Color.BLUE);
        self.setLayoutX(100);
        self.setLayoutY(320);

        enemy = new Rectangle(40, 80, Color.RED);
        enemy.setLayoutX(600);
        enemy.setLayoutY(320);

        selfBar = new ProgressBar(selfHealth);
        selfBar.setLayoutX(50);
        selfBar.setLayoutY(20);
        selfBar.setPrefWidth(200);

        enemyBar = new ProgressBar(enemyHealth);
        enemyBar.setLayoutX(550);
        enemyBar.setLayoutY(20);
        enemyBar.setPrefWidth(200);

        Label nameLabel = new Label("You: " + username);
        nameLabel.setLayoutX(50);
        nameLabel.setLayoutY(0);

        statusLabel = new Label("");
        statusLabel.setTextFill(Color.ORANGE);
        statusLabel.setLayoutX(300);
        statusLabel.setLayoutY(200);

        root.getChildren().addAll(self, enemy, selfBar, enemyBar, nameLabel, statusLabel);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Street Fighter Multiplayer");
        stage.show();
        root.requestFocus();

        try {
            Socket socket = new Socket("localhost", 5555);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Unable to connect to server");
            return;
        }

        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (!keysPressed.contains(code)) {
                keysPressed.add(code);
                sendAction(code.name());
            }
        });
        scene.setOnKeyReleased(e -> keysPressed.remove(e.getCode()));

        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> handleOpponentAction(finalLine));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendAction(String action) {
        try {
            out.write(action);
            out.newLine();
            out.flush();

            switch (action) {
                case "LEFT":
                    self.setLayoutX(self.getLayoutX() - 10);
                    break;
                case "RIGHT":
                    self.setLayoutX(self.getLayoutX() + 10);
                    break;
                case "J":
                    attackEnemy();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleOpponentAction(String action) {
        switch (action) {
            case "LEFT":
                enemy.setLayoutX(enemy.getLayoutX() - 10);
                break;
            case "RIGHT":
                enemy.setLayoutX(enemy.getLayoutX() + 10);
                break;
            case "J":
                takeDamage();
                break;
        }
    }

    private void attackEnemy() {
        enemyHealth -= 0.1;
        if (enemyHealth < 0) enemyHealth = 0;
        enemyBar.setProgress(enemyHealth);
        checkWinner();
    }

    private void takeDamage() {
        selfHealth -= 0.1;
        if (selfHealth < 0) selfHealth = 0;
        selfBar.setProgress(selfHealth);
        checkWinner();
    }

    private void checkWinner() {
        if (selfHealth <= 0) {
            statusLabel.setText("You lost!");
            try {
                DatabaseManager.recordMatch("opponent", username); // opponent wins
            } catch (Exception ignored) {}
        } else if (enemyHealth <= 0) {
            statusLabel.setText("You won!");
            try {
                DatabaseManager.recordMatch(username, "opponent");
            } catch (Exception ignored) {}
        }
    }

    // Static helper to open multiplayer game from existing JavaFX thread (like from LoginUI)
    public static void openMultiplayer(String username) {
        Platform.runLater(() -> {
            GameClient gameClient = new GameClient();
            Stage stage = new Stage();
            gameClient.start(stage, username);
        });
    }
}
