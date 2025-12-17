package juego.eventos;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Cola unificada para eventos del juego.
 *
 * Nota: por ahora el proyecto todavía usa colas tipadas por evento en algunos sistemas.
 * Esta clase se agrega para poder migrar gradualmente a un único flujo de eventos
 * (muy útil para red/snapshots/replay) sin tener que reescribir todo de una.
 */
public final class ColaEventos {

    private final ArrayDeque<EventoJuego> cola = new ArrayDeque<>();

    public void publicar(EventoJuego ev) {
        cola.add(Objects.requireNonNull(ev));
    }

    public boolean isEmpty() {
        return cola.isEmpty();
    }

    public int size() {
        return cola.size();
    }

    public EventoJuego poll() {
        return cola.poll();
    }

    /**
     * Obtiene y remueve el primer evento que sea instancia de {@code tipo}.
     * Devuelve null si no hay ninguno.
     */
    public <T extends EventoJuego> T pollFirst(Class<T> tipo) {
        Objects.requireNonNull(tipo);

        for (Iterator<EventoJuego> it = cola.iterator(); it.hasNext(); ) {
            EventoJuego ev = it.next();
            if (tipo.isInstance(ev)) {
                it.remove();
                return tipo.cast(ev);
            }
        }
        return null;
    }

    /**
     * Remueve todos los eventos de una clase determinada.
     * @return cuántos se removieron.
     */
    public <T extends EventoJuego> int limpiar(Class<T> tipo) {
        Objects.requireNonNull(tipo);
        int n = 0;
        for (Iterator<EventoJuego> it = cola.iterator(); it.hasNext(); ) {
            EventoJuego ev = it.next();
            if (tipo.isInstance(ev)) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    public void clear() {
        cola.clear();
    }

    /**
     * Drena (consume) sólo eventos de una clase determinada.
     *
     * Ejemplo:
     *   cola.drenar(EventoPickup.class, ev -> ...);
     */
    public <T extends EventoJuego> void drenar(Class<T> tipo, Consumer<T> consumer) {
        Objects.requireNonNull(tipo);
        Objects.requireNonNull(consumer);

        for (Iterator<EventoJuego> it = cola.iterator(); it.hasNext(); ) {
            EventoJuego ev = it.next();
            if (tipo.isInstance(ev)) {
                it.remove();
                consumer.accept(tipo.cast(ev));
            }
        }
    }
}
