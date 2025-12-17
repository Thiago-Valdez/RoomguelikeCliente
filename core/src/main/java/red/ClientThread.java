package red;

import java.io.IOException;
import java.net.*;

public class ClientThread extends Thread {

    private final GameController controller;

    private DatagramSocket socket;
    private InetAddress serverIP;
    private int serverPort = 5555;

    private volatile boolean running = true;

    public ClientThread(GameController controller) {
        this.controller = controller;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            serverIP = InetAddress.getByName("127.0.0.1"); // o tu IP real
        } catch (Exception e) {
            System.out.println("[CLIENT] error creando socket: " + e.getMessage());
        }
    }

    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) socket.close();
    }

    @Override
    public void run() {
        System.out.println("[CLIENT] run() arrancó");
        try {
            sendMessage("Connect");

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (running) {
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                parseMessage(msg, packet);
            }

        } catch (SocketException se) {
            // normal si cerraste el socket
        } catch (IOException e) {
            controller.disconnect("IO error: " + e.getMessage());
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
        String[] parts = msg.split(":");
        if (parts.length == 0) return;

        switch (parts[0]) {
            case "Connected": {
                if (parts.length >= 2) {
                    serverIP = packet.getAddress();

                    int playerId = Integer.parseInt(parts[1]);
                    controller.connect(playerId);
                }
                break;
            }

            case "Start": {
                long seed = (parts.length >= 2) ? Long.parseLong(parts[1]) : 0L;
                int nivel = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 1;

                System.out.println("[CLIENT] <- Start seed=" + seed + " nivel=" + nivel);
                controller.start(seed, nivel);
                break;
            }

            case "UpdatePosition": {
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[1]);
                    float x = Float.parseFloat(parts[2]);
                    float y = Float.parseFloat(parts[3]);
                    controller.updatePlayerPosition(id, x, y);
                }
                break;
            }

            case "UpdateRoom": {
                // ✅ LOG para confirmar recepción
                System.out.println("[CLIENT] <- " + msg);

                if (parts.length >= 4) {
                    controller.updateRoom(parts[1] + ":" + parts[2] + ":" + parts[3]);
                } else if (parts.length >= 2) {
                    controller.updateRoom(parts[1]);
                }
                break;
            }

            case "Disconnect": {
                controller.disconnect("Server closed");
                break;
            }
        }
    }
}
