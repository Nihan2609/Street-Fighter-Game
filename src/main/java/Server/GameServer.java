package Server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

public class GameServer {

    private static final int PORT = 5555;
    private static ConcurrentHashMap<String, PlayerInfo> players = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(PORT);
        System.out.println("Starting Game Server on port " + PORT);

        byte[] buffer = new byte[1024];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            serverSocket.receive(packet);

            String data = new String(packet.getData(), 0, packet.getLength());

            // --- ATTACK packet ---
            if (data.startsWith("ATTACK")) {
                String[] parts = data.split(",");
                String attacker = parts[1];

                PlayerInfo attackerInfo = players.values().stream()
                        .filter(p -> p.username.equals(attacker))
                        .findFirst()
                        .orElse(null);

                if (attackerInfo != null) {
                    for (PlayerInfo other : players.values()) {
                        if (!other.username.equals(attacker)) {
                            double dx = Math.abs(other.x - attackerInfo.x);
                            double dy = Math.abs(other.y - attackerInfo.y);
                            if (dx < 50 && dy < 50) {
                                other.health -= 10; // 10 HP damage
                                if (other.health < 0) other.health = 0;
                            }
                        }
                    }
                }
            }
            // --- MOVEMENT/STATE packet ---
            else {
                // Expecting data: username,x,y,health
                String[] parts = data.split(",");
                if (parts.length < 4) continue; // invalid packet

                String username = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double health = Double.parseDouble(parts[3]);

                String playerKey = packet.getAddress().toString() + ":" + packet.getPort();
                boolean isNewPlayer = !players.containsKey(playerKey);

                // Update or add player
                players.put(playerKey, new PlayerInfo(username, packet.getAddress(), packet.getPort(), x, y, health));

                if (isNewPlayer) {
                    System.out.println(username + " joined the game.");
                }
            }

            // --- Broadcast all players ---
            for (PlayerInfo p : players.values()) {
                StringBuilder sb = new StringBuilder();
                for (PlayerInfo other : players.values()) {
                    sb.append(other.username)
                            .append(",")
                            .append(other.x)
                            .append(",")
                            .append(other.y)
                            .append(",")
                            .append(other.health)
                            .append(";");
                }
                byte[] sendData = sb.toString().getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, p.address, p.port);
                serverSocket.send(sendPacket);
            }
        }
    }

    static class PlayerInfo {
        String username;
        InetAddress address;
        int port;
        double x, y;
        double health;

        public PlayerInfo(String username, InetAddress address, int port, double x, double y, double health) {
            this.username = username;
            this.address = address;
            this.port = port;
            this.x = x;
            this.y = y;
            this.health = health;
        }
    }
}
