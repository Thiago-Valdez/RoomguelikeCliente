package red;

import java.io.IOException;
import java.net.*;

public class ClientThread extends Thread {

    // ===== Constantes =====
    private static final int DEFAULT_SERVER_PORT = 5555;
    private static final String DEFAULT_SERVER_IP = "127.0.0.1";
    private static final int BUFFER_SIZE = 1024;
    private static final String SEP = ":";

    private final GameController controller;

    private DatagramSocket socket;
    private InetAddress serverIP;
    private int serverPort = DEFAULT_SERVER_PORT;

    private volatile boolean running = true;

    public ClientThread(GameController controller) {
        super("ClientThread");
        this.controller = controller;

        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            serverIP = InetAddress.getByName(DEFAULT_SERVER_IP); // o tu IP real
        } catch (Exception e) {
            System.out.println("[CLIENT] error creando socket: " + e.getMessage());
        }
    }

    public void close() {
        running = false;
        DatagramSocket s = socket; // snapshot
        if (s != null && !s.isClosed()) {
            s.close(); // hará que receive() salga con SocketException (normal)
        }
    }

    @Override
    public void run() {
        System.out.println("[CLIENT] run() arrancó");

        try {
            sendMessage("Connect");

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running) {
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                if (!msg.isEmpty()) parseMessage(msg, packet);
            }

        } catch (SocketException se) {
            // normal si cerraste el socket
        } catch (IOException e) {
            controller.disconnect("IO error: " + e.getMessage());
        } catch (Exception e) {
            // ✅ evita que el thread muera por parseos inesperados
            controller.disconnect("Unexpected error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        System.out.println("[CLIENT] -> " + message + " a " + serverIP + ":" + serverPort);

        try {
            if (socket == null || socket.isClosed()) return;

            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverIP, serverPort);
            socket.send(packet);
        } catch (IOException ignored) {}
    }

    private void parseMessage(String msg, DatagramPacket packet) {
        String[] parts = split(msg);
        if (parts.length == 0) return;

        switch (parts[0]) {

            case "Connected": {
                if (parts.length >= 2) {
                    // ✅ conservar lógica: aprende IP real del server
                    serverIP = packet.getAddress();

                    Integer playerId = parseInt(parts[1]);
                    if (playerId != null) controller.connect(playerId);
                }
                break;
            }

            case "Start": {
                long seed = (parts.length >= 2) ? parseLongOr(parts[1], 0L) : 0L;
                int nivel = (parts.length >= 3) ? parseIntOr(parts[2], 1) : 1;

                System.out.println("[CLIENT] <- Start seed=" + seed + " nivel=" + nivel);
                controller.start(seed, nivel);
                break;
            }

            case "UpdatePosition": {
                if (parts.length >= 4) {
                    Integer id = parseInt(parts[1]);
                    Float x = parseFloat(parts[2]);
                    Float y = parseFloat(parts[3]);
                    if (id != null && x != null && y != null) {
                        controller.updatePlayerPosition(id, x, y);
                    }
                }
                break;
            }

            case "UpdateRoom": {
                // ✅ LOG para confirmar recepción
                System.out.println("[CLIENT] <- " + msg);

                // misma lógica que tenías:
                if (parts.length >= 4) {
                    controller.updateRoom(parts[1] + SEP + parts[2] + SEP + parts[3]);
                } else if (parts.length >= 2) {
                    controller.updateRoom(parts[1]);
                }
                break;
            }

            case "Disconnect": {
                controller.disconnect("Server closed");
                break;
            }

            default:
                // ignorar desconocidos (sin romper)
                break;
        }
    }

    // ===== Helpers =====

    private static String[] split(String msg) {
        return msg.split(SEP);
    }

    private static Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static Float parseFloat(String s) {
        try { return Float.parseFloat(s); } catch (Exception e) { return null; }
    }

    private static long parseLongOr(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }
}
