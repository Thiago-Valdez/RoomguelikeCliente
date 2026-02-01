package red;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.physics.box2d.Body;

import control.input.ControlJugador;
import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.items.ItemTipo;
import entidades.personajes.Jugador;
import mapa.model.Habitacion;

public class RedPartidaCliente implements GameController {

    private static final String TAG = "NET";
    private static final boolean TELEPORT_ALWAYS_ONLINE = true; // (lo dejé por compat aunque hoy no lo uses)

    private ClientThread client;

    // ===== Estado online =====
    private boolean modoOnline = false;
    private boolean onlineArrancado = false;
    private int miPlayerId = -1;

    // ===== Start/seed/nivel =====
    private volatile boolean startRecibido = false;
    private volatile long seedServidor = 0L;
    private volatile int nivelServidor = 1;
    private volatile boolean mundoListo = false;

    // ===== Sala pendiente =====
    private volatile String salaPendiente = null;

    // ===== Disconnect =====
    private volatile String motivoDisconnect = null;

    // ===== GameOver (server-driven) =====
    private volatile boolean gameOverRecibido = false;
    private volatile int loserId = -1;

    // ===== Snapshots para interpolación (cliente visual) =====
    private static final int MAX_SAMPLES_PER_PLAYER = 10;
    private static final long INTERP_DELAY_MS = 100; // delay visual para interpolar (buffer)

    private static final class Sample {
        final float x;
        final float y;
        final long tMs;

        Sample(float x, float y, long tMs) {
            this.x = x;
            this.y = y;
            this.tMs = tMs;
        }
    }

    // playerId -> cola de samples (ordenados por llegada)
    private final Map<Integer, Deque<Sample>> samplesPorJugador = new HashMap<>();

    // ===== Ventanas anti-glitch =====
    private volatile int ignorarPosFrames = 0; // (lo dejé, aunque hoy no lo uses)
    private volatile int teleportFrames = 0;

    // =====================
    // Items (server-driven)
    // =====================
    private static final class SpawnItemEv {
        final int itemId;
        final String tipo;
        final float x;
        final float y;
        SpawnItemEv(int itemId, String tipo, float x, float y) {
            this.itemId = itemId;
            this.tipo = tipo;
            this.x = x;
            this.y = y;
        }
    }

    private final Object lockItems = new Object();
    private final ArrayDeque<SpawnItemEv> spawnItemsPendientes = new ArrayDeque<>();
    private final ArrayDeque<Integer> despawnItemsPendientes = new ArrayDeque<>();

    // =====================
    // Enemigos (server-driven)
    // =====================
    private static final class SpawnEnemyEv {
        final int enemyId;
        final String nombre;
        final float x;
        final float y;
        final String sala;
        SpawnEnemyEv(int enemyId, String nombre, float x, float y, String sala) {
            this.enemyId = enemyId;
            this.nombre = nombre;
            this.x = x;
            this.y = y;
            this.sala = sala;
        }
    }

    private static final class UpdateEnemyEv {
        final int enemyId;
        final float x;
        final float y;
        UpdateEnemyEv(int enemyId, float x, float y) {
            this.enemyId = enemyId;
            this.x = x;
            this.y = y;
        }
    }

    private final Object lockEnemies = new Object();
    private final ArrayDeque<SpawnEnemyEv> spawnEnemiesPendientes = new ArrayDeque<>();
    private final ArrayDeque<UpdateEnemyEv> updateEnemiesPendientes = new ArrayDeque<>();
    private final ArrayDeque<Integer> despawnEnemiesPendientes = new ArrayDeque<>();

    // =====================
    // HUD / Inventario (server-driven)
    // =====================
    private static final class HudEv {
        final int playerId;
        final int vida;
        final int vidaMax;
        final String tiposCsv;

        HudEv(int playerId, int vida, int vidaMax, String tiposCsv) {
            this.playerId = playerId;
            this.vida = vida;
            this.vidaMax = vidaMax;
            this.tiposCsv = (tiposCsv != null) ? tiposCsv : "";
        }
    }

    private final Object lockHud = new Object();
    private final ArrayDeque<HudEv> hudPendiente = new ArrayDeque<>();

    // =====================
    // MVP: UI del otro jugador (solo vida)
    // =====================
    private static final class OtherEv {
        final int otherId;
        final int vida;
        final int vidaMax;

        OtherEv(int otherId, int vida, int vidaMax) {
            this.otherId = otherId;
            this.vida = vida;
            this.vidaMax = vidaMax;
        }
    }

    private final Object lockOther = new Object();
    private final ArrayDeque<OtherEv> otherPendiente = new ArrayDeque<>();

    private volatile int otherPlayerId = -1;
    private volatile int otherVida = 0;
    private volatile int otherVidaMax = 0;

    // =====================
    // Config / lifecycle
    // =====================

    public void setClient(ClientThread client) {
        this.client = client;
        this.modoOnline = (client != null);

        if (!modoOnline) limpiarEstadoOnline();
    }

    public void setModoOnline(boolean online) {
        this.modoOnline = online;
        if (!online) limpiarEstadoOnline();
    }

    public void setMundoListo(boolean listo) {
        this.mundoListo = listo;
    }

    private void limpiarEstadoOnline() {
        ControlJugador.setPausa(false);

        onlineArrancado = false;
        miPlayerId = -1;

        startRecibido = false;
        seedServidor = 0L;
        nivelServidor = 1;

        synchronized (samplesPorJugador) {
            samplesPorJugador.clear();
        }

        salaPendiente = null;
        motivoDisconnect = null;
        gameOverRecibido = false;
        loserId = -1;

        ignorarPosFrames = 0;
        teleportFrames = 0;

        synchronized (lockItems) {
            spawnItemsPendientes.clear();
            despawnItemsPendientes.clear();
        }

        synchronized (lockEnemies) {
            spawnEnemiesPendientes.clear();
            updateEnemiesPendientes.clear();
            despawnEnemiesPendientes.clear();
        }

        synchronized (lockHud) {
            hudPendiente.clear();
        }

        synchronized (lockOther) {
            otherPendiente.clear();
        }

        otherPlayerId = -1;
        otherVida = 0;
        otherVidaMax = 0;
    }

    // =====================
    // Getters / consume
    // =====================

    public boolean isModoOnline() {
        return modoOnline;
    }

    public long getSeedServidor() { return seedServidor; }
    public int getNivelServidor() { return nivelServidor; }
    public int getMiPlayerId() { return miPlayerId; }

    // MVP: estado del otro jugador (solo vida)
    public int getOtherPlayerId() { return otherPlayerId; }
    public int getOtherVida() { return otherVida; }
    public int getOtherVidaMax() { return otherVidaMax; }

    public boolean consumirStartRecibido() {
        if (!startRecibido) return false;
        startRecibido = false;
        return true;
    }

    /** Devuelve el cambio de sala pendiente (si lo hay) y lo limpia. */
    public String consumirCambioSala() {
        if (salaPendiente == null) return null;
        String s = salaPendiente;
        salaPendiente = null;
        return s;
    }

    public int consumirGameOverLoserId() {
        if (!gameOverRecibido) return -1;
        gameOverRecibido = false;
        return loserId;
    }

    // =====================
    // Envíos al server
    // =====================


public void enviarRoomClearReq(mapa.model.Habitacion sala) {
    if (!modoOnline || !onlineArrancado || client == null || sala == null) return;
    client.sendMessage("RoomClearReq:" + sala.name());
}

public void enviarInputOnline(boolean opcionesAbiertas, boolean gameOverSolicitado) {
        if (!modoOnline || !onlineArrancado || client == null) return;
        if (opcionesAbiertas || gameOverSolicitado) return;

        int dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1;

        client.sendMessage("Move:" + dx + ":" + dy);
    }

    // ✅ Mantengo compat: con playerId o sin playerId (server acepta ambos)
    public void enviarPuertaOnline(String origen, String destino, String dir) {
        if (!modoOnline || !onlineArrancado || client == null) return;

        final String msg;
        if (miPlayerId > 0) msg = "Door:" + miPlayerId + ":" + origen + ":" + destino + ":" + dir;
        else msg = "Door:" + origen + ":" + destino + ":" + dir;

        Gdx.app.log(TAG, ">> " + msg);
        client.sendMessage(msg);
    }

    // =====================
    // Aplicación de updates
    // =====================

    public void aplicarUpdatesPendientes(Jugador jugador1, Jugador jugador2) {
        if (!modoOnline || !mundoListo) return;

        if (motivoDisconnect != null) {
            Gdx.app.log(TAG, "Disconnect reason: " + motivoDisconnect);
            motivoDisconnect = null;
        }

        // ✅ HUD/inventario server-driven: aplicar antes del render.
        aplicarHudPendiente(jugador1, jugador2);

        // ✅ MVP: estado del otro jugador (solo vida)
        aplicarOtherPendiente();

        final long nowMs = System.currentTimeMillis();
        final long renderTimeMs = nowMs - INTERP_DELAY_MS;

        // Si venimos de un cambio de sala, preferimos "snap" a la última muestra
        final boolean snap = (teleportFrames > 0);
        if (snap) teleportFrames--;

        // Aplicamos interpolación SOLO visual: movemos los cuerpos al punto interpolado
        // (en online, el cliente no debería depender de la física local para gameplay).
        aplicarInterpoladoParaJugador(1, jugador1, renderTimeMs, snap);
        aplicarInterpoladoParaJugador(2, jugador2, renderTimeMs, snap);
    }

    /**
     * Aplica eventos de items recibidos por red sobre el GestorDeEntidades.
     * En online el cliente NO decide pickups: solo spawnea/despawnea lo que diga el server.
     */
    public void aplicarEventosItems(GestorDeEntidades gestorEntidades) {
        if (!modoOnline || !mundoListo || gestorEntidades == null) return;

        synchronized (lockItems) {
            while (!spawnItemsPendientes.isEmpty()) {
                SpawnItemEv ev = spawnItemsPendientes.pollFirst();
                if (ev == null) continue;

                ItemTipo tipo = null;
                try { tipo = ItemTipo.valueOf(ev.tipo); } catch (Exception ignored) {}
                if (tipo == null) continue;

                gestorEntidades.spawnItemOnline(ev.itemId, tipo, ev.x, ev.y);
            }

            while (!despawnItemsPendientes.isEmpty()) {
                Integer id = despawnItemsPendientes.pollFirst();
                if (id == null) continue;
                gestorEntidades.despawnItemOnline(id);
            }
        }
    }

    /**
     * Aplica eventos de enemigos recibidos por red.
     * En online el cliente NO simula IA: solo crea/actualiza los enemigos que manda el server.
     */

public void aplicarRoomClearPendiente(control.puzzle.ControlPuzzlePorSala controlPuzzle,
                                     juego.sistemas.SistemaSpritesEntidades sprites,
                                     mapa.model.Habitacion salaActual) {
    if (!modoOnline) return;
    RoomClearEv ev = null;
    synchronized (lockRoomClear) {
        if (!roomClearPendientes.isEmpty()) ev = roomClearPendientes.pollFirst();
    }
    if (ev == null) return;

    mapa.model.Habitacion sala = null;
    try { sala = mapa.model.Habitacion.valueOf(ev.sala); } catch (Exception ignored) {}
    if (sala == null) return;

    // ✅ Marca resuelta (desbloquea puertas visuales y persiste estado)
    if (controlPuzzle != null) controlPuzzle.marcarResuelta(sala);

    // ✅ Mata enemigos visuales de esa sala (anim) si existe sprites
    if (sprites != null) sprites.matarEnemigosDeSalaConAnim(sala);
}

public void aplicarEventosEnemigos(GestorDeEntidades gestorEntidades) {
        if (!modoOnline || !mundoListo || gestorEntidades == null) return;

        synchronized (lockEnemies) {
            while (!spawnEnemiesPendientes.isEmpty()) {
                SpawnEnemyEv ev = spawnEnemiesPendientes.pollFirst();
                if (ev == null) continue;

                Habitacion sala = null;
                try { sala = Habitacion.valueOf(ev.sala); } catch (Exception ignored) {}
                gestorEntidades.spawnEnemyOnline(ev.enemyId, ev.nombre, sala, ev.x, ev.y);
            }

            while (!updateEnemiesPendientes.isEmpty()) {
                UpdateEnemyEv ev = updateEnemiesPendientes.pollFirst();
                if (ev == null) continue;
                gestorEntidades.updateEnemyOnline(ev.enemyId, ev.x, ev.y);
            }

            while (!despawnEnemiesPendientes.isEmpty()) {
                Integer id = despawnEnemiesPendientes.pollFirst();
                if (id == null) continue;
                gestorEntidades.despawnEnemyOnline(id);
            }
        }
    }

    private void aplicarHudPendiente(Jugador jugador1, Jugador jugador2) {
        synchronized (lockHud) {
            while (!hudPendiente.isEmpty()) {
                HudEv ev = hudPendiente.pollFirst();
                if (ev == null) continue;

                // ✅ Cada cliente muestra SU HUD. Ignoramos HUD del otro jugador.
                if (miPlayerId > 0 && ev.playerId != miPlayerId) {
                    continue;
                }

                Jugador j = (ev.playerId == 1) ? jugador1 : (ev.playerId == 2) ? jugador2 : null;
                if (j == null) continue;

                // Vida
                j.setVidaMaxima(ev.vidaMax);
                j.setVida(ev.vida);

                // Inventario: lista de ItemTipo separada por ',' (puede venir vacía)
                j.setInventarioRemoto(ev.tiposCsv);
            }
        }
    }

    private void aplicarOtherPendiente() {
        synchronized (lockOther) {
            while (!otherPendiente.isEmpty()) {
                OtherEv ev = otherPendiente.pollFirst();
                if (ev == null) continue;

                // guardamos para que el HUD lo muestre (no toca gameplay)
                otherPlayerId = ev.otherId;
                otherVida = ev.vida;
                otherVidaMax = ev.vidaMax;
            }
        }
    }

    private void aplicarInterpoladoParaJugador(int id, Jugador jugador, long renderTimeMs, boolean snap) {
        if (jugador == null) return;
        Body b = jugador.getCuerpoFisico();
        if (b == null) return;

        Sample a = null;
        Sample c = null;

        synchronized (samplesPorJugador) {
            Deque<Sample> q = samplesPorJugador.get(id);
            if (q == null || q.isEmpty()) return;

            // En modo "snap" tomamos la última muestra y limpiamos el resto.
            if (snap) {
                Sample last = q.peekLast();
                q.clear();
                q.addLast(last);
                a = last;
            } else {
                // Descarta muestras demasiado viejas (para mantener cola chica y evitar interpolar con basura)
                while (q.size() > 2 && q.peekFirst().tMs < renderTimeMs - 1000) {
                    q.pollFirst();
                }

                // Queremos dos muestras que enmarquen renderTimeMs.
                // Como llegan ordenadas por tiempo de llegada, recorremos desde el inicio.
                Sample prev = null;
                for (Sample s : q) {
                    if (s.tMs <= renderTimeMs) prev = s;
                    if (s.tMs >= renderTimeMs) {
                        c = s;
                        break;
                    }
                }

                if (prev == null) {
                    // Todavía no tenemos una muestra <= renderTime, usamos la primera disponible
                    a = q.peekFirst();
                } else {
                    a = prev;
                }

                if (c == null) {
                    // No hay muestra futura, usamos la última (extrapolación cero)
                    c = q.peekLast();
                }
            }
        }

        // Si solo tenemos una muestra útil
        if (a == null || c == null) return;

        float x;
        float y;

        if (snap || a == c || a.tMs == c.tMs) {
            x = c.x;
            y = c.y;
        } else {
            float alpha = (float) (renderTimeMs - a.tMs) / (float) (c.tMs - a.tMs);
            if (alpha < 0f) alpha = 0f;
            if (alpha > 1f) alpha = 1f;
            x = a.x + (c.x - a.x) * alpha;
            y = a.y + (c.y - a.y) * alpha;
        }

        b.setTransform(x, y, b.getAngle());
        b.setLinearVelocity(0f, 0f);
        b.setAwake(true);
    }

    public void onCambioSalaAplicado() {
        // Limpiamos buffer para no interpolar entre salas distintas
        synchronized (samplesPorJugador) {
            samplesPorJugador.clear();
        }
        teleportFrames = 2;
    }

    // =====================
    // GameController callbacks desde ClientThread
    // =====================

    @Override
    public void connect(int playerId) {
        this.miPlayerId = playerId;
        this.modoOnline = true;
        Gdx.app.log(TAG, "Connected. miPlayerId=" + miPlayerId);
    }

    @Override
    public void start(long seed, int nivel) {
        this.seedServidor = seed;
        this.nivelServidor = nivel;

        synchronized (samplesPorJugador) {
            samplesPorJugador.clear();
        }
        salaPendiente = null;

        this.startRecibido = true;
        this.onlineArrancado = true;

        // input local pausado en online
        ControlJugador.setPausa(true);

        ignorarPosFrames = 0;
        teleportFrames = 2;

        Gdx.app.log(TAG, "Start recibido seed=" + seed + " nivel=" + nivel);
    }

    @Override
    public void updatePlayerPosition(int playerId, float x, float y) {
        final long nowMs = System.currentTimeMillis();
        synchronized (samplesPorJugador) {
            Deque<Sample> q = samplesPorJugador.computeIfAbsent(playerId, k -> new ArrayDeque<>());
            // Asegura orden no-decreciente por si el reloj cambiara levemente
            if (!q.isEmpty() && nowMs < q.peekLast().tMs) {
                q.addLast(new Sample(x, y, q.peekLast().tMs));
            } else {
                q.addLast(new Sample(x, y, nowMs));
            }
            while (q.size() > MAX_SAMPLES_PER_PLAYER) q.pollFirst();
        }
    }

    @Override
    public void updateRoom(String habitacionId) {
        salaPendiente = habitacionId;
        teleportFrames = 2;
    }

    // =====================
    // Items callbacks (desde ClientThread)
    // =====================

    @Override
    public void spawnItem(int itemId, String tipo, float x, float y) {
        if (!modoOnline) return;
        synchronized (lockItems) {
            spawnItemsPendientes.addLast(new SpawnItemEv(itemId, tipo, x, y));
        }
    }

    @Override
    public void despawnItem(int itemId) {
        if (!modoOnline) return;
        synchronized (lockItems) {
            despawnItemsPendientes.addLast(itemId);
        }
    }

    @Override
    public void pickupItem(int jugadorId, int itemId, String tipo) {
        // Por ahora: el pickup solo implica despawn visual.
        // En el próximo paso vamos a sincronizar inventario/stats/HUD.
        despawnItem(itemId);
    }

    @Override
    public void hud(int playerId, int vida, int vidaMax, String tiposCsv) {
        if (!modoOnline) return;
        synchronized (lockHud) {
            hudPendiente.addLast(new HudEv(playerId, vida, vidaMax, tiposCsv));
            // Mantener cola acotada
            while (hudPendiente.size() > 20) hudPendiente.pollFirst();
        }
    }

    @Override
    public void other(int otherPlayerId, int vida, int vidaMax) {
        if (!modoOnline) return;
        synchronized (lockOther) {
            otherPendiente.addLast(new OtherEv(otherPlayerId, vida, vidaMax));
            while (otherPendiente.size() > 10) otherPendiente.pollFirst();
        }
    }

    // =====================
    // Enemigos callbacks (desde ClientThread)
    // =====================

    @Override
    public void spawnEnemy(int enemyId, String nombre, float x, float y, String sala) {
        if (!modoOnline) return;
        synchronized (lockEnemies) {
            spawnEnemiesPendientes.addLast(new SpawnEnemyEv(enemyId, nombre, x, y, sala));
            while (spawnEnemiesPendientes.size() > 50) spawnEnemiesPendientes.pollFirst();
        }
    }

    @Override
    public void updateEnemy(int enemyId, float x, float y) {
        if (!modoOnline) return;
        synchronized (lockEnemies) {
            updateEnemiesPendientes.addLast(new UpdateEnemyEv(enemyId, x, y));
            while (updateEnemiesPendientes.size() > 200) updateEnemiesPendientes.pollFirst();
        }
    }

    @Override
    public void despawnEnemy(int enemyId) {
        if (!modoOnline) return;
        synchronized (lockEnemies) {
            despawnEnemiesPendientes.addLast(enemyId);
            while (despawnEnemiesPendientes.size() > 50) despawnEnemiesPendientes.pollFirst();
        }
    }

    // =====================
    // Daño callbacks (desde ClientThread)
    // =====================

@Override
public void roomClear(String sala) {
    if (sala == null || sala.isBlank()) return;
    synchronized (lockRoomClear) {
        roomClearPendientes.addLast(new RoomClearEv(sala.trim()));
    }
}

public void damage(int playerId, int vida, int vidaMax) {
        if (!modoOnline) return;

        // Reutilizamos las colas existentes:
        // - Si es mi jugador => va a Hud (porque ese HUD se renderiza)
        // - Si es el otro => va a Other (porque se muestra como UI secundaria)
        if (miPlayerId > 0 && playerId == miPlayerId) {
            hud(playerId, vida, vidaMax, "");
        } else {
            other(playerId, vida, vidaMax);
        }
    }

    @Override
    public void dead(int playerId) {
        // MVP: hoy solo es informativo. El HUD ya reflejará vida=0.
    }

    @Override
    public void gameOver(int loserId) {
        if (!modoOnline) return;
        this.loserId = loserId;
        this.gameOverRecibido = true;

        // cortamos input local cuanto antes
        ControlJugador.setPausa(true);
        Gdx.app.log(TAG, "GAME OVER recibido. loserId=" + loserId);
    }


    @Override
    public void disconnect(String reason) {
        motivoDisconnect = reason;
    }

    // ===== Sala despejada server-driven =====
    private static class RoomClearEv {
        final String sala;
        RoomClearEv(String sala) { this.sala = sala; }
    }
    private final Object lockRoomClear = new Object();
    private final Deque<RoomClearEv> roomClearPendientes = new ArrayDeque<>();

}



