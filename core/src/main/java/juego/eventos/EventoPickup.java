package juego.eventos;

import entidades.items.Item;

public record EventoPickup(Item item, int jugadorId) implements EventoJuego {
    @Override
    public TipoEvento tipo() {
        return TipoEvento.PICKUP;
    }
}
