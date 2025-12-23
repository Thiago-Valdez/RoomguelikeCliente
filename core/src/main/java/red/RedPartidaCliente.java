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

    // ===== Updates =====
    private final Map<Integer, Vector2> posicionesPendientes = new HashMap<>();

    // ===== Ventanas anti-glitch =====
    private volatile int ignorarPosFrames = 0; // (lo dejé, aunque hoy no lo uses)
    private volatile int teleportFrames = 0;

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

        synchronized (posicionesPendientes) {
            posicionesPendientes.clear();
        }

        salaPendiente = null;
        motivoDisconnect = null;

        ignorarPosFrames = 0;
        teleportFrames = 0;
    }

    // =====================
    // Getters / consume
    // =====================

    public boolean isModoOnline() {
        return modoOnline;
    }

    public long getSeedServidor() { return seedServidor; }
    public int getNivelServidor() { return nivelServidor; }

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

    // =====================
    // Envíos al server
    // =====================

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

        Map<Integer, Vector2> snapshot;
        synchronized (posicionesPendientes) {
            if (posicionesPendientes.isEmpty()) return;
            snapshot = new HashMap<>(posicionesPendientes);
            posicionesPendientes.clear();
        }

        boolean usarTeleport = teleportFrames > 0;
        if (usarTeleport) teleportFrames--;

        // (misma lógica: setTransform + vel=0)
        for (Map.Entry<Integer, Vector2> e : snapshot.entrySet()) {
            int id = e.getKey();
            Vector2 p = e.getValue();

            Jugador j = (id == 1) ? jugador1 : (id == 2 ? jugador2 : null);
            if (j == null) continue;

            Body b = j.getCuerpoFisico();
            if (b == null) continue;

            b.setTransform(p.x, p.y, b.getAngle());
            b.setLinearVelocity(0f, 0f);
            b.setAwake(true);
        }
    }

    public void onCambioSalaAplicado() {
        synchronized (posicionesPendientes) {
            posicionesPendientes.clear();
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

        Gdx.app.log(TAG, "Start recibido seed=" + seed + " nivel=" + nivel);
    }

    @Override
    public void updatePlayerPosition(int playerId, float x, float y) {
        synchronized (posicionesPendientes) {
            posicionesPendientes.put(playerId, new Vector2(x, y));
        }
    }

    @Override
    public void updateRoom(String habitacionId) {
        salaPendiente = habitacionId;
        teleportFrames = 2;
    }

    @Override
    public void disconnect(String reason) {
        motivoDisconnect = reason;
    }
}
