package Client;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class GameUI extends Application {

    private Rectangle player1, player2;
    private ProgressBar p1HealthBar, p2HealthBar;
    private double p1Health = 1.0, p2Health = 1.0;

    private final double PLAYER_WIDTH = 40;
    private final double PLAYER_HEIGHT = 80;
    private final double MOVE_SPEED = 5;

    private Set<KeyCode> pressedKeys = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setPrefSize(800, 400);

        // Create players
        player1 = new Rectangle(PLAYER_WIDTH, PLAYER_HEIGHT, Color.BLUE);
        player1.setLayoutX(100);
        player1.setLayoutY(300 - PLAYER_HEIGHT);

        player2 = new Rectangle(PLAYER_WIDTH, PLAYER_HEIGHT, Color.RED);
        player2.setLayoutX(600);
        player2.setLayoutY(300 - PLAYER_HEIGHT);

        // Health bars
        p1HealthBar = new ProgressBar(p1Health);
        p1HealthBar.setPrefWidth(150);
        p1HealthBar.setLayoutX(50);
        p1HealthBar.setLayoutY(20);

        p2HealthBar = new ProgressBar(p2Health);
        p2HealthBar.setPrefWidth(150);
        p2HealthBar.setLayoutX(600);
        p2HealthBar.setLayoutY(20);

        root.getChildren().addAll(player1, player2, p1HealthBar, p2HealthBar);

        Scene scene = new Scene(root);

        // Key press/release handling
        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastAttackTime = 0;

            @Override
            public void handle(long now) {
                // Player 1 movement (A/D)
                if (pressedKeys.contains(KeyCode.A)) {
                    player1.setLayoutX(Math.max(0, player1.getLayoutX() - MOVE_SPEED));
                }
                if (pressedKeys.contains(KeyCode.D)) {
                    player1.setLayoutX(Math.min(root.getWidth() - PLAYER_WIDTH, player1.getLayoutX() + MOVE_SPEED));
                }

                // Player 2 movement (LEFT/RIGHT)
                if (pressedKeys.contains(KeyCode.LEFT)) {
                    player2.setLayoutX(Math.max(0, player2.getLayoutX() - MOVE_SPEED));
                }
                if (pressedKeys.contains(KeyCode.RIGHT)) {
                    player2.setLayoutX(Math.min(root.getWidth() - PLAYER_WIDTH, player2.getLayoutX() + MOVE_SPEED));
                }

                // Player 1 punch (J)
                if (pressedKeys.contains(KeyCode.J) && now - lastAttackTime > 500_000_000) { // 0.5 sec cooldown
                    if (checkCollision(player1, player2)) {
                        p2Health -= 0.1;
                        if (p2Health < 0) p2Health = 0;
                        p2HealthBar.setProgress(p2Health);
                    }
                    lastAttackTime = now;
                }

                // Player 2 punch (NUMPAD1)
                if (pressedKeys.contains(KeyCode.NUMPAD1) && now - lastAttackTime > 500_000_000) {
                    if (checkCollision(player2, player1)) {
                        p1Health -= 0.1;
                        if (p1Health < 0) p1Health = 0;
                        p1HealthBar.setProgress(p1Health);
                    }
                    lastAttackTime = now;
                }

                // Check for game over
                if (p1Health <= 0) {
                    stop();
                    System.out.println("Player 2 Wins!");
                    // TODO: Show game over screen
                } else if (p2Health <= 0) {
                    stop();
                    System.out.println("Player 1 Wins!");
                    // TODO: Show game over screen
                }
            }
        };

        gameLoop.start();

        primaryStage.setTitle("Street Fighter");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Request focus to receive key events
        root.requestFocus();
    }

    private boolean checkCollision(Rectangle attacker, Rectangle defender) {
        // Simple bounding box collision with a small range to simulate punch reach
        double distance = Math.abs(attacker.getLayoutX() - defender.getLayoutX());
        return distance <= 60; // players must be close enough
    }

    public static void main(String[] args) {
        launch(args);
    }
}
