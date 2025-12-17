package entidades.items;

import entidades.personajes.Jugador;

import java.util.function.Consumer;

public class Item {

    private final String nombre;
    private final ItemTipo tipo;
    private final Consumer<Jugador> efecto;

    public Item(String nombre, ItemTipo tipo, Consumer<Jugador> efecto) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.efecto = efecto;
    }

    public String getNombre() {
        return nombre;
    }

    public ItemTipo getTipo() {
        return tipo;
    }

    /** 👇 ESTE MÉTODO ES CLAVE */
    public void aplicarModificacion(Jugador jugador) {
        if (jugador == null || efecto == null) return;
        efecto.accept(jugador);
    }
}
