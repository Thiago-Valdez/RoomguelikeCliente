package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.principal.Principal;

public class ComoJugar implements Screen {

    private final Principal game;
    private final Screen volverA;

    private Stage stage;
    private Skin skin;

    public ComoJugar(Principal game, Screen volverA) {
        this.game = game;
        this.volverA = volverA;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        construirUI();
    }

    private void construirUI() {
        Label titulo = new Label("Cómo jugar", skin);

        Label texto = new Label(
            "• Cooperativo para 2 jugadores\n" +
                "• Ambos deben presionar botones para avanzar\n" +
                "• Derrota enemigos y llega a la trampilla\n" +
                "• Trabajen en equipo para sobrevivir",
            skin
        );
        texto.setWrap(true);

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

        t.add(titulo).padBottom(20).row();
        t.add(texto).width(500).padBottom(30).row();
        t.add(volver).width(200).height(50);

        stage.addActor(t);
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void hide() { /* no hacer dispose acá */ }


    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
}
