package juego.eventos;

import mapa.model.Habitacion;

public record EventoFinNivel(Habitacion sala) implements EventoJuego {
    @Override
    public TipoEvento tipo() {
        return TipoEvento.FIN_NIVEL;
    }
}
