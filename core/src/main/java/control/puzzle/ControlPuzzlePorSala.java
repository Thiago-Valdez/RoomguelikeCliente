package control.puzzle;

import java.util.HashMap;
import java.util.Map;

import mapa.model.Habitacion;
import mapa.model.TipoSala;

/**
 * Controla el “puzzle de botones” por sala:
 * - Aplica a ACERTIJO y COMBATE (puertas cerradas hasta cumplir condición)
 * - Estado persistente: si la sala ya fue resuelta, queda desbloqueada para siempre
 * - “Mantenido”: mientras NO esté resuelta, si un jugador suelta su botón, vuelve a bloquearse
 */
public class ControlPuzzlePorSala {

    private static final int MAX_JUGADORES = 2;

    private static class Estado {
        boolean resuelta = false;   // ✅ persistente
        boolean locked = true;      // puertas cerradas
        boolean[] pressed = new boolean[MAX_JUGADORES + 1]; // indices 1..2

        /**
         * Cantidad de enemigos vivos asociados a la sala.
         * Solo se usa para COMBATE/JEFE.
         */
        int enemigosVivos = Integer.MAX_VALUE;
    }

    private final Map<Habitacion, Estado> estados = new HashMap<>();

    private boolean aplica(Habitacion sala) {
        if (sala == null) return false;
        return sala.tipo == TipoSala.ACERTIJO
                || sala.tipo == TipoSala.COMBATE
                || sala.tipo == TipoSala.JEFE;
    }

    private boolean requiereEnemigosMuertos(Habitacion sala) {
        if (sala == null) return false;
        return sala.tipo == TipoSala.COMBATE || sala.tipo == TipoSala.JEFE;
    }

    private void evaluar(Habitacion sala, Estado e) {
        if (sala == null || e == null) return;

        if (e.resuelta) {
            e.locked = false;
            return;
        }

        boolean botonesOk = e.pressed[1] && e.pressed[2];

        switch (sala.tipo) {

            case ACERTIJO -> {
                if (botonesOk) {
                    e.resuelta = true;
                    e.locked = false;
                } else {
                    e.locked = true;
                }
            }

            case COMBATE, JEFE -> {
                // ✅ EXACTAMENTE la misma lógica
                if (botonesOk) {
                    e.resuelta = true;
                    e.locked = false;
                } else {
                    e.locked = true;
                }
            }

            default -> {
                e.resuelta = true;
                e.locked = false;
            }
        }
    }



    private boolean jugadorValido(int jugadorId) {
        return jugadorId >= 1 && jugadorId <= MAX_JUGADORES;
    }

    /** Llamar al entrar a una sala (o al cambiar salaActual) */
    public void alEntrarASala(Habitacion sala) {
        if (!aplica(sala)) return;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());

        // ✅ Si ya fue resuelta, no se vuelve a bloquear nunca
        if (e.resuelta) {
            e.locked = false;
            return;
        }

        // ✅ Si NO fue resuelta, arrancamos bloqueada.
        // Nota: dejamos pressed[] como esté o lo reseteamos:
        // - Si tus spawns siempre ocurren lejos de botones, reseteo es más simple y seguro.
        e.locked = true;
        for (int i = 1; i <= MAX_JUGADORES; i++) {
            e.pressed[i] = false;
        }

        // En COMBATE/JEFE el estado de enemigos se setea desde afuera.
        e.enemigosVivos = Integer.MAX_VALUE;
    }

    public boolean estaBloqueada(Habitacion sala) {
        if (!aplica(sala)) return false;
        Estado e = estados.get(sala);
        // si nunca se inicializó, por seguridad: bloqueada
        return e == null || e.locked;
    }

    /** Se llama al detectar BEGIN contact del jugador con SU botón */
    public boolean botonDown(Habitacion sala, int botonDeJugador) {
        if (!aplica(sala)) return false;
        if (!jugadorValido(botonDeJugador)) return false;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return false;

        e.pressed[botonDeJugador] = true;

        // En ACERTIJO: basta con ambos botones.
        // En COMBATE/JEFE: además se requiere enemigos=0 (lo setea setEnemigosVivos).
        boolean antes = e.resuelta;
        evaluar(sala, e);
        return !antes && e.resuelta;
    }

    /** Se llama al detectar END contact del jugador con SU botón */
    public void botonUp(Habitacion sala, int botonDeJugador) {
        if (!aplica(sala)) return;
        if (!jugadorValido(botonDeJugador)) return;

        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return; // si ya fue resuelta, no importa

        e.pressed[botonDeJugador] = false;

        // ✅ “mantenido”: si suelta alguno antes de resolver, vuelve a bloquear
        evaluar(sala, e);
    }

    /**
     * Actualiza enemigos vivos y re-evalúa condición.
     * Llamar:
     * - al entrar a sala (post spawn desde Tiled)
     * - y/o cada frame (barato, lista chica)
     */
    public void setEnemigosVivos(Habitacion sala, int enemigosVivos) {
        if (!aplica(sala)) return;
        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        if (e.resuelta) return;
        e.enemigosVivos = Math.max(0, enemigosVivos);
        evaluar(sala, e);
    }

    /** Para futuro: desbloquear COMBATE por enemigos muertos, etc. */
    public void marcarResuelta(Habitacion sala) {
        if (!aplica(sala)) return;
        Estado e = estados.computeIfAbsent(sala, k -> new Estado());
        e.resuelta = true;
        e.locked = false;
        for (int i = 1; i <= MAX_JUGADORES; i++) e.pressed[i] = false;
        e.enemigosVivos = 0;
    }

    public boolean estaResuelta(Habitacion sala) {
        if (!aplica(sala)) return true;
        Estado e = estados.get(sala);
        return e != null && e.resuelta;
    }
}
