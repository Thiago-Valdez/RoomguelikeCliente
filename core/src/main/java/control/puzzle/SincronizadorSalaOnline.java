package control.puzzle;

import mapa.model.Habitacion;

/**
 * En ONLINE, fuerza coherencia de estado por sala cuando cambia la sala global.
 * No cambia la lógica del puzzle: solo llama alEntrarASala() en el momento correcto.
 */
public final class SincronizadorSalaOnline {

    private Habitacion ultimaSala = null;

    /** Llamar al aplicar un cambio de sala (UpdateRoom). */
    public void onSalaCambiada(Habitacion salaActual, boolean esOnline, ControlPuzzlePorSala controlPuzzle) {
        if (!esOnline) return;
        if (controlPuzzle == null) return;
        if (salaActual == null) return;

        // Evita resetear varias veces si llega el mismo UpdateRoom o se llama doble.
        if (ultimaSala == salaActual) return;

        ultimaSala = salaActual;
        controlPuzzle.alEntrarASala(salaActual);
    }

    /** Si reiniciás partida/nivel, podés resetear el sync. */
    public void reset() {
        ultimaSala = null;
    }
}
