package Client;

import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class GameScene {
    private Fighter player1, player2;
    private Map map;
    private Canvas canvas;
    private GraphicsContext gc;
    private Stage stage;
    private int p1Wins = 0, p2Wins = 0;
    private int round = 1;
    private boolean roundOver = false;

    public GameScene(Stage stage, String p1, String p2, String mapName) {
        this.stage = stage;
        canvas = new Canvas(960, 560);
        gc = canvas.getGraphicsContext2D();
        stage.setScene(new Scene(new StackPane(canvas)));

        // setup map
        switch (mapName) {
            case "Stage2" -> map = new Map("/images/HomeBg.jpg");
            default -> map = new Map("/images/charSelectBG.jpg");
        }

        // fighters
        player1 = p1.equals("Ken") ? new Ken(100, 350) : new Ryu(100, 350);
        player2 = p2.equals("Ken") ? new Ken(700, 350) : new Ryu(700, 350);

        stage.setTitle("Street Fighter - Round " + round);

        initInput();
        initLoop();
    }

    private void initInput() {
        stage.getScene().setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case A -> player1.moveLeft();
                case D -> player1.moveRight();
                case W -> player1.jump();
                case F -> player1.attack();

                case LEFT -> player2.moveLeft();
                case RIGHT -> player2.moveRight();
                case UP -> player2.jump();
                case L -> player2.attack();
            }
        });

        stage.getScene().setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case A, D -> player1.stop();
                case LEFT, RIGHT -> player2.stop();
            }
        });
    }

    private void initLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        }.start();
    }

    private void update() {
        if (roundOver) return;

        player1.update();
        player2.update();

        // simple collision detection
        if (player1.isAttacking() && Math.abs(player1.getX() - player2.getX()) < 80) {
            player2.takeDamage(10);
        }
        if (player2.isAttacking() && Math.abs(player1.getX() - player2.getX()) < 80) {
            player1.takeDamage(10);
        }

        if (!player1.isAlive() || !player2.isAlive()) {
            roundOver = true;
            if (!player1.isAlive()) p2Wins++;
            if (!player2.isAlive()) p1Wins++;

            if (p1Wins == 2 || p2Wins == 2) {
                stage.setTitle("Winner: " + (p1Wins == 2 ? "Player 1" : "Player 2"));
            } else {
                round++;
                resetRound();
            }
        }
    }

    private void resetRound() {
        player1 = new Ryu(100, 350);
        player2 = new Ken(700, 350);
        roundOver = false;
        stage.setTitle("Street Fighter - Round " + round);
    }

    private void render() {
        map.render(gc);

        // health bars
        gc.setFill(Color.RED);
        gc.fillRect(50, 50, player1.getHealth() * 2, 20);
        gc.fillRect(860 - player2.getHealth() * 2, 50, player2.getHealth() * 2, 20);

        // fighters
        player1.render(gc);
        player2.render(gc);
    }
}
