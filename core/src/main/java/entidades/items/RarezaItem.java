package entidades.items;

public enum RarezaItem {
    COMUN(60),
    RARO(25),
    EPICO(10),
    LEGENDARIO(5);

    private final int peso;

    RarezaItem(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }
}
