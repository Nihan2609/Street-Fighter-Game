package Server;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;


public class NetworkGameServer {
    private static final int PORT = 5555;
    private static final int BUFFER_SIZE = 1024;
    private static final int TICK_RATE = 60; // Match game FPS
    private static final long TICK_DURATION_MS = 1000 / TICK_RATE;

    private DatagramSocket socket;
    private volatile boolean running = false;
    private Map<String, PlayerConnection> players = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Game state validation
    private Map<String, GameState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerConnection {
        InetAddress address;
        int port;
        String playerId;
        long lastHeartbeat;
        boolean ready;

        PlayerConnection(InetAddress address, int port, String playerId) {
            this.address = address;
            this.port = port;
            this.playerId = playerId;
            this.lastHeartbeat = System.currentTimeMillis();
            this.ready = false;
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
        socket.setSoTimeout(0); // Non-blocking
    }

    public void start() {
        running = true;
        System.out.println("🎮 Game Server started on port " + PORT);
        System.out.println("📡 Waiting for Player 2 to connect...");

        // Start receiver thread
        Thread receiverThread = new Thread(this::receiveLoop);
        receiverThread.setDaemon(false);
        receiverThread.start();

        // Start heartbeat checker
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
                // Normal timeout, continue
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
                    handleInput(packet, bb);
                    break;

                case PacketType.STATE_UPDATE:
                    handleStateUpdate(packet, bb);
                    break;

                case PacketType.HEARTBEAT:
                    handleHeartbeat(packet, bb);
                    break;

                case PacketType.DISCONNECT:
                    handleDisconnect(packet, bb);
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

        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();

        if (players.size() >= 2) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Server full");
            return;
        }

        PlayerConnection pc = new PlayerConnection(
                packet.getAddress(),
                packet.getPort(),
                playerId
        );
        players.put(clientKey, pc);

        System.out.println("✅ Player connected: " + playerId + " (" + playerName + ")");

        // Send acceptance
        sendResponse(packet, PacketType.CONNECT_ACCEPTED, playerId);

        // If we have 2 players, start game
        if (players.size() == 2) {
            System.out.println("🎮 Both players connected. Starting game...");
            broadcastGameStart();
        }
    }

    private void handleInput(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        long frameNumber = bb.getLong();
        short inputBits = bb.getShort(); // Packed input flags

        // Update heartbeat
        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) {
            pc.lastHeartbeat = System.currentTimeMillis();
        }

        // Broadcast input to all players (including sender for confirmation)
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

        // Update heartbeat
        String clientKey = packet.getAddress().toString() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) {
            pc.lastHeartbeat = System.currentTimeMillis();
        }

        // Validate critical events (like health changes)
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

        players.remove(clientKey);
        playerStates.remove(playerId);

        System.out.println("👋 Player disconnected: " + playerId);

        // Notify other player
        broadcastPlayerDisconnected(playerId);
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

        // Send to all connected players
        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, pc.address, pc.port
                );
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

        // Add player IDs
        for (PlayerConnection pc : players.values()) {
            writeString(bb, pc.playerId);
        }

        byte[] data = new byte[bb.position()];
        bb.flip();
        bb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket packet = new DatagramPacket(
                        data, data.length, pc.address, pc.port
                );
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
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, pc.address, pc.port
                    );
                    socket.send(packet);
                } catch (Exception e) {
                    System.err.println("Error broadcasting disconnect: " + e.getMessage());
                }
            }
        }
    }

    private void validateGameState(String playerId, GameState state) {
        // Basic sanity checks to prevent cheating
        if (state.health < 0) state.health = 0;
        if (state.health > 1000) state.health = 1000;

        // Position bounds
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
                System.out.println("⚠️ Player timeout: " + entry.getValue().playerId);
            }
        }

        for (String key : toRemove) {
            PlayerConnection pc = players.remove(key);
            if (pc != null) {
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

            DatagramPacket response = new DatagramPacket(
                    data, data.length,
                    originalPacket.getAddress(),
                    originalPacket.getPort()
            );
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
        System.out.println("🛑 Server stopped");
    }

    public int getPlayerCount() {
        return players.size();
    }

    // Packet type constants
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
    }

    // For Player 1 (host) to run both server and client
    public static void main(String[] args) {
        try {
            NetworkGameServer server = new NetworkGameServer();
            server.start();

            // Keep server running
            System.out.println("Press Enter to stop server...");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();

            server.stop();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}