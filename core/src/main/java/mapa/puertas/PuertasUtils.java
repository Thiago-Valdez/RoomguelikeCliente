package mapa.puertas;

import com.badlogic.gdx.math.Vector2;
import mapa.model.Direccion;
import mapa.model.Habitacion;

public final class PuertasUtils {

    private PuertasUtils() {}

    public static Vector2 calcularSpawnEntrada(
        Habitacion salaDestino,
        Direccion desdeDireccion
    ) {
        float margen = 32f; // separarlo un poco de la puerta
        float x = salaDestino.gridX * salaDestino.ancho;
        float y = salaDestino.gridY * salaDestino.alto;

        return switch (desdeDireccion) {
            case NORTE -> new Vector2(
                x + salaDestino.ancho / 2f,
                y + margen
            );
            case SUR -> new Vector2(
                x + salaDestino.ancho / 2f,
                y + salaDestino.alto - margen
            );
            case ESTE -> new Vector2(
                x + margen,
                y + salaDestino.alto / 2f
            );
            case OESTE -> new Vector2(
                x + salaDestino.ancho - margen,
                y + salaDestino.alto / 2f
            );
        };
    }
}
