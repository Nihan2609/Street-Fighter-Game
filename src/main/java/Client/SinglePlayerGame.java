package Client;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.Group;

import java.util.HashSet;
import java.util.Set;

public class SinglePlayerGame {

    public static String username = "Player";

    public static void openSinglePlayer(String playerName) {
        username = playerName;
        Platform.runLater(() -> {
            new GameStage().show();
        });
    }

    private static class GameStage extends Stage {

        private Fighter player, bot;
        private ProgressBar playerHealthBar, botHealthBar;
        private double playerHealth = 1.0, botHealth = 1.0;
        private final double MOVE_SPEED = 5;
        private final double BOT_SPEED = 2.5;

        private final Set<KeyCode> pressedKeys = new HashSet<>();
        private AnimationTimer gameLoop;

        public GameStage() {
            ImageView background = new ImageView(new Image(getClass().getResourceAsStream("/images/Dream.gif")));
            background.setFitWidth(800);
            background.setFitHeight(400);

            Group root = new Group();
            root.getChildren().add(background);

            player = new Fighter("/images/player.gif", 100, 220, 80, 120, root);
            bot = new Fighter("/images/bot.gif", 600, 220, 80, 120, root);

            playerHealthBar = new ProgressBar(playerHealth);
            playerHealthBar.setPrefWidth(150);
            playerHealthBar.setLayoutX(50);
            playerHealthBar.setLayoutY(20);

            botHealthBar = new ProgressBar(botHealth);
            botHealthBar.setPrefWidth(150);
            botHealthBar.setLayoutX(600);
            botHealthBar.setLayoutY(20);

            Pane overlay = new Pane(playerHealthBar, botHealthBar);
            overlay.setPickOnBounds(false);

            StackPane container = new StackPane();
            container.getChildren().addAll(root, overlay);

            Scene scene = new Scene(container, 800, 400);
            scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
            scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));


            Sound.backMusic();

            gameLoop = new AnimationTimer() {
                private long lastAttackTime = 0;
                private long lastBotAttack = 0;

                @Override
                public void handle(long now) {
                    if (pressedKeys.contains(KeyCode.A)) {
                        player.moveLeft();
                    }
                    if (pressedKeys.contains(KeyCode.D)) {
                        player.moveRight();
                    }
                    if (pressedKeys.contains(KeyCode.SPACE)) {
                        player.jump();
                    }
                    player.applyGravity();

                    if (pressedKeys.contains(KeyCode.J) && now - lastAttackTime > 500_000_000) {
                        if (checkCollision(player, bot)) {
                            botHealth -= 0.1;
                            if (botHealth < 0) botHealth = 0;
                            botHealthBar.setProgress(botHealth);
                            Sound.punch();
                        }
                        lastAttackTime = now;
                    }

                    if (bot.getX() > player.getX()) {
                        bot.setX(bot.getX() - BOT_SPEED);
                    } else {
                        bot.setX(bot.getX() + BOT_SPEED);
                    }
                    bot.applyGravity();

                    if (now - lastBotAttack > 1_000_000_000) {
                        if (checkCollision(bot, player)) {
                            playerHealth -= 0.1;
                            if (playerHealth < 0) playerHealth = 0;
                            playerHealthBar.setProgress(playerHealth);
                            Sound.punch();
                        }
                        lastBotAttack = now;
                    }

                    if (playerHealth <= 0) {
                        endGame("Bot");
                    } else if (botHealth <= 0) {
                        endGame(username);
                    }
                }
            };

            gameLoop.start();
            this.setTitle("Single Player - Street Fighter");
            this.setScene(scene);
            container.requestFocus();

            this.setOnCloseRequest(e -> {
                Sound.stopBackMusic();
                if (gameLoop != null) gameLoop.stop();
            });
        }

        private boolean checkCollision(Fighter attacker, Fighter defender) {
            return Math.abs(attacker.getX() - defender.getX()) <= 60;
        }

        private void endGame(String winner) {
            if (gameLoop != null) gameLoop.stop();
            Sound.stopBackMusic();
            showGameOverDialog(winner);
        }

        private void showGameOverDialog(String winner) {
            Stage dialog = new Stage();
            dialog.setTitle("Game Over");

            Label label = new Label("Winner: " + winner);
            label.setStyle("-fx-font-size: 18px; -fx-text-fill: darkgreen;");
            StackPane pane = new StackPane(label);
            pane.setPrefSize(250, 120);
            pane.setAlignment(Pos.CENTER);

            dialog.setScene(new Scene(pane));
            dialog.show();
        }
    }
}

class Fighter {
    private ImageView body;
    private double dx = 0;
    private double dy = 0;
    private final double gravity = 1;
    private final double JUMP_FORCE = -15;
    private final double FLOOR_Y = 220;
    private final double SCREEN_WIDTH = 800; // screen width

    public Fighter(String imagePath, double x, double y, double width, double height, Group root) {
        Image image = new Image(getClass().getResourceAsStream(imagePath));
        body = new ImageView(image);
        body.setX(x);
        body.setY(y);
        body.setFitWidth(width);
        body.setFitHeight(height);
        body.setPreserveRatio(true);
        body.setSmooth(true);
        body.setCache(true);
        root.getChildren().add(body);
    }

    public void moveLeft() {
        dx = -5;
        body.setX(Math.max(0, body.getX() + dx)); // cannot go beyond left
    }

    public void moveRight() {
        dx = 5;
        body.setX(Math.min(SCREEN_WIDTH - body.getFitWidth(), body.getX() + dx)); // cannot go beyond right
    }

    public void jump() {
        if (onGround()) {
            dy = JUMP_FORCE;
        }
    }

    public void applyGravity() {
        if (!onGround() || dy < 0) {
            dy += gravity;
            body.setY(body.getY() + dy);

            if (body.getY() > FLOOR_Y) {
                body.setY(FLOOR_Y);
                dy = 0;
            }
        } else {
            dy = 0;
            body.setY(FLOOR_Y);
        }
    }

    public boolean onGround() {
        return body.getY() >= FLOOR_Y;
    }

    public ImageView getBody() {
        return body;
    }

    public double getX() {
        return body.getX();
    }

    public void setX(double x) {
        body.setX(Math.max(0, Math.min(SCREEN_WIDTH - body.getFitWidth(), x)));
    }

    public double getY() {
        return body.getY();
    }

    public void setY(double y) {
        body.setY(y);
    }
}
