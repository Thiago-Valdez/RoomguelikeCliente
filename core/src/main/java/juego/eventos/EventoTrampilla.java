package juego.eventos;

import mapa.trampilla.DatosTrampilla;

/**
 * Evento encolado cuando un jugador entra a la trampilla.
 * Importante: se procesa fuera del ContactListener.
 */
public record EventoTrampilla(DatosTrampilla trampilla, int jugadorId) implements EventoJuego {
    @Override
    public TipoEvento tipo() {
        return TipoEvento.TRAMPILLA;
    }
}
