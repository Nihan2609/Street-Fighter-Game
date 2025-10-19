package Client;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.InputStream;

public class GameSceneController {

    @FXML private Canvas gameCanvas;
    @FXML private Label roundLabel;
    @FXML private Label timerLabel;
    @FXML private ImageView player1NameImage;
    @FXML private ImageView player2NameImage;
    @FXML private Label player1HealthLabel;
    @FXML private Label player2HealthLabel;
    @FXML private ProgressBar player1HealthBar;
    @FXML private ProgressBar player2HealthBar;
    @FXML private VBox messageBox;
    @FXML private Label gameStateLabel;
    @FXML private Button continueButton;
    @FXML private VBox pauseMenu;
    @FXML private Button resumeButton;
    @FXML private Button mainMenuButton;
    @FXML private VBox controlsInfo;
    @FXML private Label networkStatusLabel;
    @FXML private Label pausedByLabel;

    private Fighter player1;
    private Fighter player2;
    private InputManager inputManager;
    private AssetManager assetManager;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private Image backgroundImage;

    private String selectedMapFile = "map1";
    private String selectedPlayer1 = "RYU";
    private String selectedPlayer2 = "KEN";

    private GameState currentGameState = GameState.READY;
    private int roundTimer = 99;
    private long lastSecond = System.currentTimeMillis();
    private int currentRound = 1;
    private int player1Wins = 0;
    private int player2Wins = 0;
    private long lastFPSCheck = 0;
    private int frameCount = 0;
    private int currentFPS = 0;
    private boolean showFPS = true;

    private NetworkClient networkClient = null;
    private String localPlayerId = null;
    private boolean isNetworkMode = false;
    private boolean isHost = false;
    private int stateUpdateCounter = 0;

    private FontManager fontManager = FontManager.getInstance();

    // Character name images
    private Image ryuNameImage;
    private Image kenNameImage;

    public enum GameState {
        READY, FIGHTING, ROUND_OVER, GAME_OVER, PAUSED
    }

    @FXML
    private void initialize() {
        fontManager.initialize();

        gc = gameCanvas.getGraphicsContext2D();
        gameCanvas.setFocusTraversable(true);
        gameCanvas.setWidth(800);
        gameCanvas.setHeight(400);

        assetManager = AssetManager.getInstance();
        inputManager = InputManager.getInstance();

        loadCharacterNameImages();
        applyCustomFont();
        setupInputHandling();

        if (continueButton != null) {
            continueButton.setOnAction(e -> handleContinueButton());
            continueButton.setStyle(fontManager.getStyleString(12) +
                    "-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10;");
        }
        if (resumeButton != null) {
            resumeButton.setOnAction(e -> resumeGame());
            resumeButton.setStyle(fontManager.getStyleString(12) +
                    "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10;");
        }
        if (mainMenuButton != null) {
            mainMenuButton.setOnAction(e -> returnToMainMenu());
            mainMenuButton.setStyle(fontManager.getStyleString(12) +
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 10;");
        }

        if (controlsInfo != null) controlsInfo.setVisible(false);
        if (pausedByLabel != null) pausedByLabel.setVisible(false);

        AudioManager.playBGM("fight_theme.wav");
    }

    private void loadCharacterNameImages() {
        try {
            InputStream ryuStream = getClass().getResourceAsStream("/images/ryu_name.png");
            if (ryuStream != null) {
                ryuNameImage = new Image(ryuStream);
                ryuStream.close();
            }

            InputStream kenStream = getClass().getResourceAsStream("/images/ken_name.png");
            if (kenStream != null) {
                kenNameImage = new Image(kenStream);
                kenStream.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading character name images: " + e.getMessage());
        }
    }

    private void applyCustomFont() {
        if (roundLabel != null) roundLabel.setStyle(fontManager.getStyleString(16, "white"));
        if (timerLabel != null) timerLabel.setStyle(fontManager.getStyleString(20, "yellow"));
        if (player1HealthLabel != null) player1HealthLabel.setStyle(fontManager.getStyleString(10, "white"));
        if (player2HealthLabel != null) player2HealthLabel.setStyle(fontManager.getStyleString(10, "white"));
        if (gameStateLabel != null) gameStateLabel.setStyle(fontManager.getStyleString(28, "yellow"));
        if (networkStatusLabel != null) networkStatusLabel.setStyle(fontManager.getStyleString(9, "lime"));
        if (pausedByLabel != null) pausedByLabel.setStyle(fontManager.getStyleString(10, "yellow"));
    }

    public void setNetworkMode(NetworkClient client, String playerId) {
        this.networkClient = client;
        this.localPlayerId = playerId;
        this.isNetworkMode = true;
        this.isHost = client.isHost();

        System.out.println("Game initialized in network mode: " +
                (isHost ? "HOST" : "CLIENT") + " (" + playerId + ")");

        if (networkClient != null) {
            networkClient.setCallback(new NetworkClient.NetworkCallback() {
                @Override
                public void onConnected() {
                    Platform.runLater(() -> updateNetworkStatus("Connected"));
                }

                @Override
                public void onDisconnected() {
                    Platform.runLater(() -> {
                        updateNetworkStatus("Disconnected!");
                        pauseGame();
                        gameStateLabel.setText("Connection Lost!");
                    });
                }

                @Override
                public void onGameStart() {
                    Platform.runLater(() -> {
                        currentGameState = GameState.READY;
                        showGameMessage("READY? FIGHT!", 2000);
                    });
                }

                @Override
                public void onInputReceived(String playerId, long frameNumber, short inputBits) {
                    String remotePlayerId = playerId.equals(localPlayerId) ? null : playerId;
                    if (remotePlayerId != null) {
                        inputManager.setPlayerNetworkControlled(remotePlayerId, true);
                        NetworkClient.InputPacker.applyInputs(inputBits, inputManager, remotePlayerId);
                    }
                }

                @Override
                public void onPlayerDisconnected(String playerId) {
                    Platform.runLater(() -> {
                        updateNetworkStatus("Opponent left");
                        showGameMessage("OPPONENT LEFT", 5000);
                        currentGameState = GameState.GAME_OVER;
                    });
                }

                @Override
                public void onGameConfig(String p1Char, String p2Char, String mapFile) {
                }

                @Override
                public void onPauseGame(String pausedBy) {
                    Platform.runLater(() -> {
                        if (currentGameState == GameState.FIGHTING) {
                            currentGameState = GameState.PAUSED;
                            if (pauseMenu != null) pauseMenu.setVisible(true);
                            if (pausedByLabel != null) {
                                pausedByLabel.setText("Paused by: " + pausedBy);
                                pausedByLabel.setVisible(true);
                            }
                        }
                    });
                }

                @Override
                public void onResumeGame() {
                    Platform.runLater(() -> {
                        if (currentGameState == GameState.PAUSED) {
                            currentGameState = GameState.FIGHTING;
                            if (pauseMenu != null) pauseMenu.setVisible(false);
                            if (pausedByLabel != null) pausedByLabel.setVisible(false);
                            gameCanvas.requestFocus();
                        }
                    });
                }

                @Override
                public void onRematchRequest() {
                    Platform.runLater(() -> {
                        System.out.println("Received rematch request - resetting to Round 1");
                        startRematch();
                    });
                }

                @Override
                public void onNextRound(int round, int p1Wins, int p2Wins) {
                    Platform.runLater(() -> {
                        System.out.println("Received next round: Round " + round + " (P1:" + p1Wins + " P2:" + p2Wins + ")");
                        handleNextRound(round, p1Wins, p2Wins);
                    });
                }

                @Override
                public void onWaitingForHost() {
                    Platform.runLater(() -> System.out.println("Client waiting for host"));
                }
            });

            if (localPlayerId.equals("P1")) {
                inputManager.setPlayerNetworkControlled("P1", false);
                inputManager.setPlayerNetworkControlled("P2", true);
            } else {
                inputManager.setPlayerNetworkControlled("P1", true);
                inputManager.setPlayerNetworkControlled("P2", false);
            }

            updateNetworkStatus("Connected " + (isHost ? "HOST" : "CLIENT"));
        }
    }

    public void setGameData(String player1Char, String player2Char, String mapFile) {
        this.selectedPlayer1 = player1Char != null ? player1Char.toUpperCase() : "RYU";
        this.selectedPlayer2 = player2Char != null ? player2Char.toUpperCase() : "KEN";
        this.selectedMapFile = mapFile != null ? mapFile : "map1";

        loadBackgroundImage();
        updateCharacterNameImages();

        if (gc != null) {
            initializeGame();
            startGameLoop();
        }
    }

    private void updateCharacterNameImages() {
        if (player1NameImage != null) {
            if (selectedPlayer1.equals("RYU") && ryuNameImage != null) {
                player1NameImage.setImage(ryuNameImage);
                player1NameImage.setVisible(true);
            } else if (selectedPlayer1.equals("KEN") && kenNameImage != null) {
                player1NameImage.setImage(kenNameImage);
                player1NameImage.setVisible(true);
            }
        }

        if (player2NameImage != null) {
            if (selectedPlayer2.equals("RYU") && ryuNameImage != null) {
                player2NameImage.setImage(ryuNameImage);
                player2NameImage.setVisible(true);
            } else if (selectedPlayer2.equals("KEN") && kenNameImage != null) {
                player2NameImage.setImage(kenNameImage);
                player2NameImage.setVisible(true);
            }
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
            if (backgroundImage.isError()) backgroundImage = null;
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
        AudioManager.playFightSound();
    }

    private void setupInputHandling() {
        gameCanvas.setOnKeyPressed(this::handleKeyPressed);
        gameCanvas.setOnKeyReleased(this::handleKeyReleased);
        gameCanvas.requestFocus();
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode key = event.getCode();
        inputManager.handleKeyPressed(key);

        if (isNetworkMode && networkClient != null && networkClient.isConnected()) {
            short inputBits = NetworkClient.InputPacker.packInputs(inputManager, localPlayerId);
            networkClient.sendInput(inputBits);
        }

        if (key == KeyCode.ESCAPE) {
            if (currentGameState == GameState.FIGHTING) {
                if (isNetworkMode && networkClient != null) {
                    networkClient.sendPauseRequest();
                }
                pauseGame();
            } else if (currentGameState == GameState.PAUSED) {
                if (isNetworkMode && networkClient != null) {
                    networkClient.sendResumeRequest();
                }
                resumeGame();
            }
        }

        if (key == KeyCode.M && controlsInfo != null) {
            controlsInfo.setVisible(!controlsInfo.isVisible());
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        inputManager.handleKeyReleased(event.getCode());

        if (isNetworkMode && networkClient != null && networkClient.isConnected()) {
            short inputBits = NetworkClient.InputPacker.packInputs(inputManager, localPlayerId);
            networkClient.sendInput(inputBits);
        }
    }

    private void startGameLoop() {
        if (gameLoop != null) gameLoop.stop();

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
            player1.tick();
            player2.tick();

            player1.faceOpponent(player2);
            player2.faceOpponent(player1);

            checkCombat();
            checkWinConditions();

            if (isNetworkMode && networkClient != null) {
                stateUpdateCounter++;
                if (stateUpdateCounter >= 5) {
                    Fighter localFighter = localPlayerId.equals("P1") ? player1 : player2;
                    networkClient.sendStateUpdate(
                            localFighter.x,
                            localFighter.y,
                            localFighter.getHealth(),
                            localFighter.getCurrentAnimation().toString(),
                            localFighter.animationSM.getCurrentFrameIndex()
                    );
                    stateUpdateCounter = 0;
                }
            }
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
        if (player1.isAttacking() && player2.canBeHit(player1)) {
            CombatSystem.AttackData attackData = CombatSystem.getAttackData(player1.getCurrentAnimation());
            if (attackData != null) {
                player2.takeDamage(attackData.damage, player1.getCurrentAnimation());
            }
        }

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
            showGameMessage(winner + " WINS MATCH!", 5000);

            if (continueButton != null) {
                if (isNetworkMode) {
                    if (isHost) {
                        continueButton.setText("Rematch");
                    } else {
                        continueButton.setText("Wait for Host...");
                        continueButton.setDisable(true);
                        if (networkClient != null) {
                            networkClient.sendWaitingForHost();
                        }
                    }
                } else {
                    continueButton.setText("New Game");
                }
                continueButton.setVisible(true);
            }
        } else {
            if (continueButton != null) {
                if (isNetworkMode) {
                    if (isHost) {
                        continueButton.setText("Next Round");
                    } else {
                        continueButton.setText("Wait for Host...");
                        continueButton.setDisable(true);
                        if (networkClient != null) {
                            networkClient.sendWaitingForHost();
                        }
                    }
                } else {
                    continueButton.setText("Next Round");
                }
                continueButton.setVisible(true);
            }
        }
    }

    private void handleContinueButton() {
        if (isNetworkMode && isHost) {
            if (networkClient != null) {
                boolean isRematch = (currentGameState == GameState.GAME_OVER);

                if (isRematch) {
                    // Full rematch - reset everything
                    networkClient.sendRematchRequest();
                    player1Wins = 0;
                    player2Wins = 0;
                    currentRound = 1;
                } else {
                    // Next round - increment and sync
                    currentRound++;
                    networkClient.sendNextRound(currentRound, player1Wins, player2Wins);
                }

                initializeGame();
                if (messageBox != null) messageBox.setVisible(false);
                if (continueButton != null) {
                    continueButton.setVisible(false);
                    continueButton.setDisable(false);
                }
            }
        } else if (!isNetworkMode) {
            nextRound();
        }
    }

    private void startRematch() {
        System.out.println("Starting rematch - resetting to Round 1");
        player1Wins = 0;
        player2Wins = 0;
        currentRound = 1;

        initializeGame();
        if (messageBox != null) messageBox.setVisible(false);
        if (continueButton != null) {
            continueButton.setVisible(false);
            continueButton.setDisable(false);
        }
    }

    private void handleNextRound(int round, int p1Wins, int p2Wins) {
        System.out.println("Syncing to Round " + round + " (P1: " + p1Wins + " wins, P2: " + p2Wins + " wins)");

        currentRound = round;
        player1Wins = p1Wins;
        player2Wins = p2Wins;

        initializeGame();
        if (messageBox != null) messageBox.setVisible(false);
        if (continueButton != null) {
            continueButton.setVisible(false);
            continueButton.setDisable(false);
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
        if (messageBox != null) messageBox.setVisible(false);
        if (continueButton != null) continueButton.setVisible(false);
    }

    private void render() {
        frameCount++;
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastFPSCheck >= 1000) {
            currentFPS = frameCount;
            frameCount = 0;
            lastFPSCheck = currentTime;
        }

        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());
        drawMapBackground();

        if (player1 != null) player1.render(gc);
        if (player2 != null) player2.render(gc);

        if (showFPS) {
            gc.setFont(FontManager.getInstance().getSmall());
            gc.setFill(Color.YELLOW);
            gc.fillText("FPS: " + currentFPS, 10, 20);
        }
    }

    private void drawMapBackground() {
        if (backgroundImage != null && !backgroundImage.isError()) {
            gc.drawImage(backgroundImage, 0, 0, 800, 400);
        } else {
            gc.setFill(Color.LIGHTBLUE);
            gc.fillRect(0, 0, 800, 320);
            gc.setFill(Color.BROWN);
            gc.fillRect(0, 320, 800, 80);
        }
    }

    private void updateUI() {
        if (player1 == null || player2 == null) return;

        double p1HealthPercent = (double) player1.getHealth() / player1.getMaxHealth();
        double p2HealthPercent = (double) player2.getHealth() / player2.getMaxHealth();

        player1HealthBar.setProgress(p1HealthPercent);
        player2HealthBar.setProgress(p2HealthPercent);

        updateHealthBarColor(player1HealthBar, p1HealthPercent);
        updateHealthBarColor(player2HealthBar, p2HealthPercent);

        player1HealthLabel.setText(player1.getHealth() + "/" + player1.getMaxHealth());
        player2HealthLabel.setText(player2.getHealth() + "/" + player2.getMaxHealth());

        timerLabel.setText(String.valueOf(roundTimer));
        roundLabel.setText("ROUND " + currentRound);
    }

    private void updateHealthBarColor(ProgressBar healthBar, double percent) {
        if (percent > 0.6) {
            healthBar.setStyle("-fx-accent: #2ecc71;");
        } else if (percent > 0.3) {
            healthBar.setStyle("-fx-accent: #f39c12;");
        } else {
            healthBar.setStyle("-fx-accent: #e74c3c;");
        }
    }

    private void updateNetworkStatus(String status) {
        if (networkStatusLabel != null) {
            networkStatusLabel.setText("Net: " + status);
            networkStatusLabel.setVisible(true);
        }
    }

    private void showGameMessage(String message, long duration) {
        if (gameStateLabel != null) gameStateLabel.setText(message);
        if (messageBox != null) messageBox.setVisible(true);

        new Thread(() -> {
            try {
                Thread.sleep(duration);
                if (currentGameState == GameState.READY) {
                    currentGameState = GameState.FIGHTING;
                    Platform.runLater(() -> {
                        if (messageBox != null) messageBox.setVisible(false);
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void pauseGame() {
        if (currentGameState == GameState.FIGHTING) {
            currentGameState = GameState.PAUSED;
            if (pauseMenu != null) pauseMenu.setVisible(true);
            if (pausedByLabel != null && !isNetworkMode) {
                pausedByLabel.setVisible(false);
            }
        }
    }

    private void resumeGame() {
        if (currentGameState == GameState.PAUSED) {
            currentGameState = GameState.FIGHTING;
            if (pauseMenu != null) pauseMenu.setVisible(false);
            if (pausedByLabel != null) pausedByLabel.setVisible(false);
            gameCanvas.requestFocus();
        }
    }

    private void returnToMainMenu() {
        try {
            if (gameLoop != null) gameLoop.stop();
            if (isNetworkMode && networkClient != null) {
                networkClient.disconnect();
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