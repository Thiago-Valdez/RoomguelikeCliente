package mapa.model;

public enum Direccion {
    NORTE(0, 1),
    SUR(0, -1),
    ESTE(1, 0),
    OESTE(-1, 0);

    public final int dx;
    public final int dy;

    Direccion(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direccion opuesta() {
        return switch (this) {
            case NORTE -> SUR;
            case SUR -> NORTE;
            case ESTE -> OESTE;
            case OESTE -> ESTE;
        };
    }
}
