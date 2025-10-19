package Server;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class NetworkGameServer {
    private static final int PORT = 5555;
    private static final int BUFFER_SIZE = 1024;

    private final DatagramSocket socket;
    private volatile boolean running = false;
    private final Map<String, PlayerConnection> players = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, GameState> playerStates = new ConcurrentHashMap<>();

    private volatile boolean gamePaused = false;
    private String hostPlayerId = null;

    private static class PlayerConnection {
        InetAddress address;
        int port;
        String playerId;
        long lastHeartbeat;
        boolean ready;
        boolean isHost;

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
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(PORT));
        socket.setSoTimeout(0);
    }

    public void start() {
        running = true;

        Thread receiverThread = new Thread(this::receiveLoop, "UDP-Receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();

        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 1, 1, TimeUnit.SECONDS);
    }

    private void receiveLoop() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running) {
            try {
                socket.receive(packet);
                handlePacket(packet);
            } catch (Exception e) {
                if (running && !(e instanceof SocketTimeoutException)) {
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
                    if (!gamePaused) handleInput(packet, bb);
                    break;
                case PacketType.STATE_UPDATE:
                    if (!gamePaused) handleStateUpdate(packet, bb);
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
                case PacketType.NEXT_ROUND:
                    handleNextRound(packet, bb);
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
        String clientKey = packet.getAddress() + ":" + packet.getPort();

        if (players.isEmpty() && !isHost) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Host connects first");
            return;
        }

        if (players.size() >= 2) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Server full");
            return;
        }

        if (isHost && hostPlayerId != null) {
            sendResponse(packet, PacketType.CONNECT_REJECTED, "Host already connected");
            return;
        }

        PlayerConnection pc = new PlayerConnection(packet.getAddress(), packet.getPort(), playerId, isHost);
        players.put(clientKey, pc);

        if (isHost) {
            hostPlayerId = playerId;
            System.out.println("HOST connected: " + playerId + " (" + playerName + ")");
            System.out.println("Waiting for client to join...");
        } else {
            System.out.println("CLIENT connected: " + playerId + " (" + playerName + ")");
        }

        sendResponse(packet, PacketType.CONNECT_ACCEPTED, playerId);

        if (players.size() == 2) {
            System.out.println("Both players connected! Ready to fight!");
            broadcastGameStart();
        }
    }

    private void handleInput(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        long frameNumber = bb.getLong();
        short inputBits = bb.getShort();

        String clientKey = packet.getAddress() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) pc.lastHeartbeat = System.currentTimeMillis();

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

        String clientKey = packet.getAddress() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) pc.lastHeartbeat = System.currentTimeMillis();

        validateGameState(playerId, state);
    }

    private void handleHeartbeat(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        String clientKey = packet.getAddress() + ":" + packet.getPort();
        PlayerConnection pc = players.get(clientKey);
        if (pc != null) pc.lastHeartbeat = System.currentTimeMillis();
    }

    private void handleDisconnect(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        String clientKey = packet.getAddress() + ":" + packet.getPort();

        PlayerConnection pc = players.remove(clientKey);
        playerStates.remove(playerId);

        if (pc != null && pc.isHost) hostPlayerId = null;

        System.out.println("Player disconnected: " + playerId + (pc != null && pc.isHost ? " (HOST)" : ""));
        broadcastPlayerDisconnected(playerId);
    }

    private void handleGameConfig(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        if (!playerId.equals(hostPlayerId)) {
            System.out.println("Non-host player tried to configure game: " + playerId);
            return;
        }

        String p1Char = readString(bb);
        String p2Char = readString(bb);
        String mapFile = readString(bb);

        System.out.println("Host configured game: " + p1Char + " vs " + p2Char + " on " + mapFile);

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
                System.out.println("Sent game config to " + pc.playerId);
            } catch (Exception e) {
                System.err.println("Error broadcasting game config: " + e.getMessage());
            }
        }
    }

    private void handlePauseGame(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        if (gamePaused) return;

        gamePaused = true;
        System.out.println("⏸Game PAUSED by: " + playerId);

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
        if (!gamePaused) return;

        gamePaused = false;
        System.out.println("Game RESUMED by: " + playerId);

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

        if (!playerId.equals(hostPlayerId)) {
            System.out.println("Non-host player tried to start rematch: " + playerId);
            return;
        }

        System.out.println("Host started REMATCH - Resetting to Round 1");

        gamePaused = false;
        playerStates.clear();

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
                System.out.println("Sent rematch to " + pc.playerId);
            } catch (Exception e) {
                System.err.println("Error broadcasting rematch: " + e.getMessage());
            }
        }
    }

    private void handleNextRound(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);

        if (!playerId.equals(hostPlayerId)) {
            System.out.println("Non-host player tried to start next round: " + playerId);
            return;
        }

        int round = bb.getInt();
        int p1Wins = bb.getInt();
        int p2Wins = bb.getInt();

        System.out.println("Host started next round: Round " + round + " (P1: " + p1Wins + " wins, P2: " + p2Wins + " wins)");

        gamePaused = false;
        playerStates.clear();

        ByteBuffer outBb = ByteBuffer.allocate(128);
        outBb.put(PacketType.NEXT_ROUND);
        outBb.putInt(round);
        outBb.putInt(p1Wins);
        outBb.putInt(p2Wins);

        byte[] data = new byte[outBb.position()];
        outBb.flip();
        outBb.get(data);

        for (PlayerConnection pc : players.values()) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(data, data.length, pc.address, pc.port);
                socket.send(sendPacket);
                System.out.println("Sent next round to " + pc.playerId);
            } catch (Exception e) {
                System.err.println("Error broadcasting next round: " + e.getMessage());
            }
        }
    }

    private void handleWaitingForHost(DatagramPacket packet, ByteBuffer bb) {
        String playerId = readString(bb);
        System.out.println(playerId + " is waiting for host to start rematch");
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
        state.health = Math.min(1000, Math.max(0, state.health));
        state.x = Math.min(740, Math.max(0, state.x));
        state.y = Math.min(400, Math.max(0, state.y));
    }

    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, PlayerConnection>> it = players.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, PlayerConnection> entry = it.next();
            if (now - entry.getValue().lastHeartbeat > 5000) {
                System.out.println("Player timeout: " + entry.getValue().playerId);
                it.remove();
                if (entry.getValue().isHost) {
                    hostPlayerId = null;
                }
                broadcastPlayerDisconnected(entry.getValue().playerId);
            }
        }
    }

    private void sendResponse(DatagramPacket original, byte packetType, String message) {
        try {
            ByteBuffer bb = ByteBuffer.allocate(256);
            bb.put(packetType);
            writeString(bb, message);

            byte[] data = new byte[bb.position()];
            bb.flip();
            bb.get(data);

            DatagramPacket response = new DatagramPacket(data, data.length, original.getAddress(), original.getPort());
            socket.send(response);
        } catch (Exception e) {
            System.err.println("Send response error: " + e.getMessage());
        }
    }

    private void writeString(ByteBuffer bb, String str) {
        byte[] bytes = str.getBytes();
        bb.putShort((short) bytes.length);
        bb.put(bytes);
    }

    private String readString(ByteBuffer bb) {
        short len = bb.getShort();
        byte[] data = new byte[len];
        bb.get(data);
        return new String(data);
    }

    public void stop() {
        running = false;
        scheduler.shutdownNow();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
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

    public static void main(String[] args) {
        try {
            NetworkGameServer server = new NetworkGameServer();
            server.start();
            new Scanner(System.in).nextLine();
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}