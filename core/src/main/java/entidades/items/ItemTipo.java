package entidades.items;

import java.util.Random;

/**
 * Enum de todos los tipos de ítems del juego.
 * Cada tipo tiene:
 *  - Rareza (afecta su probabilidad de aparición)
 *  - Implementación del uso del ítem (aplicar efectos)
 */
public enum ItemTipo {

    // -------------------------
    // ÍTEMS DEFINIDOS
    // -------------------------
    CORAZON_EXTRA(RarezaItem.RARO) {
        @Override
        public Item crearInstancia() {
            return new Item("Corazón Extra", this, (jugador) -> {
                jugador.setVidaMaxima(jugador.getVidaMaxima() + 1);
                jugador.setVida(jugador.getVida() + 1);
            });
        }
    },

    BOTA_VELOZ(RarezaItem.COMUN) {
        @Override
        public Item crearInstancia() {
            return new Item("Bota Veloz", this, (jugador) -> {
                jugador.setVelocidad(jugador.getVelocidad() + 500);
            });
        }
    },

    CORAZON_VACIO(RarezaItem.COMUN) {
        @Override
        public Item crearInstancia() {
            return new Item("Corazon Vacio", this, (jugador) -> {
                jugador.setVida(jugador.getVida() + 1);
            });
        }
    },

    RELOJ_ROTO(RarezaItem.RARO) {
        @Override
        public Item crearInstancia() {
            return new Item("Reloj Roto", this, (jugador) -> {
                jugador.setVelocidad(jugador.getVelocidad() + 1000);
            });
        }
    },

    ARMADURA_LIGERA(RarezaItem.EPICO) {
        @Override
        public Item crearInstancia() {
            return new Item("Armadura Ligera", this, (jugador) -> {
                jugador.setVidaMaxima(jugador.getVidaMaxima() + 2);
                jugador.setVida(jugador.getVida() + 2);
            });
        }
    };

    // ============================================================
    // CAMPOS Y CONSTRUCTOR
    // ============================================================

    public final RarezaItem rareza;

    ItemTipo(RarezaItem rareza) {
        this.rareza = rareza;
    }

    // Cada tipo debe crear su propia instancia del item
    public abstract Item crearInstancia();

    private static final Random rng = new Random();

    // ============================================================
    // GENERADOR ALEATORIO POR RAREZA
    // ============================================================

    public static Item generarAleatorioPorRareza() {
        // Sumar pesos totales
        int pesoTotal = 0;
        for (ItemTipo tipo : values()) {
            pesoTotal += tipo.rareza.getPeso();
        }

        // Sortear número dentro del rango total
        int tirada = rng.nextInt(pesoTotal);

        // Selección ponderada
        int acumulado = 0;
        for (ItemTipo tipo : values()) {
            acumulado += tipo.rareza.getPeso();
            if (tirada < acumulado) {
                return tipo.crearInstancia();
            }
        }

        // Seguridad (no debería llegar)
        return values()[0].crearInstancia();
    }
}
