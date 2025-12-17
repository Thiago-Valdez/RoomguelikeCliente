package juego.eventos;

public record EventoDanio(int jugadorId, float ex, float ey) implements EventoJuego {
    @Override
    public TipoEvento tipo() {
        return TipoEvento.DANIO;
    }
}

