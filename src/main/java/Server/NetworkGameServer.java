package Server;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class NetworkGameServer {
    private static final int PORT = 5555;
    private static final int BUFFER_SIZE = 1024;
    private static final int TICK_RATE = 60;
    private static final long TICK_DURATION_MS = 1000 / TICK_RATE;

    private DatagramSocket socket;
    private volatile boolean running = false;
    private Map<String, PlayerConnection> players = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private Map<String, GameState> playerStates = new ConcurrentHashMap<>();

    // NEW: Track game state
    private volatile boolean gamePaused = false;
    private String hostPlayerId = null;

    private static class PlayerConnection {
        InetAddress address;
        int port;
        String playerId;
        long lastHeartbeat;
        boolean ready;
        boolean isHost; // NEW

        PlayerConnection(InetAddress address, int port, String playerId, boolean isHost) {
            this.address = address;
            this.port = port;
            this.playerId = playerId;
            this.lastHeartbeat = System.currentTimeMillis();
            this.ready = false;
            this.isHost = isHost;
        }

        boolean isAlive() {
            return (System.currentTimeMillis() - lastHeartbeat) < 5000;
        }
    }

    private static class GameState {
        float x, y;
        int health;
        String animationType;
        int frameIndex;
        long timestamp;
    }

    public NetworkGameServer() throws SocketException {
        socket = new DatagramSocket(PORT);
        socket.setSoTimeout(0);
    }

    public void start() {
        running = true;
        System.out.println("===========================================");
        System.out.println("    Street Fighter Network Server v2.0    ");
        System.out.println("===========================================");
        System.out.println("Server started on port " + PORT);
        System.out.println("Waiting for host to connect...");

        Thread receiverThread = new Thread(this::receiveLoop);
        receiverThread.setDaemon(false);
        receiverThread.start();

        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 1, 1, TimeUnit.SECONDS);
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
                case PacketType.CONNECT:
                    handleConnect(packet, bb);
                    break;
                case PacketType.INPUT:
                    if (!gamePaused) { // Only process input when not paused
                        handleInput(packet, bb);
                    }
                    break;
                case PacketType.STATE_UPDATE:
                    if (!gamePaused) { // Only process state when not paused
                        handleStateUpdate(packet, bb);
                    }
                    break;
                case PacketType.HEARTBEAT:
                    handleHeartbeat(packet, bb);
                    break;
                case PacketType.DISCONNECT:
                    handleDisconnect(packet, bb);
                    break;
                case PacketType.GAME_CONFIG:
                    handleGameConfig(packet, bb);
                    break;
                case PacketType.PAUSE_GAME:
                    handlePauseGame(packet, bb);
                    break;
                case PacketType.RESUME_GAME:
                    handleResumeGame(packet, bb);
                    break;
                case PacketType.REMATCH:
                    handleRematch(packet, bb);
                    break;
                case PacketType.WAITING_FOR_HOST:
                    handleWaitingForHost(packet, bb);
                    break;
                default:
                    System.err.println("Unknown packet type: " + packetType);
            }
        } catch (Exception e) {
            System.err.println("Error handling packet: " + e.getMessage());
        }
    }

    private void handleConnect(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        String playerName = readString(bb);
        boolean isHost = bb.get() == 1;

        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();

        // First connection must be host
        if (players.isEmpty() && !isHost) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Host must connect first");
            return;
        }

        if (players.size() >= 2) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Server full");
            return;
        }

        // Ensure only one host
        if (isHost && hostPlayerId != null) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Host already connected");
            return;
        }

        PlayerConnection pc = new PlayerConnection(packet.getAddress(), packet.getPort(), playerId, isHost);
        players.put(clientKey, pc);

        if (isHost) {
            hostPlayerId = playerId;
            System.out.println("✓ HOST connected: " + playerId + " (" + playerName + ")");
            System.out.println("  Waiting for client to join...");
        } else {
            System.out.println("✓ CLIENT connected: " + playerId + " (" + playerName + ")");
        }

        sendResponse(packet, PacketType.CONNECT_ACCEPTED, playerId);

        if (players.size() == 2) {

            System.out.println("  Both players connected! Ready to fight!");

            broadcastGameStart();
        }
    }

    private void handleInput(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        long frameNumber = bb.getLong();
        short inputBits = bb.getShort();

        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) {
            pc.lastHeartbeat = System.currentTimeMillis();
        }

        broadcastInput(playerId, frameNumber, inputBits);
    }

    private void handleStateUpdate(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        GameState state = new GameState();
        state.x = bb.getFloat();
        state.y = bb.getFloat();
        state.health = bb.getInt();
        state.animationType = readString(bb);
        state.frameIndex = bb.getInt();
        state.timestamp = System.currentTimeMillis();

        playerStates.put(playerId, state);

        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) {
            pc.lastHeartbeat = System.currentTimeMillis();
        }

        validateGameState(playerId, state);
    }

    private void handleHeartbeat(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();

        PlayerConnection pc = players.get(clientKey);
        if (pc != null) {
            pc.lastHeartbeat = System.currentTimeMillis();
        }
    }

    private void handleDisconnect(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();

        PlayerConnection pc = players.remove(clientKey);
        playerStates.remove(playerId);

        if (pc != null && pc.isHost) {
            hostPlayerId = null;
        }

        System.out.println("✗ Player disconnected: " + playerId + (pc != null && pc.isHost ? " (HOST)" : ""));
        broadcastPlayerDisconnected(playerId);
    }

    private void handleGameConfig(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        // Only host can configure game
        if (!playerId.equals(hostPlayerId)) {
            System.out.println("⚠ Non-host player tried to configure game: " + playerId);
            return;
        }

        String p1Char = readString(bb);
        String p2Char = readString(bb);
        String mapFile = readString(bb);

        System.out.println("→ Host configured game: " + p1Char + " vs " + p2Char + " on " + mapFile);

        ByteBuffer outBb = ByteBuffer.allocate(256);
        outBb.put(PacketType.GAME_CONFIG);
        writeString(outBb, p1Char);
        writeString(outBb, p2Char);
        writeString(outBb, mapFile);

        byte[] data = new byte[outBb.position()];
        outBb.flip();
        outBb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(sendPacket);
            } catch (Exception e) {
                System.err.println("Error broadcasting game config: " + e.getMessage());
            }
        }
    }

    private void handlePauseGame(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        if (gamePaused) {
            System.out.println("⏸ Game already paused");
            return;
        }

        gamePaused = true;
        System.out.println("⏸ Game PAUSED by: " + playerId);

        // Broadcast pause to all players
        ByteBuffer outBb = ByteBuffer.allocate(128);
        outBb.put(PacketType.PAUSE_GAME);
        writeString(outBb, playerId);

        byte[] data = new byte[outBb.position()];
        outBb.flip();
        outBb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(sendPacket);
            } catch (Exception e) {
                System.err.println("Error broadcasting pause: " + e.getMessage());
            }
        }
    }

    private void handleResumeGame(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        if (!gamePaused) {
            System.out.println("▶ Game not paused");
            return;
        }

        gamePaused = false;
        System.out.println("▶ Game RESUMED by: " + playerId);

        // Broadcast resume to all players
        ByteBuffer outBb = ByteBuffer.allocate(128);
        outBb.put(PacketType.RESUME_GAME);
        writeString(outBb, playerId);

        byte[] data = new byte[outBb.position()];
        outBb.flip();
        outBb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(sendPacket);
            } catch (Exception e) {
                System.err.println("Error broadcasting resume: " + e.getMessage());
            }
        }
    }

    private void handleRematch(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        // Only host can start rematch
        if (!playerId.equals(hostPlayerId)) {
            System.out.println("⚠ Non-host player tried to start rematch: " + playerId);
            return;
        }

        System.out.println("Host started REMATCH");

        // Reset game state
        gamePaused = false;
        playerStates.clear();

        // Broadcast rematch to all players
        ByteBuffer outBb = ByteBuffer.allocate(128);
        outBb.put(PacketType.REMATCH);
        writeString(outBb, playerId);

        byte[] data = new byte[outBb.position()];
        outBb.flip();
        outBb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(sendPacket);
            } catch (Exception e) {
                System.err.println("Error broadcasting rematch: " + e.getMessage());
            }
        }
    }

    private void handleWaitingForHost(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        System.out.println("⏳ " + playerId + " is waiting for host to start rematch");
    }

    private void broadcastInput(String playerId, long frameNumber, short inputBits) {
        ByteBuffer bb = ByteBuffer.allocate(256);
        bb.put(PacketType.INPUT_BROADCAST);
        writeString(bb, playerId);
        bb.putLong(frameNumber);
        bb.putShort(inputBits);

        byte[] data = new byte[bb.position()];
        bb.flip();
        bb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(packet);
            } catch (Exception e) {
                System.err.println("Error broadcasting input: " + e.getMessage());
            }
        }
    }

    private void broadcastGameStart() {
        ByteBuffer bb = ByteBuffer.allocate(256);
        bb.put(PacketType.GAME_START);
        bb.putInt(players.size());

        for (PlayerConnection pc : players.values()) {
            writeString(bb, pc.playerId);
        }

        byte[] data = new byte[bb.position()];
        bb.flip();
        bb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket packet = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(packet);
            } catch (Exception e) {
                System.err.println("Error broadcasting game start: " + e.getMessage());
            }
        }
    }

    private void broadcastPlayerDisconnected(String playerId) {
        ByteBuffer bb = ByteBuffer.allocate(256);
        bb.put(PacketType.PLAYER_DISCONNECTED);
        writeString(bb, playerId);

        byte[] data = new byte[bb.position()];
        bb.flip();
        bb.get(data);

        for (PlayerConnection pc : players.values()) {
            if (!pc.playerId.equals(playerId)) {
                try {
                    DatagramPacket packet = new DatagramPacket(data, data.length, pc.address, pc.port);
                    socket.send(packet);
                } catch (Exception e) {
                    System.err.println("Error broadcasting disconnect: " + e.getMessage());
                }
            }
        }
    }

    private void validateGameState(String playerId, GameState state) {
        if (state.health < 0) state.health = 0;
        if (state.health > 1000) state.health = 1000;
        if (state.x < 0) state.x = 0;
        if (state.x > 740) state.x = 740;
        if (state.y < 0) state.y = 0;
        if (state.y > 400) state.y = 400;
    }

    private void checkHeartbeats() {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, PlayerConnection> entry : players.entrySet()) {
            if (!entry.getValue().isAlive()) {
                toRemove.add(entry.getKey());
                System.out.println("💔 Player timeout: " + entry.getValue().playerId);
            }
        }

        for (String key : toRemove) {
            PlayerConnection pc = players.remove(key);
            if (pc != null) {
                if (pc.isHost) {
                    hostPlayerId = null;
                }
                broadcastPlayerDisconnected(pc.playerId);
            }
        }
    }

    private void sendResponse(DatagramPacket originalPacket, byte packetType, String message) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(256);
            bb.put(packetType);
            writeString(bb, message);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket response = new DatagramPacket(data, data.length, originalPacket.getAddress(), originalPacket.getPort());
            socket.send(response);
        } catch (Exception e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
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

    public void stop() {
        running = false;
        scheduler.shutdown();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("\n===========================================");
        System.out.println("         Server stopped gracefully        ");
        System.out.println("===========================================");
    }

    public int getPlayerCount() {
        return players.size();
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
        public static final byte WAITING_FOR_HOST = (byte) 0x81;
    }

    public static void main(String[] args) {
        try {
            NetworkGameServer server = new NetworkGameServer();
            server.start();

            System.out.println("\nPress Enter to stop server...");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();

            server.stop();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}