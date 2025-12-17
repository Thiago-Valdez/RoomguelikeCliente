package red;

public interface GameController {
    void connect(int playerId);

    // ✅ ahora Start lleva seed + nivel
    void start(long seed, int nivel);

    void updatePlayerPosition(int playerId, float x, float y);

    void updateRoom(String habitacionId);

    void disconnect(String reason);
}
