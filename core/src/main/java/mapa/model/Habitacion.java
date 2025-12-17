package mapa.model;

import java.util.EnumMap;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import mapa.puertas.EspecificacionPuerta;

/**
 * Habitaciones corregidas para topología 5x5 estilo Binding of Isaac.
 * TODAS las puertas son geométricamente válidas según su gridX/gridY.
 * Mantuvimos tus offsets exactos.
 */
public enum Habitacion {

    // -----------------------------------------------------------
    // Fila y = 4 (fila superior)
    // -----------------------------------------------------------

    INICIO_1(
        1, "Inicio", TipoSala.INICIO,
        0, 0, 512, 512,
        0, 4,
        Map.of(
            // NORTE eliminado (no existe Y=5)
            Direccion.ESTE, new EspecificacionPuerta(Direccion.ESTE, 512, 256),
            Direccion.SUR,  new EspecificacionPuerta(Direccion.SUR, 256, 0)
        )
    ),

    ACERTIJO_1(
        2, "Acertijo 1", TipoSala.ACERTIJO,
        512, 0, 512, 512,
        1, 4,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE, 0,   256),
            Direccion.ESTE,  new EspecificacionPuerta(Direccion.ESTE,  512, 256),
            Direccion.SUR,   new EspecificacionPuerta(Direccion.SUR,   256, 0)
        )
    ),

    ACERTIJO_2(
        3, "Acertijo 2", TipoSala.ACERTIJO,
        1024, 0, 512, 512,
        2, 4,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE, 0, 256),
            Direccion.ESTE,  new EspecificacionPuerta(Direccion.ESTE, 512,256),
            Direccion.SUR,   new EspecificacionPuerta(Direccion.SUR,  256,0)
        )
    ),

    ACERTIJO_3(
        4, "Acertijo 3", TipoSala.ACERTIJO,
        1536, 0, 512, 512,
        3, 4,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE, 0,256),
            Direccion.ESTE,  new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.SUR,   new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    ACERTIJO_4(
        5, "Acertijo 4", TipoSala.ACERTIJO,
        2048, 0, 512, 512,
        4, 4,
        Map.of(
            Direccion.OESTE, new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.SUR,   new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    // -----------------------------------------------------------
    // Fila y = 3
    // -----------------------------------------------------------

    ACERTIJO_5(
        6, "Acertijo 5", TipoSala.ACERTIJO,
        2560, 0, 512, 512,
        0, 3,
        Map.of(
            Direccion.ESTE, new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,  new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    ACERTIJO_6(
        7, "Acertijo 6", TipoSala.ACERTIJO,
        3072, 0, 512, 512,
        1, 3,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE, new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,  new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    ACERTIJO_7(
        8, "Acertijo 7", TipoSala.ACERTIJO,
        3584, 0, 512, 512,
        2, 3,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE, new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,  new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    ACERTIJO_8(
        9, "Acertijo 8", TipoSala.ACERTIJO,
        4096, 0, 512, 512,
        3, 3,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE, new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,  new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    ACERTIJO_9(
        10, "Acertijo 9", TipoSala.ACERTIJO,
        4608, 0, 512, 512,
        4, 3,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,  new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    // -----------------------------------------------------------
    // Fila y = 2
    // -----------------------------------------------------------

    ACERTIJO_10(
        11, "Acertijo 10", TipoSala.ACERTIJO,
        5120, 0, 512, 512,
        0, 2,
        Map.of(
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_1(
        12, "Combate 1", TipoSala.COMBATE,
        5632, 0, 512, 512,
        1, 2,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_2(
        13, "Combate 2", TipoSala.COMBATE,
        6144, 0, 512, 512,
        2, 2,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_3(
        14, "Combate 3", TipoSala.COMBATE,
        6656, 0, 512, 512,
        3, 2,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_4(
        15, "Combate 4", TipoSala.COMBATE,
        7168, 0, 512, 512,
        4, 2,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    // -----------------------------------------------------------
    // Fila y = 1
    // -----------------------------------------------------------

    COMBATE_5(
        16, "Combate 5", TipoSala.COMBATE,
        7680, 0, 512, 512,
        0, 1,
        Map.of(
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_6(
        17, "Combate 6", TipoSala.COMBATE,
        8192, 0, 512, 512,
        1, 1,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_7(
        18, "Combate 7", TipoSala.COMBATE,
        8704, 0, 512, 512,
        2, 1,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_8(
        19, "Combate 8", TipoSala.COMBATE,
        9216, 0, 512, 512,
        3, 1,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    COMBATE_9(
        20, "Combate 9", TipoSala.COMBATE,
        9728, 0, 512, 512,
        4, 1,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512),
            Direccion.SUR,new EspecificacionPuerta(Direccion.SUR,256,0)
        )
    ),

    // -----------------------------------------------------------
    // Fila y = 0 (jefe en (3,0) y (4,0)) – un solo acceso por OESTE
    // -----------------------------------------------------------

    COMBATE_10(
        21, "Combate 10", TipoSala.COMBATE,
        10240, 0, 512, 512,
        0, 0,
        Map.of(
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512)
        )
    ),

    BOTIN_1(
        22, "Botín 1", TipoSala.BOTIN,
        10752, 0, 512, 512,
        1, 0,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512)
        )
    ),

    BOTIN_2(
        23, "Botín 2", TipoSala.BOTIN,
        11264, 0, 512, 512,
        2, 0,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256),
            Direccion.ESTE,new EspecificacionPuerta(Direccion.ESTE,512,256),
            Direccion.NORTE,new EspecificacionPuerta(Direccion.NORTE,256,512)
        )
    ),

    JEFE_1(
        24, "Jefe 1", TipoSala.JEFE,
        11776, 0, 512, 512,
        3, 0,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256)  // ÚNICA ENTRADA
        )
    ),

    JEFE_2(
        25, "Jefe 2", TipoSala.JEFE,
        12288, 0, 512, 512,
        4, 0,
        Map.of(
            Direccion.OESTE,new EspecificacionPuerta(Direccion.OESTE,0,256)  // ÚNICA ENTRADA
        )
    );

    // -----------------------------------------------------------
    // Código base sin cambios
    // -----------------------------------------------------------

    public final int id;
    public final String nombreVisible;
    public final TipoSala tipo;
    public final int srcX, srcY, ancho, alto;
    public final int gridX;
    public final int gridY;
    public final EnumMap<Direccion, EspecificacionPuerta> puertas;

    Habitacion(int id,
               String nombreVisible,
               TipoSala tipo,
               int srcX, int srcY, int ancho, int alto,
               int gridX, int gridY,
               Map<Direccion, EspecificacionPuerta> puertas) {

        this.id = id;
        this.nombreVisible = nombreVisible;
        this.tipo = tipo;
        this.srcX = srcX;
        this.srcY = srcY;
        this.ancho = ancho;
        this.alto = alto;
        this.gridX = gridX;
        this.gridY = gridY;

        this.puertas = new EnumMap<>(Direccion.class);
        this.puertas.putAll(puertas);
    }

    public boolean tienePuerta(Direccion d) {
        return puertas.containsKey(d);
    }

    public Rectangle rectFuente() {
        return new Rectangle(srcX, srcY, ancho, alto);
    }
}
