package config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public final class Settings {

    private static final String PREFS = "roomguelike_settings";

    private static final String K_VOL = "master_volume";
    private static final String K_W = "window_w";
    private static final String K_H = "window_h";
    private static final String K_FULLSCREEN = "fullscreen";

    private final Preferences prefs;

    public Settings() {
        this.prefs = Gdx.app.getPreferences(PREFS);
    }

    public float getVolumen() {
        return prefs.getFloat(K_VOL, 0.8f);
    }

    public void setVolumen(float v) {
        prefs.putFloat(K_VOL, clamp01(v));
    }

    public int getWindowW() {
        return prefs.getInteger(K_W, 1280);
    }

    public int getWindowH() {
        return prefs.getInteger(K_H, 720);
    }

    public void setResolucion(int w, int h) {
        prefs.putInteger(K_W, w);
        prefs.putInteger(K_H, h);
    }

    public boolean isFullscreen() {
        return prefs.getBoolean(K_FULLSCREEN, false);
    }

    public void setFullscreen(boolean fullscreen) {
        prefs.putBoolean(K_FULLSCREEN, fullscreen);
    }

    /** Aplica lo guardado (ventana o fullscreen) */
    public void aplicarResolucion() {
        if (isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(getWindowW(), getWindowH());
        }
    }

    public void flush() {
        prefs.flush();
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
