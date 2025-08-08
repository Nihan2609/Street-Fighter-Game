package Server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 5555;
    private static final ExecutorService pool = Executors.newFixedThreadPool(2);

    private static Socket player1Socket = null;
    private static Socket player2Socket = null;

    private static BufferedWriter p1Out = null;
    private static BufferedWriter p2Out = null;

    public static void main(String[] args) {
        System.out.println("Starting Game Server on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running... Waiting for players to connect.");

            // Accept Player 1
            player1Socket = serverSocket.accept();
            System.out.println("Player 1 connected from " + player1Socket.getRemoteSocketAddress());
            p1Out = new BufferedWriter(new OutputStreamWriter(player1Socket.getOutputStream()));

            // Accept Player 2
            player2Socket = serverSocket.accept();
            System.out.println("Player 2 connected from " + player2Socket.getRemoteSocketAddress());
            p2Out = new BufferedWriter(new OutputStreamWriter(player2Socket.getOutputStream()));

            // Start handler threads for both players
            pool.execute(new ClientHandler(player1Socket, p2Out, "Player 1"));
            pool.execute(new ClientHandler(player2Socket, p1Out, "Player 2"));

            System.out.println("Both players connected. Game communication started.");

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void shutdown() {
        try {
            if (p1Out != null) p1Out.close();
            if (p2Out != null) p2Out.close();
            if (player1Socket != null && !player1Socket.isClosed()) player1Socket.close();
            if (player2Socket != null && !player2Socket.isClosed()) player2Socket.close();
            pool.shutdownNow();
            System.out.println("Server shutdown.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private BufferedWriter opponentOut;
        private String playerName;

        public ClientHandler(Socket clientSocket, BufferedWriter opponentOut, String playerName) {
            this.clientSocket = clientSocket;
            this.opponentOut = opponentOut;
            this.playerName = playerName;
            try {
                this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println(playerName + " input stream error: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            String msg;
            try {
                while ((msg = in.readLine()) != null) {
                    System.out.println(playerName + " says: " + msg);
                    // Forward message to opponent
                    synchronized (opponentOut) {
                        opponentOut.write(msg);
                        opponentOut.newLine();
                        opponentOut.flush();
                    }
                }
            } catch (IOException e) {
                System.out.println(playerName + " disconnected: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            try {
                if (in != null) in.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(playerName + " handler stopped.");
        }
    }
}
