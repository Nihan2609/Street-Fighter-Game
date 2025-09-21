package Client;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class GameSceneController {

    @FXML private Canvas gameCanvas;

    // UI Elements
    @FXML private Label roundLabel;
    @FXML private Label timerLabel;
    @FXML private Label player1NameLabel;
    @FXML private Label player2NameLabel;
    @FXML private Label player1HealthLabel;
    @FXML private Label player2HealthLabel;
    @FXML private ProgressBar player1HealthBar;
    @FXML private ProgressBar player2HealthBar;

    // Game State UI
    @FXML private VBox messageBox;
    @FXML private Label gameStateLabel;
    @FXML private Button continueButton;

    // Pause Menu
    @FXML private VBox pauseMenu;
    @FXML private Button resumeButton;
    @FXML private Button mainMenuButton;
    @FXML private VBox controlsInfo;

    // Game objects
    private Entity player1;
    private Entity player2;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;

    // Game data from MapSelectController
    private String selectedMapFile = "map1"; // default
    private String selectedPlayer1 = "RYU";
    private String selectedPlayer2 = "KEN";

    // Game state
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private GameState currentGameState = GameState.READY;
    private int roundTimer = 99;
    private long lastSecond = System.currentTimeMillis();
    private int currentRound = 1;
    private int player1Wins = 0;
    private int player2Wins = 0;

    // Input handling
    private final Set<KeyCode> pressedKeys = new HashSet<>();

    // Game states
    public enum GameState {
        READY, FIGHTING, ROUND_OVER, GAME_OVER, PAUSED
    }

    @FXML
    private void initialize() {
        // Initialize canvas
        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.setFocusTraversable(true);

        // Setup input handling
        setupInputHandling();

        // Initialize button actions
        continueButton.setOnAction(e -> nextRound());
        resumeButton.setOnAction(e -> resumeGame());
        mainMenuButton.setOnAction(e -> returnToMainMenu());

        // Initialize assets and fighters
        initializeGame();

        // Start game loop
        startGameLoop();
    }

    // Method to receive game data from MapSelectController
    public void setGameData(String player1Char, String player2Char, String mapFile) {
        this.selectedPlayer1 = player1Char != null ? player1Char.toUpperCase() : "RYU";
        this.selectedPlayer2 = player2Char != null ? player2Char.toUpperCase() : "KEN";
        this.selectedMapFile = mapFile != null ? mapFile : "map1";

        System.out.println("GameScene received data:");
        System.out.println("- Player 1: " + this.selectedPlayer1);
        System.out.println("- Player 2: " + this.selectedPlayer2);
        System.out.println("- Map: " + this.selectedMapFile);

        // Reinitialize the game with new data
        if (gc != null) {
            initializeGame();
        }
    }

    private void initializeGame() {
        // Initialize assets if not already done
        if (Assets.idle == null || Assets.idle[0] == null) {
            Assets.init();
        }

        // Create fighters based on selection
        player1 = createFighter(selectedPlayer1, 150, 450, true);
        player2 = createFighter(selectedPlayer2, 550, 450, false);

        // Update UI
        updateUI();

        // Show ready message
        showGameMessage("READY?", 2000);
    }

    // Helper method to create fighters based on character selection
    private Entity createFighter(String characterName, double x, double y, boolean isPlayer1) {
        switch (characterName) {
            case "RYU":
                return new Ryu(x, y);
            case "KEN":
                return new Ken(x, y);
            // Add more characters as you implement them
            // case "CHUN_LI":
            //     return new ChunLi(x, y);
            default:
                // Default fallback
                return isPlayer1 ? new Ryu(x, y) : new Ken(x, y);
        }
    }

    private void setupInputHandling() {
        gameCanvas.setOnKeyPressed(this::handleKeyPressed);
        gameCanvas.setOnKeyReleased(this::handleKeyReleased);

        gameCanvas.setFocusTraversable(true);
        gameCanvas.requestFocus();
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode key = event.getCode();
        pressedKeys.add(key);

        if (key == KeyCode.ESCAPE) {
            if (currentGameState == GameState.FIGHTING) {
                pauseGame();
            } else if (currentGameState == GameState.PAUSED) {
                resumeGame();
            }
        }

        if (key == KeyCode.H) {
            controlsInfo.setVisible(!controlsInfo.isVisible());
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        pressedKeys.remove(event.getCode());
    }

    private void processInput() {
        if (currentGameState != GameState.FIGHTING) return;

        // Cast to concrete types for accessing methods
        Ryu p1 = (player1 instanceof Ryu) ? (Ryu) player1 : null;
        Ken p2 = (player2 instanceof Ken) ? (Ken) player2 : null;
        Ryu p1AsRyu = (player1 instanceof Ryu) ? (Ryu) player1 : null;
        Ken p1AsKen = (player1 instanceof Ken) ? (Ken) player1 : null;
        Ryu p2AsRyu = (player2 instanceof Ryu) ? (Ryu) player2 : null;
        Ken p2AsKen = (player2 instanceof Ken) ? (Ken) player2 : null;

        // Player 1 Controls (A,W,S,D + J,K,L,U,I)
        boolean p1Moving = false;
        if (pressedKeys.contains(KeyCode.A)) {
            if (p1AsRyu != null) p1AsRyu.moveBackward();
            if (p1AsKen != null) p1AsKen.moveBackward();
            p1Moving = true;
        }
        if (pressedKeys.contains(KeyCode.D)) {
            if (p1AsRyu != null) p1AsRyu.moveForward();
            if (p1AsKen != null) p1AsKen.moveForward();
            p1Moving = true;
        }
        if (!p1Moving) {
            if (p1AsRyu != null) p1AsRyu.stopMoving();
            if (p1AsKen != null) p1AsKen.stopMoving();
        }

        if (pressedKeys.contains(KeyCode.W)) {
            if (p1AsRyu != null) p1AsRyu.jump();
            if (p1AsKen != null) p1AsKen.jump();
        }

        if (pressedKeys.contains(KeyCode.S)) {
            if (p1AsRyu != null) p1AsRyu.crouch();
            if (p1AsKen != null) p1AsKen.crouch();
        } else {
            if (p1AsRyu != null) p1AsRyu.stopCrouching();
            if (p1AsKen != null) p1AsKen.stopCrouching();
        }

        // Player 1 attacks
        if (pressedKeys.contains(KeyCode.J)) {
            if (p1AsRyu != null) p1AsRyu.punch();
            if (p1AsKen != null) p1AsKen.punch();
        }
        if (pressedKeys.contains(KeyCode.K)) {
            if (p1AsRyu != null) p1AsRyu.kick();
            if (p1AsKen != null) p1AsKen.kick();
        }
        if (pressedKeys.contains(KeyCode.U)) {
            if (p1AsRyu != null) p1AsRyu.quickPunch();
            if (p1AsKen != null) p1AsKen.quickPunch();
        }
        if (pressedKeys.contains(KeyCode.I)) {
            if (p1AsRyu != null) p1AsRyu.uppercut();
            if (p1AsKen != null) p1AsKen.uppercut();
        }
        if (pressedKeys.contains(KeyCode.L)) {
            if (p1AsRyu != null) p1AsRyu.block();
            if (p1AsKen != null) p1AsKen.block();
        } else {
            if (p1AsRyu != null) p1AsRyu.stopBlocking();
            if (p1AsKen != null) p1AsKen.stopBlocking();
        }

        // Player 2 Controls (Arrow keys + 1,2,3,4,5)
        boolean p2Moving = false;
        if (pressedKeys.contains(KeyCode.LEFT)) {
            if (p2AsRyu != null) p2AsRyu.moveForward();
            if (p2AsKen != null) p2AsKen.moveForward();
            p2Moving = true;
        }
        if (pressedKeys.contains(KeyCode.RIGHT)) {
            if (p2AsRyu != null) p2AsRyu.moveBackward();
            if (p2AsKen != null) p2AsKen.moveBackward();
            p2Moving = true;
        }
        if (!p2Moving) {
            if (p2AsRyu != null) p2AsRyu.stopMoving();
            if (p2AsKen != null) p2AsKen.stopMoving();
        }

        if (pressedKeys.contains(KeyCode.UP)) {
            if (p2AsRyu != null) p2AsRyu.jump();
            if (p2AsKen != null) p2AsKen.jump();
        }

        if (pressedKeys.contains(KeyCode.DOWN)) {
            if (p2AsRyu != null) p2AsRyu.crouch();
            if (p2AsKen != null) p2AsKen.crouch();
        } else {
            if (p2AsRyu != null) p2AsRyu.stopCrouching();
            if (p2AsKen != null) p2AsKen.stopCrouching();
        }

        // Player 2 attacks
        if (pressedKeys.contains(KeyCode.DIGIT1)) {
            if (p2AsRyu != null) p2AsRyu.punch();
            if (p2AsKen != null) p2AsKen.punch();
        }
        if (pressedKeys.contains(KeyCode.DIGIT2)) {
            if (p2AsRyu != null) p2AsRyu.kick();
            if (p2AsKen != null) p2AsKen.kick();
        }
        if (pressedKeys.contains(KeyCode.DIGIT4)) {
            if (p2AsRyu != null) p2AsRyu.quickPunch();
            if (p2AsKen != null) p2AsKen.quickPunch();
        }
        if (pressedKeys.contains(KeyCode.DIGIT5)) {
            if (p2AsRyu != null) p2AsRyu.uppercut();
            if (p2AsKen != null) p2AsKen.uppercut();
        }
        if (pressedKeys.contains(KeyCode.DIGIT3)) {
            if (p2AsRyu != null) p2AsRyu.block();
            if (p2AsKen != null) p2AsKen.block();
        } else {
            if (p2AsRyu != null) p2AsRyu.stopBlocking();
            if (p2AsKen != null) p2AsKen.stopBlocking();
        }
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gamePaused) {
                    update();
                    render();
                }
            }
        };
        gameLoop.start();
    }

    private void update() {
        processInput();
        updateTimer();

        // Update fighters
        player1.tick();
        player2.tick();

        // Make fighters face each other
        if (player1 instanceof Ryu) ((Ryu) player1).faceOpponent(player2);
        if (player1 instanceof Ken) ((Ken) player1).faceOpponent(player2);
        if (player2 instanceof Ryu) ((Ryu) player2).faceOpponent(player1);
        if (player2 instanceof Ken) ((Ken) player2).faceOpponent(player1);

        // Check combat
        checkCombat();

        // Check win conditions
        checkWinConditions();

        // Update UI
        updateUI();
    }

    private void updateTimer() {
        if (currentGameState == GameState.FIGHTING) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSecond >= 1000) {
                roundTimer--;
                lastSecond = currentTime;

                if (roundTimer <= 0) {
                    endRoundByTime();
                }
            }
        }
    }

    private void checkCombat() {
        // Check if player1 is hitting player2
        boolean p1Hitting = false;
        boolean p2Blocking = false;

        if (player1 instanceof Ryu && ((Ryu) player1).isAttackHitting(player2)) {
            p1Hitting = true;
        }
        if (player1 instanceof Ken && ((Ken) player1).isAttackHitting(player2)) {
            p1Hitting = true;
        }

        if (player2 instanceof Ryu && ((Ryu) player2).getCurrentState() == 17) {
            p2Blocking = true;
        }
        if (player2 instanceof Ken && ((Ken) player2).getCurrentState() == 17) {
            p2Blocking = true;
        }

        if (p1Hitting && !p2Blocking) {
            int damage = getDamageForState(getPlayerCurrentState(player1));
            if (player2 instanceof Ryu) ((Ryu) player2).takeDamage(damage);
            if (player2 instanceof Ken) ((Ken) player2).takeDamage(damage);
        }

        // Check if player2 is hitting player1
        boolean p2Hitting = false;
        boolean p1Blocking = false;

        if (player2 instanceof Ryu && ((Ryu) player2).isAttackHitting(player1)) {
            p2Hitting = true;
        }
        if (player2 instanceof Ken && ((Ken) player2).isAttackHitting(player1)) {
            p2Hitting = true;
        }

        if (player1 instanceof Ryu && ((Ryu) player1).getCurrentState() == 17) {
            p1Blocking = true;
        }
        if (player1 instanceof Ken && ((Ken) player1).getCurrentState() == 17) {
            p1Blocking = true;
        }

        if (p2Hitting && !p1Blocking) {
            int damage = getDamageForState(getPlayerCurrentState(player2));
            if (player1 instanceof Ryu) ((Ryu) player1).takeDamage(damage);
            if (player1 instanceof Ken) ((Ken) player1).takeDamage(damage);
        }
    }

    private int getPlayerCurrentState(Entity player) {
        if (player instanceof Ryu) return ((Ryu) player).getCurrentState();
        if (player instanceof Ken) return ((Ken) player).getCurrentState();
        return 0; // IDLING
    }

    private int getDamageForState(int attackState) {
        switch (attackState) {
            case 5: // ATTACKING_H (quick punch)
                return 8;
            case 4: // ATTACKING_G (punch)
            case 8: // ATTACKING_C_G (crouch punch)
                return 12;
            case 7: // ATTACKING_N (kick)
                return 15;
            case 6: // ATTACKING_B (uppercut)
                return 20;
            case 12: // ATTACKING_A_G (air punch)
                return 10;
            case 14: // ATTACKING_A_B (air kick)
                return 18;
            default:
                return 5;
        }
    }

    private void checkWinConditions() {
        if (currentGameState != GameState.FIGHTING) return;

        boolean p1Dead = false;
        boolean p2Dead = false;

        if (player1 instanceof Ryu) p1Dead = ((Ryu) player1).isDead();
        if (player1 instanceof Ken) p1Dead = ((Ken) player1).isDead();
        if (player2 instanceof Ryu) p2Dead = ((Ryu) player2).isDead();
        if (player2 instanceof Ken) p2Dead = ((Ken) player2).isDead();

        if (p1Dead || p2Dead) {
            if (p2Dead) {
                player1Wins++;
                showGameMessage("PLAYER 1 WINS!", 3000);
            } else {
                player2Wins++;
                showGameMessage("PLAYER 2 WINS!", 3000);
            }
            endRound();
        }
    }

    private void endRoundByTime() {
        int p1Health = 0;
        int p2Health = 0;

        if (player1 instanceof Ryu) p1Health = ((Ryu) player1).getHealth();
        if (player1 instanceof Ken) p1Health = ((Ken) player1).getHealth();
        if (player2 instanceof Ryu) p2Health = ((Ryu) player2).getHealth();
        if (player2 instanceof Ken) p2Health = ((Ken) player2).getHealth();

        if (p1Health > p2Health) {
            player1Wins++;
            showGameMessage("TIME UP! PLAYER 1 WINS!", 3000);
        } else if (p2Health > p1Health) {
            player2Wins++;
            showGameMessage("TIME UP! PLAYER 2 WINS!", 3000);
        } else {
            showGameMessage("TIME UP! DRAW!", 3000);
        }
        endRound();
    }

    private void endRound() {
        currentGameState = GameState.ROUND_OVER;

        if (player1Wins >= 2 || player2Wins >= 2) {
            currentGameState = GameState.GAME_OVER;
            String winner = player1Wins >= 2 ? "PLAYER 1" : "PLAYER 2";
            showGameMessage(winner + " WINS THE MATCH!", 5000);
            continueButton.setText("New Game");
        } else {
            continueButton.setText("Next Round");
            continueButton.setVisible(true);
        }
    }

    private void nextRound() {
        if (currentGameState == GameState.GAME_OVER) {
            player1Wins = 0;
            player2Wins = 0;
            currentRound = 1;
        } else {
            currentRound++;
        }

        initializeGame();
        roundTimer = 99;
        currentGameState = GameState.FIGHTING;
        messageBox.setVisible(false);
        continueButton.setVisible(false);
    }

    private void render() {
        // Clear canvas
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw background based on selected map
        drawMapBackground();

        // Draw ground
        gc.setFill(Color.BROWN);
        gc.fillRect(0, 450, gameCanvas.getWidth(), 150);

        // Draw fighters
        player1.render(gc);
        player2.render(gc);
    }

    private void drawMapBackground() {
        try {
            // Try to load the selected map background
            Image bgImage = ImageLoader.loadImage("/images/" + selectedMapFile + ".jpg");
            if (bgImage == null) {
                // Try .gif extension if .jpg doesn't exist
                bgImage = ImageLoader.loadImage("/images/" + selectedMapFile + ".gif");
            }
            if (bgImage == null) {
                // Try .png extension
                bgImage = ImageLoader.loadImage("/images/" + selectedMapFile + ".png");
            }

            if (bgImage != null) {
                // Draw image to fill entire canvas, stretching if necessary
                gc.drawImage(bgImage, 0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
                return;
            }
        } catch (Exception e) {
            System.err.println("Could not load map background: " + selectedMapFile);
            e.printStackTrace();
        }

        // Fallback to colored background
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
    }

    private void updateUI() {
        // Get health values safely
        int p1Health = 0, p1MaxHealth = 100;
        int p2Health = 0, p2MaxHealth = 100;
        String p1Name = selectedPlayer1;
        String p2Name = selectedPlayer2;

        if (player1 instanceof Ryu) {
            Ryu ryu = (Ryu) player1;
            p1Health = ryu.getHealth();
            p1MaxHealth = ryu.getMaxHealth();
            p1Name = ryu.getName();
        }
        if (player1 instanceof Ken) {
            Ken ken = (Ken) player1;
            p1Health = ken.getHealth();
            p1MaxHealth = ken.getMaxHealth();
            p1Name = ken.getName();
        }

        if (player2 instanceof Ryu) {
            Ryu ryu = (Ryu) player2;
            p2Health = ryu.getHealth();
            p2MaxHealth = ryu.getMaxHealth();
            p2Name = ryu.getName();
        }
        if (player2 instanceof Ken) {
            Ken ken = (Ken) player2;
            p2Health = ken.getHealth();
            p2MaxHealth = ken.getMaxHealth();
            p2Name = ken.getName();
        }

        // Update health bars
        double p1HealthPercent = (double) p1Health / p1MaxHealth;
        double p2HealthPercent = (double) p2Health / p2MaxHealth;

        player1HealthBar.setProgress(p1HealthPercent);
        player2HealthBar.setProgress(p2HealthPercent);

        player1HealthLabel.setText(p1Health + "/" + p1MaxHealth);
        player2HealthLabel.setText(p2Health + "/" + p2MaxHealth);

        // Update timer
        timerLabel.setText(String.valueOf(roundTimer));

        // Update round info
        roundLabel.setText("ROUND " + currentRound);

        // Update player names with win count
        player1NameLabel.setText(p1Name + " (Wins: " + player1Wins + ")");
        player2NameLabel.setText(p2Name + " (Wins: " + player2Wins + ")");
    }

    private void showGameMessage(String message, long duration) {
        gameStateLabel.setText(message);
        messageBox.setVisible(true);

        // Auto-hide message after duration and start fighting
        new Thread(() -> {
            try {
                Thread.sleep(duration);
                if (currentGameState == GameState.READY) {
                    currentGameState = GameState.FIGHTING;
                    javafx.application.Platform.runLater(() -> messageBox.setVisible(false));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void pauseGame() {
        if (currentGameState == GameState.FIGHTING) {
            currentGameState = GameState.PAUSED;
            gamePaused = true;
            pauseMenu.setVisible(true);
        }
    }

    private void resumeGame() {
        if (currentGameState == GameState.PAUSED) {
            currentGameState = GameState.FIGHTING;
            gamePaused = false;
            pauseMenu.setVisible(false);
            gameCanvas.requestFocus();
        }
    }

    private void returnToMainMenu() {
        try {
            if (gameLoop != null) {
                gameLoop.stop();
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/game/HomeUI.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) gameCanvas.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Street Fighter - Home");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}