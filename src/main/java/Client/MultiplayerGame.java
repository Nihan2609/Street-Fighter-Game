package Client;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class MultiplayerGame extends Application {

    private static final int SERVER_PORT = 9999;  // your server's UDP port
    private static final String SERVER_IP = "127.0.0.1"; // server IP or hostname

    private DatagramSocket socket;
    private InetAddress serverAddress;

    private Set<KeyCode> pressedKeys = new HashSet<>();

    private Player player, opponent;
    private ProgressBar playerHealthBar, opponentHealthBar;
    private double playerHealth = 1.0, opponentHealth = 1.0;

    public MultiplayerGame() throws Exception {
        socket = new DatagramSocket();
        serverAddress = InetAddress.getByName(SERVER_IP);
    }

    public void start(Stage stage) {
        Group root = new Group();

        ImageView background = new ImageView(new Image(getClass().getResourceAsStream("/images/Dream.gif")));
        background.setFitWidth(800);
        background.setFitHeight(400);
        root.getChildren().add(background);

        player = new Player("/images/player.gif", 100, 220, 80, 120, root);
        opponent = new Player("/images/bot.gif", 600, 220, 80, 120, root);

        playerHealthBar = new ProgressBar(playerHealth);
        playerHealthBar.setPrefWidth(150);
        playerHealthBar.setLayoutX(50);
        playerHealthBar.setLayoutY(20);

        opponentHealthBar = new ProgressBar(opponentHealth);
        opponentHealthBar.setPrefWidth(150);
        opponentHealthBar.setLayoutX(600);
        opponentHealthBar.setLayoutY(20);

        Pane overlay = new Pane(playerHealthBar, opponentHealthBar);
        overlay.setPickOnBounds(false);

        StackPane container = new StackPane();
        container.getChildren().addAll(root, overlay);

        Scene scene = new Scene(container, 800, 400);

        scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastSend = 0;

            @Override
            public void handle(long now) {
                // Handle player movement input
                if (pressedKeys.contains(KeyCode.A)) player.moveLeft();
                if (pressedKeys.contains(KeyCode.D)) player.moveRight();
                if (pressedKeys.contains(KeyCode.W)) player.jump();
                player.applyGravity();

                // Send player position and actions to server every 50ms
                if (now - lastSend > 50_000_000) {
                    sendPlayerState();
                    lastSend = now;
                }
            }
        };

        gameLoop.start();

        // Thread to listen to opponent updates from server
        new Thread(this::listenForOpponentUpdates).start();

        stage.setTitle("Multiplayer - Street Fighter");
        stage.setScene(scene);
        stage.show();
    }

    private void sendPlayerState() {
        try {
            String state = player.getX() + "," + player.getY(); // add more info if needed
            byte[] buf = state.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, serverAddress, SERVER_PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForOpponentUpdates() {
        byte[] buffer = new byte[256];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength());
                String[] parts = data.split(",");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                Platform.runLater(() -> {
                    opponent.setX(x);
                    opponent.setY(y);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Player class (similar to your Fighter class)
    private static class Player {
        private ImageView body;
        private double dy = 0;
        private final double JUMP_FORCE = -15;
        private final double FLOOR_Y = 220;
        private final double gravity = 1;

        public Player(String imagePath, double x, double y, double width, double height, Group root) {
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
            body.setX(body.getX() - 5);
        }

        public void moveRight() {
            body.setX(body.getX() + 5);
        }

        public void jump() {
            if (onGround()) {
                dy = JUMP_FORCE;
            }
        }

        public void applyGravity() {
            if (!onGround()) {
                dy += gravity;
                body.setY(body.getY() + dy);
            } else {
                dy = 0;
                body.setY(FLOOR_Y);
            }
        }

        public boolean onGround() {
            return body.getY() >= FLOOR_Y;
        }

        public double getX() {
            return body.getX();
        }

        public void setX(double x) {
            body.setX(x);
        }

        public double getY() {
            return body.getY();
        }

        public void setY(double y) {
            body.setY(y);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}

