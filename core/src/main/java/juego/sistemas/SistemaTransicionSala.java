package juego.sistemas;

import java.util.function.BiConsumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.physics.box2d.World;

import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.GestorDeEntidades;
import entidades.enemigos.EnemigosDesdeTiled;
import juego.eventos.ColaEventos;
import mapa.generacion.DisposicionMapa;
import mapa.model.Habitacion;
import juego.eventos.EventoPuerta;
import red.RedPartidaCliente;

/**
 * Maneja el cambio de sala por puertas y el cooldown anti "ping-pong".
 * OFFLINE: teletransporta local
 * ONLINE: avisa al server con Door y NO teletransporta acá
 */
public final class SistemaTransicionSala {

    private int framesBloqueoPuertas = 0;

    public boolean bloqueoActivo() {
        return framesBloqueoPuertas > 0;
    }

    public void tickCooldown() {
        if (framesBloqueoPuertas > 0) framesBloqueoPuertas--;
    }

    public Habitacion procesarPuertasPendientes(
        Habitacion salaActual,
        ColaEventos eventos,
        ControlPuzzlePorSala controlPuzzle,
        GestorSalas gestorSalas,
        DisposicionMapa disposicion,
        BiConsumer<Habitacion, Habitacion> notificarCambioSala,
        TiledMap mapaTiled,
        World world,
        GestorDeEntidades gestorEntidades,
        SistemaSpritesEntidades sprites,
        boolean esOnline,
        RedPartidaCliente redPartida
    ) {

        if (eventos == null || eventos.isEmpty()) return salaActual;

        if (controlPuzzle != null && controlPuzzle.estaBloqueada(salaActual)) {
            eventos.limpiar(EventoPuerta.class);
            return salaActual;
        }

        if (framesBloqueoPuertas > 0) {
            eventos.limpiar(EventoPuerta.class);
            return salaActual;
        }

        EventoPuerta ev = eventos.pollFirst(EventoPuerta.class);
        if (ev == null) return salaActual;

        // ✅ ONLINE: avisar al server y NO teletransportar acá
        if (esOnline) {
            if (redPartida != null) {
                String origen = ev.puerta().origen().name();
                String destino = ev.puerta().destino().name();
                String dir = ev.puerta().direccion().name();

                Gdx.app.log("NET", "PUERTA ONLINE -> enviar Door " + origen + " -> " + destino + " (" + dir + ")");
                redPartida.enviarPuertaOnline(origen, destino, dir);
            } else {
                Gdx.app.log("NET", "PUERTA ONLINE pero redPartida == null (no se envía Door)");
            }

            framesBloqueoPuertas = 15;
            eventos.limpiar(EventoPuerta.class);
            return salaActual;
        }

        // ✅ OFFLINE: comportamiento actual
        Habitacion nueva = gestorSalas.irASalaVecinaPorPuerta(salaActual, ev.puerta(), ev.jugadorId());

        if (nueva != null) {
            Habitacion anterior = salaActual;

            gestorEntidades.eliminarEnemigosDeSala(anterior);

            if (sprites != null) sprites.limpiarSpritesDeEntidadesMuertas();

            salaActual = nueva;
            disposicion.descubrir(salaActual);

            if (notificarCambioSala != null) {
                notificarCambioSala.accept(anterior, salaActual);
            }

            if (controlPuzzle != null) controlPuzzle.alEntrarASala(salaActual);

            EnemigosDesdeTiled.crearEnemigosDesdeMapa(mapaTiled, salaActual, world, gestorEntidades);

            if (controlPuzzle != null && gestorEntidades != null) {
                controlPuzzle.setEnemigosVivos(salaActual, gestorEntidades.getEnemigosDeSala(salaActual).size());
            }
            if (sprites != null) {
                sprites.registrarSpritesDeEnemigosVivos();
                sprites.limpiarSpritesDeEntidadesMuertas();
            }
        }

        framesBloqueoPuertas = 15;
        eventos.limpiar(EventoPuerta.class);
        return salaActual;
    }
}
