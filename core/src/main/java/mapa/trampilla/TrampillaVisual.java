package mapa.trampilla;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
* Representación visual simple de la trampilla (solo render).
*/
public final class TrampillaVisual {
    public final float x;

    private final TextureRegion region;

    public final float h;

    public final float w;

    public final float y;

    public TextureRegion region() {
        return region;
    }

    public TrampillaVisual(float x, float y, float w, float h, TextureRegion region) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.region = region;
    }
}