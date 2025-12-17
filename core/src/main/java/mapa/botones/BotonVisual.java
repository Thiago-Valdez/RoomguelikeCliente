package mapa.botones;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import mapa.model.Habitacion;

public class BotonVisual {

    // --- Identificación / contexto ---
    public String id;                 // opcional, desde Tiled
    public Habitacion sala;            // sala a la que pertenece

    // --- Transform ---
    public final Vector2 posCentro = new Vector2();
    public final float w;
    public final float h;

    // --- Estado ---
    public final int jugadorId;        // 1 o 2
    public boolean presionado = false;

    // --- Sprites ---
    private final TextureRegion up;
    private final TextureRegion down;

    public BotonVisual(float cx, float cy,
                       float w, float h,
                       int jugadorId,
                       TextureRegion up,
                       TextureRegion down) {
        this.posCentro.set(cx, cy);
        this.w = w;
        this.h = h;
        this.jugadorId = jugadorId;
        this.up = up;
        this.down = down;
    }

    public TextureRegion frameActual() {
        return presionado ? down : up;
    }
}
