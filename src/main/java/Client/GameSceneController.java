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
import java.io.InputStream;

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

    // Game systems
    private Fighter player1;
    private Fighter player2;
    private InputManager inputManager;
    private AssetManager assetManager;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private Image backgroundImage;

    // Game data
    private String selectedMapFile = "map1";
    private String selectedPlayer1 = "RYU";
    private String selectedPlayer2 = "KEN";

    // Game state
    private GameState currentGameState = GameState.READY;
    private int roundTimer = 99;
    private long lastSecond = System.currentTimeMillis();
    private int currentRound = 1;
    private int player1Wins = 0;
    private int player2Wins = 0;

    public enum GameState {
        READY, FIGHTING, ROUND_OVER, GAME_OVER, PAUSED
    }

    @FXML
    private void initialize() {
        // Initialize canvas
        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.setFocusTraversable(true);
        gameCanvas.setWidth(800);
        gameCanvas.setHeight(400);

        // Initialize systems
        assetManager = AssetManager.getInstance();
        inputManager = InputManager.getInstance();

        // Setup input handling
        setupInputHandling();

        // Initialize button actions
        continueButton.setOnAction(e -> nextRound());
        resumeButton.setOnAction(e -> resumeGame());
        mainMenuButton.setOnAction(e -> returnToMainMenu());

        // Hide controls info by default
        if (controlsInfo != null) {
            controlsInfo.setVisible(false);
        }

        AudioManager.playBGM("fight_theme.wav");
    }

    public void setGameData(String player1Char, String player2Char, String mapFile) {
        this.selectedPlayer1 = player1Char != null ? player1Char.toUpperCase() : "RYU";
        this.selectedPlayer2 = player2Char != null ? player2Char.toUpperCase() : "KEN";
        this.selectedMapFile = mapFile != null ? mapFile : "map1";


        loadBackgroundImage();

        if (gc != null) {
            initializeGame();
            startGameLoop();
        }
    }

    private void loadBackgroundImage() {
        try {
            InputStream stream = getClass().getResourceAsStream("/images/" + selectedMapFile + ".gif");
            if (stream != null) {
                backgroundImage = new Image(stream);
                if (!backgroundImage.isError()) {
                    stream.close();
                    return;
                }
                stream.close();
            }

            backgroundImage = new Image("/images/" + selectedMapFile + ".gif");
            if (backgroundImage.isError()) {
                backgroundImage = null;
            }

        } catch (Exception e) {
            backgroundImage = null;
        }
    }

    private void initializeGame() {
        player1 = new Fighter(selectedPlayer1, 150, 270, "P1", true);
        player2 = new Fighter(selectedPlayer2, 550, 270, "P2", false);

        roundTimer = 99;
        currentGameState = GameState.READY;
        lastSecond = System.currentTimeMillis();

        updateUI();
        showGameMessage("READY? FIGHT!", 2000);
    }

    private void setupInputHandling() {
        gameCanvas.setOnKeyPressed(this::handleKeyPressed);
        gameCanvas.setOnKeyReleased(this::handleKeyReleased);
        gameCanvas.requestFocus();
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode key = event.getCode();
        inputManager.handleKeyPressed(key);

        if (key == KeyCode.ESCAPE) {
            if (currentGameState == GameState.FIGHTING) {
                pauseGame();
            } else if (currentGameState == GameState.PAUSED) {
                resumeGame();
            }
        }

        if (key == KeyCode.M) {
            controlsInfo.setVisible(!controlsInfo.isVisible());
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        inputManager.handleKeyReleased(event.getCode());
    }

    private void startGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop(); // Stop existing loop if any
        }

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (currentGameState != GameState.PAUSED) {
                    update();
                    render();
                }
            }
        };
        gameLoop.start();
    }

    private void update() {
        updateTimer();

        if (currentGameState == GameState.FIGHTING) {
            // Update fighters
            player1.tick();
            player2.tick();

            // Make fighters face each other
            player1.faceOpponent(player2);
            player2.faceOpponent(player1);

            checkCombat();

            // Check win conditions
            checkWinConditions();
        }

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
        // Player 1 hitting Player 2
        if (player1.isAttacking() && player2.canBeHit(player1)) {
            CombatSystem.AttackData attackData = CombatSystem.getAttackData(player1.getCurrentAnimation());
            if (attackData != null) {
                player2.takeDamage(attackData.damage, player1.getCurrentAnimation());
            }
        }

        // Player 2 hitting Player 1
        if (player2.isAttacking() && player1.canBeHit(player2)) {
            CombatSystem.AttackData attackData = CombatSystem.getAttackData(player2.getCurrentAnimation());
            if (attackData != null) {
                player1.takeDamage(attackData.damage, player2.getCurrentAnimation());
            }
        }
    }

    private void checkWinConditions() {
        if (player1.isDead() || player2.isDead()) {
            if (player2.isDead()) {
                player1Wins++;
                player1.performWin();
                showGameMessage(selectedPlayer1 + " WINS!", 3000);
            } else {
                player2Wins++;
                player2.performWin();
                showGameMessage(selectedPlayer2 + " WINS!", 3000);
            }
            endRound();
        }
    }

    private void endRoundByTime() {
        if (player1.getHealth() > player2.getHealth()) {
            player1Wins++;
            showGameMessage("TIME UP! " + selectedPlayer1 + " WINS!", 3000);
        } else if (player2.getHealth() > player1.getHealth()) {
            player2Wins++;
            showGameMessage("TIME UP! " + selectedPlayer2 + " WINS!", 3000);
        } else {
            showGameMessage("TIME UP! DRAW!", 3000);
        }
        endRound();
    }

    private void endRound() {
        currentGameState = GameState.ROUND_OVER;

        if (player1Wins >= 2 || player2Wins >= 2) {
            currentGameState = GameState.GAME_OVER;
            String winner = player1Wins >= 2 ? selectedPlayer1 : selectedPlayer2;
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
        messageBox.setVisible(false);
        continueButton.setVisible(false);
    }

    private void render() {
        // Clear canvas
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        // Draw background
        drawMapBackground();

        // Draw fighters
        player1.render(gc);
        player2.render(gc);
    }

    private void drawMapBackground() {
        if (backgroundImage != null && !backgroundImage.isError()) {
            // Draw background image stretched to fill entire 800x400 canvas
            gc.drawImage(backgroundImage, 0, 0, 800, 400);
        } else {
            // Fallback background - gradient sky and ground
            gc.setFill(Color.LIGHTBLUE);
            gc.fillRect(0, 0, 800, 320);

            // Ground
            gc.setFill(Color.BROWN);
            gc.fillRect(0, 320, 800, 80);
        }
    }

    private void updateUI() {
        // Update health bars
        double p1HealthPercent = (double) player1.getHealth() / player1.getMaxHealth();
        double p2HealthPercent = (double) player2.getHealth() / player2.getMaxHealth();

        player1HealthBar.setProgress(p1HealthPercent);
        player2HealthBar.setProgress(p2HealthPercent);

        player1HealthLabel.setText(player1.getHealth() + "/" + player1.getMaxHealth());
        player2HealthLabel.setText(player2.getHealth() + "/" + player2.getMaxHealth());

        // Update timer
        timerLabel.setText(String.valueOf(roundTimer));

        // Update round info
        roundLabel.setText("ROUND " + currentRound);

        // Update player names with win count
        player1NameLabel.setText(player1.getName() + " (Wins: " + player1Wins + ")");
        player2NameLabel.setText(player2.getName() + " (Wins: " + player2Wins + ")");
    }

    private void showGameMessage(String message, long duration) {
        gameStateLabel.setText(message);
        messageBox.setVisible(true);

        // Auto-hide message and start fighting
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
            pauseMenu.setVisible(true);
        }
    }

    private void resumeGame() {
        if (currentGameState == GameState.PAUSED) {
            currentGameState = GameState.FIGHTING;
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
            stage.setTitle("Street Fighter");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}