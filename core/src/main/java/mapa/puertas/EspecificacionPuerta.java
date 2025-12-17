package mapa.puertas;

import mapa.model.Direccion;

public class EspecificacionPuerta {
    public final Direccion direccion;
    public final int localX;
    public final int localY;

    public EspecificacionPuerta(Direccion direccion, int localX, int localY) {
        this.direccion = direccion;
        this.localX = localX;
        this.localY = localY;
    }
}
