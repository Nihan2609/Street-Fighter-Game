package Client;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiplayerGame {

    public static String username = "Player";

    public static void openMultiplayer(String playerName) {
        username = playerName;
        Platform.runLater(() -> new GameStage().show());
    }

    private static class GameStage extends Stage {

        private Fighter player;
        private Map<String, Fighter> opponents = new HashMap<>();
        private Map<String, ProgressBar> opponentHealthBars = new HashMap<>();

        private ProgressBar playerHealthBar;
        private double playerHealth = 1.0;

        private final Set<KeyCode> pressedKeys = new HashSet<>();
        private AnimationTimer gameLoop;

        private DatagramSocket socket;
        private InetAddress serverAddress;
        private final int SERVER_PORT = 5555;
        private final String SERVER_IP = "127.0.0.1";

        private long lastAttackTime = 0;

        public GameStage() {
            try {
                socket = new DatagramSocket();
                serverAddress = InetAddress.getByName(SERVER_IP);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Background
            ImageView background = new ImageView(new Image(getClass().getResourceAsStream("/images/Dream.gif")));
            background.setFitWidth(800);
            background.setFitHeight(400);

            Group root = new Group(background);

            // Player
            player = new Fighter("/images/player.gif", 100, 220, 80, 120, root);

            // Health bar
            playerHealthBar = new ProgressBar(playerHealth);
            playerHealthBar.setPrefWidth(150);
            playerHealthBar.setLayoutX(50);
            playerHealthBar.setLayoutY(20);

            Pane overlay = new Pane(playerHealthBar);
            overlay.setPickOnBounds(false);

            StackPane container = new StackPane(root, overlay);
            Scene scene = new Scene(container, 800, 400);

            // Key handling
            scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
            scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

            // Game loop
            gameLoop = new AnimationTimer() {
                private long lastSend = 0;

                @Override
                public void handle(long now) {
                    handlePlayerInput(now);
                    if (now - lastSend > 50_000_000) { // ~50ms
                        sendPlayerState();
                        lastSend = now;
                    }
                }
            };
            gameLoop.start();

            // Listen for opponents
            new Thread(this::listenForOpponentUpdates).start();

            this.setTitle("Multiplayer - Street Fighter");
            this.setScene(scene);
            container.requestFocus();

            this.setOnCloseRequest(e -> {
                if (gameLoop != null) gameLoop.stop();
            });
        }

        private void sendAttack() {
            try {
                String msg = "ATTACK," + username;
                byte[] buf = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddress, SERVER_PORT);
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void handlePlayerInput(long now) {
            if (pressedKeys.contains(KeyCode.A)) player.moveLeft();
            if (pressedKeys.contains(KeyCode.D)) player.moveRight();
            if (pressedKeys.contains(KeyCode.SPACE)) player.jump();
            player.applyGravity();

            if (pressedKeys.contains(KeyCode.J) && now - lastAttackTime > 500_000_000) {
                sendAttack();
                lastAttackTime = now;
            }

        }

        private void sendPlayerState() {
            try {
                String state = username + "," + player.getX() + "," + player.getY() + "," + playerHealth;
                byte[] buf = state.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddress, SERVER_PORT);
                socket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void listenForOpponentUpdates() {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength());
                    String[] playersData = data.split(";");

                    Platform.runLater(() -> {
                        for (String p : playersData) {
                            if (p.isEmpty()) continue;
                            String[] parts = p.split(",");
                            String uname = parts[0];
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            double health = Double.parseDouble(parts[3]);

                            if (uname.equals(username)) continue; // skip ourselves

                            Fighter f = opponents.get(uname);
                            if (f == null) {
                                f = new Fighter("/images/bot.gif", x, y, 80, 120, ((Group) player.getBody().getParent()));
                                opponents.put(uname, f);

                                ProgressBar bar = new ProgressBar(health);
                                bar.setPrefWidth(150);
                                bar.setLayoutX(600);
                                bar.setLayoutY(20 + 30 * opponents.size()); // stack bars
                                opponentHealthBars.put(uname, bar);
                                ((Pane) ((StackPane) getScene().getRoot()).getChildren().get(1)).getChildren().add(bar);
                            } else {
                                f.setX(x);
                                f.setY(y);
                                opponentHealthBars.get(uname).setProgress(health);
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}



