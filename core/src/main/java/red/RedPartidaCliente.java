package red;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

    @Override
    public void disconnect(String reason) {
        motivoDisconnect = reason;
    }
}
