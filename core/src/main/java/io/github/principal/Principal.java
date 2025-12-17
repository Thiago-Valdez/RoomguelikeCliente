package io.github.principal;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import config.AudioManager;
import config.Settings;
import pantallas.MenuPrincipal;

public class Principal extends Game {

    public Settings settings;
    public AudioManager audio;
    private boolean modoOnline = false;

    @Override
    public void create() {
        settings = new Settings();
        audio = new AudioManager();

        audio.cargarMusicas();          // ✅ carga
        audio.setMasterVolume(settings.getVolumen()); // ✅ aplica volumen guardado
        audio.playMenu();               // ✅ suena el menú

        cambiarPantalla(new MenuPrincipal(this));
    }

    public void aplicarSettings() {
        // Volumen
        audio.setMasterVolume(settings.getVolumen());

        // Resolución / fullscreen
        settings.aplicarResolucion();
    }

    /**
     * Cambia de pantalla y libera (dispose) la anterior.
     * Importante: Game#setScreen NO dispone la pantalla anterior automáticamente.
     */
    public void cambiarPantalla(Screen nueva) {
        Screen vieja = getScreen();
        setScreen(nueva);
        if (vieja != null) vieja.dispose();
    }

    public boolean isModoOnline() {
        return modoOnline;
    }

    public void setModoOnline(boolean modoOnline) {
        this.modoOnline = modoOnline;
    }


    @Override
    public void dispose() {
        if (audio != null) audio.dispose();
    }
}
