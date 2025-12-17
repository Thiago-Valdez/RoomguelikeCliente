package juego.eventos;

/**
 * Interfaz común para todos los eventos del juego.
 *
 * - Hoy se usa como "contrato" y para poder tratarlos de forma unificada.
 * - Mañana sirve como base para ruteo/serialización (red), logging y debugging.
 */
public sealed interface EventoJuego
        permits EventoBoton, EventoPuerta, EventoDanio, EventoPickup, EventoFinNivel, EventoTrampilla {

    TipoEvento tipo();
}
