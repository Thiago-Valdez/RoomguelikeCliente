package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import io.github.principal.Principal;

public class MenuPrincipal implements Screen {
    private Stage stage;

    private final Principal game;

    private Image imgFondo;

    private Skin skin;

    @Override public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (texFondo != null) texFondo.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        game.audio.playMenu();

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        construirFondo();
        construirUI();
    }

    public MenuPrincipal(Principal game) {
        this.game = game;
    }

    private void construirFondo() {
        // Ajustá el path a tu asset real
        texFondo = new Texture(Gdx.files.internal("Fondos/menu_principal.png"));
        texFondo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        imgFondo = new Image(texFondo);
        imgFondo.setFillParent(true);

        // 1️⃣ fondo abajo de todo
        stage.addActor(imgFondo);

        // 2️⃣ overlay oscuro suave (opcional pero muy recomendable)
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(
        skin.newDrawable("white", 0f, 0f, 0f, 0.35f)
        );
        stage.addActor(overlay);
    }

    private void construirUI() {

        TextButton btnJugar = new TextButton("Jugar", skin);
        TextButton btnOpciones = new TextButton("Opciones", skin);
        TextButton btnComoJugar = new TextButton("Cómo jugar", skin);

        btnJugar.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setModoOnline(true);
                game.cambiarPantalla(new SeleccionPersonaje(game, MenuPrincipal.this));
            }
        });

        btnOpciones.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.cambiarPantalla(new MenuOpciones(game, MenuPrincipal.this));
            }
        });

        btnComoJugar.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.cambiarPantalla(new ComoJugar(game, MenuPrincipal.this));
            }
        });

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        root.add(btnJugar).width(260).height(60).pad(10).row();
        root.add(btnOpciones).width(260).height(60).pad(10).row();
        root.add(btnComoJugar).width(260).height(60).pad(10).row();

        // 3️⃣ UI arriba de todo
        stage.addActor(root);
    }

    // Fondo
    private Texture texFondo;

    @Override public void pause() {}
    @Override public void resume() {}
}