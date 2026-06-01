package mapa.puertas;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class PuertaVisual {
    private TextureRegion frameCerrada;

    public final float height;

    public final float width;

    public final float x;      // esquina inferior izquierda
    public final float y;

    public PuertaVisual(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public TextureRegion frameActual() {
        // Si falta algún frame, devolvemos el que exista
        if (abierta) return frameAbierta != null ? frameAbierta : frameCerrada;
        return frameCerrada != null ? frameCerrada : frameAbierta;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }

    public void setFrames(TextureRegion abierta, TextureRegion cerrada) {
        this.frameAbierta = abierta;
        this.frameCerrada = cerrada;
    }

    // Estado
    private boolean abierta = true;

    // Texturas (se inyectan al crear)
    private TextureRegion frameAbierta;
}