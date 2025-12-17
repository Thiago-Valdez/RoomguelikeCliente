package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.principal.Principal;

public class PantallaGameOver implements Screen {

    private final Principal game;
    private Stage stage;
    private Skin skin;

    private float timer = 0f;
    private static final float DURACION = 2.5f; // segundos

    public PantallaGameOver(Principal game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Label titulo = new Label("GAME OVER", skin);
        titulo.setAlignment(Align.center);

        Label subtitulo = new Label("Volviendo al menú...", skin);
        subtitulo.setAlignment(Align.center);

        Table t = new Table();
        t.setFillParent(true);
        t.center();
        t.add(titulo).padBottom(20).row();
        t.add(subtitulo);

        stage.addActor(t);
    }

    @Override
    public void render(float delta) {
        timer += delta;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        if (timer >= DURACION) {
            game.cambiarPantalla(new MenuPrincipal(game));
        }
    }

    @Override public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
}
