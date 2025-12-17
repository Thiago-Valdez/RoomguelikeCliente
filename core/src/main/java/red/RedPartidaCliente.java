package red;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import control.input.ControlJugador;
import entidades.personajes.Jugador;

public class RedPartidaCliente implements GameController {

    private ClientThread client;

    private boolean modoOnline = false;
    private boolean onlineArrancado = false;
    private int miPlayerId = -1;

    private volatile boolean startRecibido = false;
    private volatile long seedServidor = 0L;
    private volatile int nivelServidor = 1;
    private volatile boolean mundoListo = false;

    // sala
    private volatile String salaPendiente = null;

    // estado red
    private volatile String motivoDisconnect = null;

    // updates
    private final Map<Integer, Vector2> posicionesPendientes = new HashMap<>();

    // “ventanas” para evitar glitches al cambiar de sala
    private volatile int ignorarPosFrames = 0;
    private volatile int teleportFrames = 0;

    // Si tu server es autoritativo con Box2D, lo más correcto es aplicar posiciones con teleport,
    // y NO intentar “seguir” con setLinearVelocity en el cliente (eso genera deslizamiento/enganche).
    private static final boolean TELEPORT_ALWAYS_ONLINE = true;

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

        synchronized (posicionesPendientes) {
            posicionesPendientes.clear();
        }
        salaPendiente = null;
        motivoDisconnect = null;

        ignorarPosFrames = 0;
        teleportFrames = 0;
    }

    public boolean isModoOnline() {
        return modoOnline;
    }

    public boolean consumirStartRecibido() {
        if (!startRecibido) return false;
        startRecibido = false;
        return true;
    }

    public long getSeedServidor() { return seedServidor; }
    public int getNivelServidor() { return nivelServidor; }

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

        Gdx.app.log("NET", ">> " + msg);
        client.sendMessage(msg);
    }

    /** Devuelve el cambio de sala pendiente (si lo hay) y lo limpia. */
    public String consumirCambioSala() {
        if (salaPendiente == null) return null;
        String s = salaPendiente;
        salaPendiente = null;
        return s;
    }

    /**
     * Aplica los UpdatePosition recibidos del server.
     * Regla: en ONLINE el server es la autoridad.
     * - Evitamos “follow” con setLinearVelocity porque genera deslizamiento/enganche.
     * - Aplicamos setTransform + vel=0 (teleport) para representar el estado autoritativo.
     */
    public void aplicarUpdatesPendientes(Jugador jugador1, Jugador jugador2) {
        if (!modoOnline || !mundoListo) return;

        if (motivoDisconnect != null) {
            Gdx.app.log("NET", "Disconnect reason: " + motivoDisconnect);
            motivoDisconnect = null;
        }

        Map<Integer, Vector2> snapshot;
        synchronized (posicionesPendientes) {
            if (posicionesPendientes.isEmpty()) return;
            snapshot = new HashMap<>(posicionesPendientes);
            posicionesPendientes.clear();
        }

        boolean usarTeleport = teleportFrames > 0;
        if (usarTeleport) teleportFrames--;

        for (Map.Entry<Integer, Vector2> e : snapshot.entrySet()) {
            int id = e.getKey();
            Vector2 p = e.getValue();

            Jugador j = (id == 1) ? jugador1 : (id == 2 ? jugador2 : null);
            if (j == null) continue;

            Body b = j.getCuerpoFisico();
            if (b == null) continue;

            // ✅ Posición autoritativa
            b.setTransform(p.x, p.y, b.getAngle());

            // ✅ Cortar cualquier “inercia” local
            b.setLinearVelocity(0f, 0f);
            b.setAwake(true);
        }
    }


    // =====================
    // GameController callbacks desde ClientThread
    // =====================

    @Override
    public void connect(int playerId) {
        this.miPlayerId = playerId;
        this.modoOnline = true;
        Gdx.app.log("NET", "Connected. miPlayerId=" + miPlayerId);
    }

    @Override
    public void start(long seed, int nivel) {
        this.seedServidor = seed;
        this.nivelServidor = nivel;

        synchronized (posicionesPendientes) {
            posicionesPendientes.clear();
        }
        salaPendiente = null;

        this.startRecibido = true;
        this.onlineArrancado = true;

        // input local pausado en online
        ControlJugador.setPausa(true);

        ignorarPosFrames = 0;
        teleportFrames = 2;

        Gdx.app.log("NET", "Start recibido seed=" + seed + " nivel=" + nivel);
    }

    @Override
    public void updatePlayerPosition(int playerId, float x, float y) {
        synchronized (posicionesPendientes) {
            posicionesPendientes.put(playerId, new Vector2(x, y));
        }
    }

    public void onCambioSalaAplicado() {
        // Si todavía querés esta “ventana”, dejala corta para no congelar el sync.

        synchronized (posicionesPendientes) {
            posicionesPendientes.clear();
        }

        // La clave: los primeros updates de posición post-UpdateRoom deben teletransportar
        teleportFrames = 2;
    }

    @Override
    public void updateRoom(String habitacionId) {
        salaPendiente = habitacionId;

        // ✅ Durante 2 frames, aplicar UpdatePosition como TELEPORT (sin smoothing)
        teleportFrames = 2;
    }

    @Override
    public void disconnect(String reason) {
        motivoDisconnect = reason;
    }
}
