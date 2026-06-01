package mapa.botones;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import mapa.model.Habitacion;

public class BotonVisual {
    private final TextureRegion down;

    public final float h;

    public final float w;

    public TextureRegion frameActual() {
        return presionado ? down : up;
    }

    // --- Estado ---
    public final int jugadorId;        // 1 o 2
    public boolean presionado = false;

    // --- Identificación / contexto ---
    public String id;                 // opcional, desde Tiled
    public Habitacion sala;            // sala a la que pertenece

    // --- Transform ---
    public final Vector2 posCentro = new Vector2();

    // --- Sprites ---
    private final TextureRegion up;

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
}