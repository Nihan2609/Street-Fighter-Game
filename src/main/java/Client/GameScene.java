package Client;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.HashMap;

public class GameScene {

    @FXML private AnchorPane mainPane;
    @FXML private Canvas gameCanvas;
    @FXML private Rectangle p1HealthBar;
    @FXML private Rectangle p2HealthBar;
    @FXML private Label p1NameLabel;
    @FXML private Label p2NameLabel;
    @FXML private Label roundLabel;
    @FXML private Label statusLabel;

    private GraphicsContext gc;
    private AnimationTimer gameLoop;

    // Game state variables
    private String p1Choice;
    private String p2Choice;
    private String mapChoice;
    private Image mapImage; // The image for the map background

    private Player player1;
    private Player player2;

    private int round = 1;
    private int p1RoundsWon = 0;
    private int p2RoundsWon = 0;
    private boolean matchOver = false;

    // Player states and constants
    private final double PLAYER_WIDTH = 50;
    private final double PLAYER_HEIGHT = 100;
    private final double JUMP_STRENGTH = 10;
    private final double GRAVITY = 0.5;
    private final double ATTACK_RANGE = 70;
    private final double ATTACK_DAMAGE = 10;

    // Keyboard state
    private HashMap<KeyCode, Boolean> keys = new HashMap<>();

    // Mapping for map names to image file names
    private HashMap<String, String> mapFilePaths = new HashMap<>();

    @FXML
    public void initialize() {
        gc = gameCanvas.getGraphicsContext2D();

        // Add map names and corresponding image file paths
        mapFilePaths.put("Sunken Sanctuary", "/images/map1.gif");
        mapFilePaths.put("City Street", "/images/map2.gif");
        mapFilePaths.put("Hidden Dojo", "/images/map3.gif");
        mapFilePaths.put("Jungle Judgement", "/images/map4.gif");
        mapFilePaths.put("Night sky", "/images/map5.gif");
        mapFilePaths.put("Showdown", "/images/map6.png");

        // Handle key presses and releases
        mainPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(e -> keys.put(e.getCode(), true));
                newScene.setOnKeyReleased(e -> keys.put(e.getCode(), false));
            }
        });

        // Set up the game loop
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!matchOver) {
                    update();
                    draw();
                }
            }
        };
    }

    // This method is called from the previous controller to pass data
    public void startGame(String p1, String p2, String map) {
        this.p1Choice = p1;
        this.p2Choice = p2;
        this.mapChoice = map;

        System.out.println("Starting match: P1=" + p1 + ", P2=" + p2 + ", Map=" + map);

        // Load the map image based on the selected map
        loadMapImage();

        // Initialize players
        player1 = new Player(50, gameCanvas.getHeight() - PLAYER_HEIGHT, p1, Color.BLUE);
        player2 = new Player(gameCanvas.getWidth() - 50 - PLAYER_WIDTH, gameCanvas.getHeight() - PLAYER_HEIGHT, p2, Color.RED);

        p1NameLabel.setText(p1);
        p2NameLabel.setText(p2);

        startRound();
        gameLoop.start();
    }

    private void loadMapImage() {
        String mapFilePath = mapFilePaths.get(mapChoice);
        if (mapFilePath != null) {
            try {
                this.mapImage = new Image(getClass().getResource(mapFilePath).toExternalForm());
            } catch (Exception e) {
                System.err.println("Error loading map image: " + mapFilePath);
                e.printStackTrace();
            }
        }
    }

    private void startRound() {
        if (round > 3 || p1RoundsWon == 2 || p2RoundsWon == 2) {
            endMatch();
            return;
        }

        player1.reset();
        player2.reset();

        roundLabel.setText("ROUND " + round);
        statusLabel.setVisible(false);
        updateHealthBars();
    }

    private void endMatch() {
        matchOver = true;
        gameLoop.stop();
        statusLabel.setVisible(true);
        if (p1RoundsWon > p2RoundsWon) {
            statusLabel.setText(p1Choice + " Wins!");
        } else {
            statusLabel.setText(p2Choice + " Wins!");
        }
        System.out.println("Match Over!");
    }

    private void update() {
        // Player 1 input
        if (keys.getOrDefault(KeyCode.A, false)) {
            player1.moveX(-5);
        } else if (keys.getOrDefault(KeyCode.D, false)) {
            player1.moveX(5);
        }
        if (keys.getOrDefault(KeyCode.W, false)) {
            player1.jump();
        }
        if (keys.getOrDefault(KeyCode.S, false)) {
            if (player1.isAttacking()) {
                // If already attacking, do nothing
            } else {
                player1.startAttack();
                if (checkCollision(player1, player2)) {
                    player2.takeDamage(ATTACK_DAMAGE);
                }
            }
        } else {
            player1.stopAttack();
        }

        // Player 2 input
        if (keys.getOrDefault(KeyCode.LEFT, false)) {
            player2.moveX(-5);
        } else if (keys.getOrDefault(KeyCode.RIGHT, false)) {
            player2.moveX(5);
        }
        if (keys.getOrDefault(KeyCode.UP, false)) {
            player2.jump();
        }
        if (keys.getOrDefault(KeyCode.DOWN, false)) {
            if (player2.isAttacking()) {
                // Do nothing
            } else {
                player2.startAttack();
                if (checkCollision(player2, player1)) {
                    player1.takeDamage(ATTACK_DAMAGE);
                }
            }
        } else {
            player2.stopAttack();
        }

        // Update player physics
        player1.updatePhysics();
        player2.updatePhysics();

        // Boundary checks
        player1.clampPosition(0, gameCanvas.getWidth());
        player2.clampPosition(0, gameCanvas.getWidth());

        // Check for round win condition
        if (player1.getHealth() <= 0 || player2.getHealth() <= 0) {
            handleRoundEnd();
        }
    }

    private void handleRoundEnd() {
        if (player1.getHealth() <= 0) {
            p2RoundsWon++;
            statusLabel.setText(p2Choice + " KO!");
        } else {
            p1RoundsWon++;
            statusLabel.setText(p1Choice + " KO!");
        }

        round++;
        statusLabel.setVisible(true);

        // Wait for a bit before starting the next round
        gameLoop.stop();
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> startRound());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void draw() {
        // Clear canvas
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw the map image if it has been loaded
        if (mapImage != null) {
            gc.drawImage(mapImage, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        }

        // Draw players
        player1.draw(gc);
        player2.draw(gc);

        // Update health bars
        updateHealthBars();
    }

    private void updateHealthBars() {
        p1HealthBar.setWidth(player1.getHealth() * 2.5);
        p2HealthBar.setWidth(player2.getHealth() * 2.5);
        p1HealthBar.setFill(getHealthColor(player1.getHealth()));
        p2HealthBar.setFill(getHealthColor(player2.getHealth()));
    }

    private Color getHealthColor(double health) {
        if (health > 60) return Color.LIMEGREEN;
        if (health > 20) return Color.YELLOW;
        return Color.RED;
    }

    // Simple collision detection for attacks
    private boolean checkCollision(Player attacker, Player defender) {
        if (attacker.isAttacking() && attacker.getAttackFrame() > 0) {
            double distance = Math.sqrt(Math.pow(attacker.getX() - defender.getX(), 2) + Math.pow(attacker.getY() - defender.getY(), 2));
            return distance < ATTACK_RANGE;
        }
        return false;
    }

    // Inner class to manage player state
    private class Player {
        private double x, y;
        private double velX = 0, velY = 0;
        private double health = 100;
        private boolean isJumping = false;
        private String name;
        private Color color;
        private boolean isAttacking = false;
        private int attackFrame = 0;

        public Player(double x, double y, String name, Color color) {
            this.x = x;
            this.y = y;
            this.name = name;
            this.color = color;
        }

        public void reset() {
            this.health = 100;
            this.x = (color == Color.BLUE) ? 50 : gameCanvas.getWidth() - 50 - PLAYER_WIDTH;
            this.y = gameCanvas.getHeight() - PLAYER_HEIGHT;
            this.velX = 0;
            this.velY = 0;
            this.isJumping = false;
            this.isAttacking = false;
            this.attackFrame = 0;
        }

        public void moveX(double deltaX) {
            this.x += deltaX;
        }

        public void jump() {
            if (!isJumping) {
                this.velY = -JUMP_STRENGTH;
                this.isJumping = true;
            }
        }

        public void startAttack() {
            this.isAttacking = true;
            this.attackFrame = 1; // Start the attack animation/hitbox frame
        }

        public void stopAttack() {
            this.isAttacking = false;
            this.attackFrame = 0;
        }

        public boolean isAttacking() {
            return isAttacking;
        }

        public int getAttackFrame() {
            return attackFrame;
        }

        public void takeDamage(double damage) {
            this.health -= damage;
            if (this.health < 0) {
                this.health = 0;
            }
        }

        public void updatePhysics() {
            if (isJumping) {
                this.velY += GRAVITY;
                this.y += this.velY;
                if (this.y >= gameCanvas.getHeight() - PLAYER_HEIGHT) {
                    this.y = gameCanvas.getHeight() - PLAYER_HEIGHT;
                    this.velY = 0;
                    this.isJumping = false;
                }
            }
        }

        public void clampPosition(double minX, double maxX) {
            if (this.x < minX) this.x = minX;
            if (this.x + PLAYER_WIDTH > maxX) this.x = maxX - PLAYER_WIDTH;
        }

        public void draw(GraphicsContext gc) {
            gc.setFill(this.color);
            gc.fillRect(x, y, PLAYER_WIDTH, PLAYER_HEIGHT);

            // Draw a simple attack effect
            if (isAttacking) {
                gc.setFill(Color.YELLOW);
                gc.fillRect(x + PLAYER_WIDTH, y + PLAYER_HEIGHT/4, 20, 20);
            }
        }

        // Getters
        public double getX() { return x; }
        public double getY() { return y; }
        public double getHealth() { return health; }
    }
}
