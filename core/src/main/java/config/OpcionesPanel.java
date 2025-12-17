package config;

import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import io.github.principal.Principal;

public class OpcionesPanel {

    private final Principal game;

    private final Table root = new Table();
    private final Slider volumen;
    private final SelectBox<String> resoluciones;

    public OpcionesPanel(Principal game, Skin skin) {
        this.game = game;

        Label titulo = new Label("Opciones", skin);

        volumen = new Slider(0f, 1f, 0.01f, false, skin);
        resoluciones = new SelectBox<>(skin);
        resoluciones.setItems("1280x720", "1600x900", "1920x1080");

        Label controles = new Label(
            "Jugador 1: WASD + Acción\nJugador 2: Flechas + Acción",
            skin
        );

        root.center();
        root.add(titulo).padBottom(30).row();

        root.add(new Label("Volumen", skin)).row();
        root.add(volumen).width(300).padBottom(20).row();

        root.add(new Label("Resolución", skin)).row();
        root.add(resoluciones).width(300).padBottom(20).row();

        root.add(new Label("Controles", skin)).padTop(10).row();
        root.add(controles).padBottom(30).row();

        cargarValoresGuardados();
        conectarListeners();
    }

    public Table getRoot() {
        return root;
    }

    private void cargarValoresGuardados() {
        volumen.setValue(game.settings.getVolumen());

        String actual = game.settings.getWindowW() + "x" + game.settings.getWindowH();
        boolean coincide =
            actual.equals("1280x720") ||
                actual.equals("1600x900") ||
                actual.equals("1920x1080");
        resoluciones.setSelected(coincide ? actual : "1280x720");
    }

    private void conectarListeners() {
        volumen.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.settings.setVolumen(volumen.getValue());
                game.settings.flush();
                game.aplicarSettings();
            }
        });

        resoluciones.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                String[] p = resoluciones.getSelected().split("x");
                int w = Integer.parseInt(p[0]);
                int h = Integer.parseInt(p[1]);

                game.settings.setResolucion(w, h);
                game.settings.flush();
                game.aplicarSettings();
            }
        });
    }
}
