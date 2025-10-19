package Client;

import javafx.application.Platform;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

public class NetworkClient {
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private volatile boolean connected = false;
    private volatile boolean running = false;

    private String playerId;
    private String playerName;
    private boolean isHost;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private NetworkCallback callback;

    private long frameNumber = 0;
    private static final int BUFFER_SIZE = 1024;
    private static final int INPUT_BUFFER_SIZE = 3;
    private long lastInputSendTime = 0;
    private static final long INPUT_SEND_INTERVAL = 16;

    private Object lobbyController = null;

    public interface NetworkCallback {
        void onConnected();
        void onDisconnected();
        void onGameStart();
        void onInputReceived(String playerId, long frameNumber, short inputBits);
        void onPlayerDisconnected(String playerId);
        void onGameConfig(String p1Char, String p2Char, String mapFile);
        void onPauseGame(String pausedBy);
        void onResumeGame();
        void onRematchRequest();
        void onNextRound(int round, int p1Wins, int p2Wins); // NEW
        void onWaitingForHost();
    }

    public NetworkClient(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.isHost = playerId.equals("P1");
    }

    public void setLobbyController(Object controller) {
        this.lobbyController = controller;
    }

    public void connect(String serverIP, int port) throws Exception {
        this.serverAddress = InetAddress.getByName(serverIP);
        this.serverPort = port;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(100);

        running = true;

        Thread receiverThread = new Thread(this::receiveLoop);
        receiverThread.setDaemon(true);
        receiverThread.start();

        sendConnectRequest();
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 1, 1, TimeUnit.SECONDS);
    }

    private void sendConnectRequest() {
        try {
            ByteBuffer bb = ByteBuffer.allocate(256);
            bb.put(PacketType.CONNECT);
            writeString(bb, playerId);
            writeString(bb, playerName);
            bb.put((byte) (isHost ? 1 : 0));

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);

            System.out.println("Connection request sent to " + serverAddress + ":" + serverPort + " as " + (isHost ? "HOST" : "CLIENT"));
        } catch (Exception e) {
            System.err.println("Error sending connect request: " + e.getMessage());
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                handlePacket(packet);
            } catch (SocketTimeoutException e) {
                // Normal timeout
            } catch (Exception e) {
                if (running) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                }
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
            byte packetType = bb.get();

            switch (packetType) {
                case PacketType.CONNECT_ACCEPTED:
                    handleConnectAccepted(bb);
                    break;
                case PacketType.CONNECT_REJECTED:
                    handleConnectRejected(bb);
                    break;
                case PacketType.INPUT_BROADCAST:
                    handleInputBroadcast(bb);
                    break;
                case PacketType.GAME_START:
                    handleGameStart(bb);
                    break;
                case PacketType.GAME_CONFIG:
                    handleGameConfig(bb);
                    break;
                case PacketType.PLAYER_DISCONNECTED:
                    handlePlayerDisconnected(bb);
                    break;
                case PacketType.PAUSE_GAME:
                    handlePauseGame(bb);
                    break;
                case PacketType.RESUME_GAME:
                    handleResumeGame(bb);
                    break;
                case PacketType.REMATCH:
                    handleRematch(bb);
                    break;
                case PacketType.NEXT_ROUND:
                    handleNextRound(bb);
                    break;
                case PacketType.WAITING_FOR_HOST:
                    handleWaitingForHost(bb);
                    break;
                default:
                    System.err.println("Unknown packet type: " + packetType);
            }
        } catch (Exception e) {
            System.err.println("Error handling packet: " + e.getMessage());
        }
    }

    private void handleConnectAccepted(ByteBuffer bb) {
        String assignedId = readString(bb);
        connected = true;
        System.out.println("Connected to server! Player ID: " + assignedId + (isHost ? " (HOST)" : " (CLIENT)"));
        if (callback != null) {
            callback.onConnected();
        }
    }

    private void handleConnectRejected(ByteBuffer bb) {
        String reason = readString(bb);
        System.err.println("Connection rejected: " + reason);
        disconnect();
        if (callback != null) {
            callback.onDisconnected();
        }
    }

    private void handleInputBroadcast(ByteBuffer bb) {
        String playerId = readString(bb);
        long frameNumber = bb.getLong();
        short inputBits = bb.getShort();
        if (callback != null) {
            callback.onInputReceived(playerId, frameNumber, inputBits);
        }
    }

    private void handleGameStart(ByteBuffer bb) {
        int playerCount = bb.getInt();
        System.out.println("Game starting with " + playerCount + " players");
        if (callback != null) {
            callback.onGameStart();
        }
    }

    private void handleGameConfig(ByteBuffer bb) {
        String p1Char = readString(bb);
        String p2Char = readString(bb);
        String mapFile = readString(bb);

        System.out.println("Received game config: " + p1Char + " vs " + p2Char + " on " + mapFile);

        if (callback != null) {
            callback.onGameConfig(p1Char, p2Char, mapFile);
        }

        if (!isHost && lobbyController != null) {
            try {
                java.lang.reflect.Method method = lobbyController.getClass()
                        .getMethod("launchGame", String.class, String.class, String.class);
                method.invoke(lobbyController, p1Char, p2Char, mapFile);
            } catch (Exception e) {
                System.err.println("Error launching game from lobby: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handlePlayerDisconnected(ByteBuffer bb) {
        String playerId = readString(bb);
        System.out.println("Player disconnected: " + playerId);
        if (callback != null) {
            callback.onPlayerDisconnected(playerId);
        }
    }

    private void handlePauseGame(ByteBuffer bb) {
        String pausedBy = readString(bb);
        System.out.println("Game paused by: " + pausedBy);
        if (callback != null) {
            Platform.runLater(() -> callback.onPauseGame(pausedBy));
        }
    }

    private void handleResumeGame(ByteBuffer bb) {
        System.out.println("Game resumed");
        if (callback != null) {
            Platform.runLater(() -> callback.onResumeGame());
        }
    }

    private void handleRematch(ByteBuffer bb) {
        System.out.println("Host started rematch");
        if (callback != null) {
            Platform.runLater(() -> callback.onRematchRequest());
        }
    }

    private void handleNextRound(ByteBuffer bb) {
        int round = bb.getInt();
        int p1Wins = bb.getInt();
        int p2Wins = bb.getInt();

        System.out.println("Received next round: Round " + round + " (P1: " + p1Wins + ", P2: " + p2Wins + ")");

        if (callback != null) {
            Platform.runLater(() -> callback.onNextRound(round, p1Wins, p2Wins));
        }
    }

    private void handleWaitingForHost(ByteBuffer bb) {
        System.out.println("Waiting for host to start rematch");
        if (callback != null) {
            Platform.runLater(() -> callback.onWaitingForHost());
        }
    }

    public void sendPauseRequest() {
        if (!connected) return;

        try {
            ByteBuffer bb = ByteBuffer.allocate(128);
            bb.put(PacketType.PAUSE_GAME);
            writeString(bb, playerId);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
            System.out.println("Pause request sent");
        } catch (Exception e) {
            System.err.println("Error sending pause request: " + e.getMessage());
        }
    }

    public void sendResumeRequest() {
        if (!connected) return;

        try {
            ByteBuffer bb = ByteBuffer.allocate(128);
            bb.put(PacketType.RESUME_GAME);
            writeString(bb, playerId);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
            System.out.println("Resume request sent");
        } catch (Exception e) {
            System.err.println("Error sending resume request: " + e.getMessage());
        }
    }

    public void sendRematchRequest() {
        if (!connected || !isHost) {
            System.out.println("Only host can start rematch");
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.allocate(128);
            bb.put(PacketType.REMATCH);
            writeString(bb, playerId);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
            System.out.println("Rematch request sent");
        } catch (Exception e) {
            System.err.println("Error sending rematch request: " + e.getMessage());
        }
    }

    public void sendNextRound(int round, int p1Wins, int p2Wins) {
        if (!connected || !isHost) {
            System.out.println("Only host can send next round");
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.allocate(128);
            bb.put(PacketType.NEXT_ROUND);
            writeString(bb, playerId);
            bb.putInt(round);
            bb.putInt(p1Wins);
            bb.putInt(p2Wins);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);

            System.out.println("Sent next round: Round " + round + " (P1: " + p1Wins + ", P2: " + p2Wins + ")");
        } catch (Exception e) {
            System.err.println("Error sending next round: " + e.getMessage());
        }
    }

    public void sendWaitingForHost() {
        if (!connected || isHost) return;

        try {
            ByteBuffer bb = ByteBuffer.allocate(128);
            bb.put(PacketType.WAITING_FOR_HOST);
            writeString(bb, playerId);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Error sending waiting status: " + e.getMessage());
        }
    }

    public void sendGameConfig(String p1Char, String p2Char, String mapFile) {
        if (!connected || !isHost) {
            System.out.println("Only host can send game config");
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.allocate(256);
            bb.put(PacketType.GAME_CONFIG);
            writeString(bb, playerId);
            writeString(bb, p1Char);
            writeString(bb, p2Char);
            writeString(bb, mapFile);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);

            System.out.println("Sent game config: " + p1Char + " vs " + p2Char + " on " + mapFile);
        } catch (Exception e) {
            System.err.println("Error sending game config: " + e.getMessage());
        }
    }

    public void sendInput(short inputBits) {
        if (!connected) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInputSendTime < INPUT_SEND_INTERVAL) {
            return;
        }
        lastInputSendTime = currentTime;

        try {
            ByteBuffer bb = ByteBuffer.allocate(128);
            bb.put(PacketType.INPUT);
            writeString(bb, playerId);
            bb.putLong(frameNumber++);
            bb.putShort(inputBits);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Error sending input: " + e.getMessage());
        }
    }

    public void sendStateUpdate(float x, float y, int health, String animType, int frameIdx) {
        if (!connected) return;

        try {
            ByteBuffer bb = ByteBuffer.allocate(256);
            bb.put(PacketType.STATE_UPDATE);
            writeString(bb, playerId);
            bb.putFloat(x);
            bb.putFloat(y);
            bb.putInt(health);
            writeString(bb, animType);
            bb.putInt(frameIdx);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Error sending state update: " + e.getMessage());
        }
    }

    private void sendHeartbeat() {
        if (!connected) return;

        try {
            ByteBuffer bb = ByteBuffer.allocate(64);
            bb.put(PacketType.HEARTBEAT);
            writeString(bb, playerId);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Error sending heartbeat: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (!connected) return;

        try {
            ByteBuffer bb = ByteBuffer.allocate(64);
            bb.put(PacketType.DISCONNECT);
            writeString(bb, playerId);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, serverPort);
            socket.send(packet);
        } catch (Exception e) {
            System.err.println("Error sending disconnect: " + e.getMessage());
        }

        connected = false;
        running = false;
        scheduler.shutdown();

        if (socket != null && !socket.isClosed()) {
            socket.close();
        }

        System.out.println("Disconnected from server");
    }

    private void writeString(ByteBuffer bb, String str) {
        byte[] bytes = str.getBytes();
        bb.putShort((short) bytes.length);
        bb.put(bytes);
    }

    private String readString(ByteBuffer bb) {
        short length = bb.getShort();
        byte[] bytes = new byte[length];
        bb.get(bytes);
        return new String(bytes);
    }

    public void setCallback(NetworkCallback callback) {
        this.callback = callback;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isHost() {
        return isHost;
    }

    public static class PacketType {
        public static final byte CONNECT = 0x01;
        public static final byte CONNECT_ACCEPTED = 0x02;
        public static final byte CONNECT_REJECTED = 0x03;
        public static final byte DISCONNECT = 0x04;
        public static final byte INPUT = 0x10;
        public static final byte INPUT_BROADCAST = 0x11;
        public static final byte STATE_UPDATE = 0x20;
        public static final byte GAME_START = 0x30;
        public static final byte HEARTBEAT = 0x40;
        public static final byte PLAYER_DISCONNECTED = 0x50;
        public static final byte GAME_CONFIG = 0x60;
        public static final byte PAUSE_GAME = 0x70;
        public static final byte RESUME_GAME = 0x71;
        public static final byte REMATCH = (byte) 0x80;
        public static final byte NEXT_ROUND = (byte) 0x82;
        public static final byte WAITING_FOR_HOST = (byte) 0x81;
    }

    public static class InputPacker {
        public static final short LEFT = 1 << 0;
        public static final short RIGHT = 1 << 1;
        public static final short UP = 1 << 2;
        public static final short DOWN = 1 << 3;
        public static final short LIGHT_PUNCH = 1 << 4;
        public static final short HEAVY_PUNCH = 1 << 5;
        public static final short LIGHT_KICK = 1 << 6;
        public static final short HEAVY_KICK = 1 << 7;
        public static final short BLOCK = 1 << 8;

        public static short packInputs(InputManager inputManager, String playerId) {
            short bits = 0;
            if (inputManager.isActionPressed(playerId, "left")) bits |= LEFT;
            if (inputManager.isActionPressed(playerId, "right")) bits |= RIGHT;
            if (inputManager.isActionPressed(playerId, "up")) bits |= UP;
            if (inputManager.isActionPressed(playerId, "down")) bits |= DOWN;
            if (inputManager.isActionPressed(playerId, "light_punch")) bits |= LIGHT_PUNCH;
            if (inputManager.isActionPressed(playerId, "heavy_punch")) bits |= HEAVY_PUNCH;
            if (inputManager.isActionPressed(playerId, "light_kick")) bits |= LIGHT_KICK;
            if (inputManager.isActionPressed(playerId, "heavy_kick")) bits |= HEAVY_KICK;
            if (inputManager.isActionPressed(playerId, "block")) bits |= BLOCK;
            return bits;
        }

        public static void applyInputs(short inputBits, InputManager inputManager, String playerId) {
            inputManager.setNetworkInput(playerId, "left", (inputBits & LEFT) != 0);
            inputManager.setNetworkInput(playerId, "right", (inputBits & RIGHT) != 0);
            inputManager.setNetworkInput(playerId, "up", (inputBits & UP) != 0);
            inputManager.setNetworkInput(playerId, "down", (inputBits & DOWN) != 0);
            inputManager.setNetworkInput(playerId, "light_punch", (inputBits & LIGHT_PUNCH) != 0);
            inputManager.setNetworkInput(playerId, "heavy_punch", (inputBits & HEAVY_PUNCH) != 0);
            inputManager.setNetworkInput(playerId, "light_kick", (inputBits & LIGHT_KICK) != 0);
            inputManager.setNetworkInput(playerId, "heavy_kick", (inputBits & HEAVY_KICK) != 0);
            inputManager.setNetworkInput(playerId, "block", (inputBits & BLOCK) != 0);
        }
    }
}