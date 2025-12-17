package juego.eventos;

import mapa.puertas.DatosPuerta;

public record EventoPuerta(DatosPuerta puerta, int jugadorId) implements EventoJuego {
    @Override
    public TipoEvento tipo() {
        return TipoEvento.PUERTA;
    }
}
