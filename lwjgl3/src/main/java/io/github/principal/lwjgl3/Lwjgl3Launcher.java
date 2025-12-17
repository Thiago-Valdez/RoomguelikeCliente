package io.github.principal.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import io.github.principal.Principal;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Principal(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {

        System.out.println(">>>> Lwjgl3Launcher.getDefaultConfiguration() VERSION NUEVA <<<<");

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("EscapeRoomguelike");
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(640, 480);

        // 🔥 GUARDA: forzamos que NO se use ningún icono
        // Si existe cualquier otra llamada a setWindowIcon, la borrás.
        // NO dejes ni una sola línea con configuration.setWindowIcon(...);

        return configuration;
    }
}
