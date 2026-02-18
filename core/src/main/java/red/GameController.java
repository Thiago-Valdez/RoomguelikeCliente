package red;

public interface GameController {
    void disconnect(String reason);

    void updatePlayerPosition(int playerId, float x, float y);

    void updateRoom(String habitacionId);

    void connect(int playerId);

    void dead(int playerId);

    void despawnEnemy(int enemyId);

    void despawnItem(int itemId);

    void pickupItem(int jugadorId, int itemId, String tipo);

    void updateEnemy(int enemyId, float x, float y);

    // ===== Daño server-driven =====
    // Damage:playerId:vida:vidaMax
    void damage(int playerId, int vida, int vidaMax);

    // ===== Enemigos server-driven =====
    // SpawnEnemy:id:nombre:x:y:sala
    void spawnEnemy(int enemyId, String nombre, float x, float y, String sala);

    // ===== HUD / Inventario server-driven =====
    // Hud:playerId:vida:vidaMax:tiposCsv
    void hud(int playerId, int vida, int vidaMax, String tiposCsv);

    // ===== Items server-driven =====
    void spawnItem(int itemId, String tipo, float x, float y);

    // ===== MVP: UI del otro jugador (solo vida) =====
    // Other:otherPlayerId:vida:vidaMax
    void other(int otherPlayerId, int vida, int vidaMax);

    // ===== Sala despejada server-driven =====
    // RoomClear:sala
    void roomClear(String sala);

    // Apariencia del jugador (género + estilo)
    void appearance(int playerId, String genero, String estilo);

    // GameOver:loserId
    void gameOver(int loserId);

    // ✅ ahora Start lleva seed + nivel
    void start(long seed, int nivel);
}