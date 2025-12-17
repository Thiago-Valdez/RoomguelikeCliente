package mapa.puertas;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class PuertaVisual {
    public final float x;      // esquina inferior izquierda
    public final float y;
    public final float width;
    public final float height;

    // Texturas (se inyectan al crear)
    private TextureRegion frameAbierta;
    private TextureRegion frameCerrada;

    // Estado
    private boolean abierta = true;

    public PuertaVisual(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setFrames(TextureRegion abierta, TextureRegion cerrada) {
        this.frameAbierta = abierta;
        this.frameCerrada = cerrada;
    }

    public void setAbierta(boolean abierta) {
        this.abierta = abierta;
    }

    public boolean isAbierta() {
        return abierta;
    }

    public TextureRegion frameActual() {
        // Si falta algún frame, devolvemos el que exista
        if (abierta) return frameAbierta != null ? frameAbierta : frameCerrada;
        return frameCerrada != null ? frameCerrada : frameAbierta;
    }
}
