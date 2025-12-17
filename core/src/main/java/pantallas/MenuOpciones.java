package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.principal.Principal;

public class MenuOpciones implements Screen {

    private final Principal game;
    private final Screen volverA;

    private Stage stage;
    private Skin skin;

    private Slider volumen;
    private SelectBox<String> resoluciones;

    public MenuOpciones(Principal game, Screen volverA) {
        this.game = game;
        this.volverA = volverA;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        construirUI();
        cargarValores();
        conectarListeners();
    }

    private void construirUI() {
        Label titulo = new Label("Opciones", skin);

        volumen = new Slider(0f, 1f, 0.01f, false, skin);

        resoluciones = new SelectBox<>(skin);
        resoluciones.setItems(
            "1280x720",
            "1600x900",
            "1920x1080"
        );

        Label controles = new Label(
            "Jugador 1: WASD + Acción\nJugador 2: Flechas + Acción",
            skin
        );

        TextButton volver = new TextButton("Volver", skin);
        volver.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                game.cambiarPantalla(volverA);
            }
        });

        Table t = new Table();
        t.setFillParent(true);
        t.center();

        t.add(titulo).padBottom(30).row();

        t.add(new Label("Volumen", skin)).row();
        t.add(volumen).width(300).padBottom(20).row();

        t.add(new Label("Resolución", skin)).row();
        t.add(resoluciones).width(300).padBottom(20).row();

        t.add(new Label("Controles", skin)).padTop(10).row();
        t.add(controles).padBottom(30).row();

        t.add(volver).width(200).height(50);

        stage.addActor(t);
    }

    private void cargarValores() {
        // Volumen actual
        volumen.setValue(game.settings.getVolumen());

        // Resolución actual
        String actual = game.settings.getWindowW() + "x" + game.settings.getWindowH();

        boolean coincide =
            actual.equals("1280x720") ||
                actual.equals("1600x900") ||
                actual.equals("1920x1080");

        resoluciones.setSelected(coincide ? actual : "1280x720");
    }

    private void conectarListeners() {
        volumen.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                game.settings.setVolumen(volumen.getValue());
                game.settings.flush();
                game.aplicarSettings();
            }
        });

        resoluciones.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                String sel = resoluciones.getSelected(); // "1280x720"
                String[] p = sel.split("x");
                int w = Integer.parseInt(p[0]);
                int h = Integer.parseInt(p[1]);

                game.settings.setResolucion(w, h);
                game.settings.setFullscreen(false); // por si quedó guardado true
                game.settings.flush();

                game.aplicarSettings();
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void hide() { /* no dispose acá */ }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
}
