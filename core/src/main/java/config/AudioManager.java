package config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class AudioManager {

    private float masterVolume = 0.8f;

    private Music menuMusic;
    private Music gameMusic;

    public void cargarMusicas() {
        // Ajustá extensión si hace falta: .mp3 / .ogg
        menuMusic = Gdx.audio.newMusic(Gdx.files.internal("Audio/musica_menu.mp3"));
        menuMusic.setLooping(true);

        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("Audio/musica_juego.mp3"));
        gameMusic.setLooping(true);

        aplicarVolumen();
    }

    public void setMasterVolume(float v) {
        masterVolume = clamp01(v);
        aplicarVolumen();
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    private void aplicarVolumen() {
        if (menuMusic != null) menuMusic.setVolume(masterVolume);
        if (gameMusic != null) gameMusic.setVolume(masterVolume);
    }

    public void playMenu() {
        if (menuMusic == null) return;
        if (gameMusic != null) gameMusic.stop();
        if (!menuMusic.isPlaying()) menuMusic.play();
    }

    public void playJuego() {
        if (gameMusic == null) return;
        if (menuMusic != null) menuMusic.stop();
        if (!gameMusic.isPlaying()) gameMusic.play();
    }

    public void stopAll() {
        if (menuMusic != null) menuMusic.stop();
        if (gameMusic != null) gameMusic.stop();
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    public void dispose() {
        if (menuMusic != null) menuMusic.dispose();
        if (gameMusic != null) gameMusic.dispose();
    }
}
