package juego.eventos;

import mapa.botones.DatosBoton;

public record EventoBoton(DatosBoton boton, int jugadorId, boolean down) implements EventoJuego {
    @Override
    public TipoEvento tipo() {
        return TipoEvento.BOTON;
    }
}
