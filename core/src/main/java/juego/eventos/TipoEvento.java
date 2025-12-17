package juego.eventos;

/**
 * Identificador estable de tipos de evento.
 * Útil para logging/telemetría y futuro ruteo/serialización de eventos (red).
 */
public enum TipoEvento {
    BOTON,
    PUERTA,
    DANIO,
    PICKUP,
    FIN_NIVEL,
    TRAMPILLA
}
