package red;

public interface GameController {
    void connect(int playerId);

    // Apariencia del jugador (género + estilo)
    void appearance(int playerId, String genero, String estilo);

    // ✅ ahora Start lleva seed + nivel
    void start(long seed, int nivel);

    void updatePlayerPosition(int playerId, float x, float y);

    void updateRoom(String habitacionId);

    // ===== Items server-driven =====
    void spawnItem(int itemId, String tipo, float x, float y);
    void despawnItem(int itemId);
    void pickupItem(int jugadorId, int itemId, String tipo);

    // ===== HUD / Inventario server-driven =====
    // Hud:playerId:vida:vidaMax:tiposCsv
    void hud(int playerId, int vida, int vidaMax, String tiposCsv);

    // ===== MVP: UI del otro jugador (solo vida) =====
    // Other:otherPlayerId:vida:vidaMax
    void other(int otherPlayerId, int vida, int vidaMax);

    // ===== Enemigos server-driven =====
    // SpawnEnemy:id:nombre:x:y:sala
    void spawnEnemy(int enemyId, String nombre, float x, float y, String sala);
    void updateEnemy(int enemyId, float x, float y);
    void despawnEnemy(int enemyId);

    // ===== Sala despejada server-driven =====
    // RoomClear:sala
    void roomClear(String sala);

    // ===== Daño server-driven =====
    // Damage:playerId:vida:vidaMax
    void damage(int playerId, int vida, int vidaMax);
    void dead(int playerId);

    // GameOver:loserId
    void gameOver(int loserId);

    void disconnect(String reason);
}
