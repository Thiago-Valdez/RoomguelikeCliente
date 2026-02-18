package juego.sistemas;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import camara.CamaraDeSala;
import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.GestorDeEntidades;
import juego.eventos.ColaEventos;
import juego.eventos.EventoPuerta;
import entidades.items.Item;
import entidades.personajes.Jugador;
import fisica.FisicaMundo;
import mapa.generacion.DisposicionMapa;
import mapa.model.Habitacion;
import red.RedPartidaCliente;

/**
* Agrupa el update del gameplay (sin render).
* Mantiene a Partida como un orquestador chico y legible.
*/
public final class SistemaActualizacionPartida {
    private final GestorDeEntidades gestorEntidades;

    private final RedPartidaCliente red;

    private final CamaraDeSala camaraSala;

    private final FisicaMundo fisica;

    private final ProcesadorColasEventos procesadorEventos;

    private final SistemaSpritesEntidades sprites;

    private final SistemaTransicionSala transicionSala;

    public Habitacion actualizar(ContextoActualizacionPartida ctx) {
        if (ctx == null) return null;

        float delta = ctx.delta;
        Habitacion salaActual = ctx.salaActual;

        Jugador jugador1 = ctx.jugador1;
        Jugador jugador2 = ctx.jugador2;
        ControlJugador controlJugador1 = ctx.controlJugador1;
        ControlJugador controlJugador2 = ctx.controlJugador2;

        ColaEventos eventos = ctx.eventos;

        Set<Item> itemsYaProcesados = ctx.itemsYaProcesados;
        Set<Integer> jugadoresDanioFrame = ctx.jugadoresDanioFrame;

        List<mapa.botones.BotonVisual> botonesVisuales = ctx.botonesVisuales;

        GestorSalas gestorSalas = ctx.gestorSalas;
        var mapaTiled = ctx.mapaTiled;
        var world = ctx.world;
        RedPartidaCliente redPartida = ctx.redPartida;

        ControlPuzzlePorSala controlPuzzle = ctx.controlPuzzle;
        DisposicionMapa disposicion = ctx.disposicion;
        var notificarCambioSala = ctx.notificarCambioSala;

        if (gestorEntidades == null || fisica == null) return salaActual;

        final boolean esOnline = ctx.esOnline;

        // 1) lógica pura (LOCAL)
        if (!esOnline) {
            gestorEntidades.actualizar(delta, salaActual);
        }

        // 2) input (LOCAL)
        if (!esOnline) {
            actualizarControles(delta, controlJugador1, controlJugador2);
        }

        // 3) IA enemigos (LOCAL)
        if (!esOnline) {
            gestorEntidades.actualizarEnemigos(delta, jugador1, jugador2);
        }

        // 4) físicas
        // En ONLINE idealmente NO hacés step si el server manda posiciones.
        // Pero si tu render/otros sistemas dependen de step (por ejemplo timers/fixtures),
        // lo podés dejar. Yo te lo dejo prendido pero con delta clamp para evitar explosiones.
        float stepDelta = Math.min(delta, 1f / 30f);
        fisica.step(stepDelta);

        // 5) EVENTOS (LOCAL)

        // 5.A) PUERTAS (OFFLINE y ONLINE)
        if (transicionSala != null) {
            transicionSala.tickCooldown();
        }
        if (transicionSala != null && eventos != null && gestorSalas != null && disposicion != null) {
            salaActual = transicionSala.procesarPuertasPendientes(
            salaActual,
            eventos,
            controlPuzzle,
            gestorSalas,
            disposicion,
            notificarCambioSala,
            mapaTiled,
            world,
            gestorEntidades,
            sprites,
            esOnline,
            redPartida
            );
        }

        if (procesadorEventos != null && eventos != null) {

            // 🔘 BOTONES: SIEMPRE (offline y online)
            Consumer<Habitacion> matarEnemigos =
            (!esOnline && sprites != null)
            ? sprites::matarEnemigosDeSalaConAnim
            : (esOnline && red != null)
            ? (s) -> red.enviarRoomClearReq(s)
            : null;

            procesadorEventos.procesarBotonesPendientes(
            eventos,
            salaActual,
            controlPuzzle,
            matarEnemigos,
            botonesVisuales
            );

            if (!esOnline) {
                // 📦 ITEMS: SOLO OFFLINE
                procesadorEventos.procesarItemsPendientes(
                eventos,
                itemsYaProcesados,
                gestorEntidades
                );

                // 💥 DAÑO LOCAL
                procesadorEventos.procesarDaniosPendientes(
                eventos,
                jugadoresDanioFrame,
                gestorEntidades,
                sprites
                );
            } else {
                // ONLINE: limpiamos lo que NO debe existir
                eventos.limpiar(juego.eventos.EventoDanio.class);
                eventos.limpiar(juego.eventos.EventoPickup.class);
                // 🚪 EventoPuerta NO se toca acá (lo maneja SistemaTransicionSala)

                // 📦 Items: spawns/despawns vienen por red (server-driven)
                if (redPartida != null) {
                    redPartida.aplicarEventosItems(gestorEntidades);
                    // 👾 Enemigos: spawns/updates vienen por red (server-driven)
                    redPartida.aplicarEventosEnemigos(gestorEntidades);
                }
            }
        }

        // 6) estado jugadores
        // En ONLINE, si el server maneja vida/inmunidad/etc, no lo corras acá.
        // Si de momento no lo sincronizás por red, podés dejarlo solo en local.
        if (!esOnline) {
            actualizarJugadores(delta, jugador1, jugador2);
        }

        // 7) sprites

        if (sprites != null) {
            sprites.actualizarAnimJugadores(delta);

            sprites.registrarSpritesDeEnemigosVivos();
            sprites.procesarEnemigosEnMuerte();
            sprites.limpiarSpritesDeEntidadesMuertas();
        }

        // 8) cámara
        if (camaraSala != null) {
            camaraSala.update(delta);
        }

        return salaActual;
    }

    private void actualizarControles(float delta, ControlJugador c1, ControlJugador c2) {
        if (c1 != null) c1.actualizar(delta);
        if (c2 != null) c2.actualizar(delta);
    }

    private void actualizarJugadores(float delta, Jugador j1, Jugador j2) {
        if (j1 != null) {
            boolean estabaEnMuerte = j1.estaEnMuerte();
            j1.updateEstado(delta);
            j1.tick(delta);
            if (estabaEnMuerte && !j1.estaEnMuerte() && sprites != null) sprites.detenerMuerte(j1);
        }
        if (j2 != null) {
            boolean estabaEnMuerte = j2.estaEnMuerte();
            j2.updateEstado(delta);
            j2.tick(delta);
            if (estabaEnMuerte && !j2.estaEnMuerte() && sprites != null) sprites.detenerMuerte(j2);
        }
    }

    public SistemaActualizacionPartida(
    GestorDeEntidades gestorEntidades,
    FisicaMundo fisica,
    CamaraDeSala camaraSala,
    SistemaTransicionSala transicionSala,
    ProcesadorColasEventos procesadorEventos,
    SistemaSpritesEntidades sprites,
    RedPartidaCliente red
    ) {
        this.gestorEntidades = gestorEntidades;
        this.fisica = fisica;
        this.camaraSala = camaraSala;
        this.transicionSala = transicionSala;
        this.procesadorEventos = procesadorEventos;
        this.sprites = sprites;
        this.red = red;
    }
}