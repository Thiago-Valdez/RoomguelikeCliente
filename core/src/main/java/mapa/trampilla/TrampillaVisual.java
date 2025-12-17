package mapa.trampilla;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Representación visual simple de la trampilla (solo render).
 */
public final class TrampillaVisual {

    public final float x;
    public final float y;
    public final float w;
    public final float h;
    private final TextureRegion region;

    public TrampillaVisual(float x, float y, float w, float h, TextureRegion region) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.region = region;
    }

    public TextureRegion region() {
        return region;
    }
}
