package entidades.items;

import entidades.personajes.Jugador;

import java.util.function.Consumer;

public class Item {
    private final String nombre;

    private final Consumer<Jugador> efecto;

    private final ItemTipo tipo;

    public Item(String nombre, ItemTipo tipo, Consumer<Jugador> efecto) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.efecto = efecto;
    }

    public ItemTipo getTipo() {
        return tipo;
    }

    public String getNombre() {
        return nombre;
    }

    /** 👇 ESTE MÉTODO ES CLAVE */
    public void aplicarModificacion(Jugador jugador) {
        if (jugador == null || efecto == null) return;
        efecto.accept(jugador);
    }
}