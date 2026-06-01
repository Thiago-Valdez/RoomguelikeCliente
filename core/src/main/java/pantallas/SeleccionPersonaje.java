package pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import entidades.datos.Estilo;
import entidades.datos.Genero;
import io.github.principal.Principal;

/**
* Pantalla intermedia para elegir la apariencia del jugador (género + estilo).
* Se guarda en Principal y luego se envía al servidor en el Connect.
*/
public class SeleccionPersonaje implements Screen {
    private static final float PREVIEW_FRAME_DURATION = 0.15f;

    private Stage stage;

    private Texture texFondo;

    private final Principal game;

    private Image imgFondo;

    private Image imgPreview;

    private Skin skin;

    private TextureRegionDrawable previewDrawable;

    private boolean esperandoConexion = false;

    private final Screen pantallaVolver;

    private float esperandoTime = 0f;

    private float previewStateTime = 0f;

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Animación idle del preview
        if (imgPreview != null && previewFrames != null && previewFrames.length > 0) {
            previewStateTime += delta;
            int idx = (int) (previewStateTime / PREVIEW_FRAME_DURATION) % previewFrames.length;
            if (previewDrawable != null) previewDrawable.setRegion(previewFrames[idx]);
        }

        stage.act(delta);
        stage.draw();

        // Si el usuario tocó Continuar, mostramos el mensaje al menos 1 frame antes de cambiar
        if (esperandoConexion) {
            esperandoTime += delta;
            if (esperandoTime >= 0.15f) {
                esperandoConexion = false;
                game.cambiarPantalla(new JuegoPrincipal(game));
            }
        }
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("uiskin.json"));

        construirFondo();
        construirUI();
    }

    public SeleccionPersonaje(Principal game, Screen pantallaVolver) {
        this.game = game;
        this.pantallaVolver = pantallaVolver;
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (texFondo != null) texFondo.dispose();
        if (texPreview != null) texPreview.dispose();
    }

    private String previewPath(Genero genero, Estilo estilo) {
        String base = (genero == Genero.FEMENINO) ? "jugador_fem" : "jugador_masc";
        int estiloNum = (estilo != null) ? (estilo.ordinal() + 1) : 1;

        String conEstilo = "Jugadores/" + base + estiloNum + "_quieto.png";
        if (Gdx.files.internal(conEstilo).exists()) return conEstilo;

        String sinEstilo = "Jugadores/" + base + "_quieto.png";
        return sinEstilo;
    }

    private void actualizarPreview() {
        // fallback a lo que haya guardado en Principal
        actualizarPreview(game.getGeneroSeleccionado(), game.getEstiloSeleccionado());
    }

    private void actualizarPreview(Genero genero, Estilo estilo) {
        String path = previewPath(genero, estilo);

        try {
            if (texPreview != null) texPreview.dispose();
        } catch (Exception ignored) {}

        texPreview = new Texture(Gdx.files.internal(path));
        texPreview.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // el *_quieto.png es un spritesheet (ej: 4 frames horizontal). Armamos frames automáticamente.
        int frames = 4;
        int frameW = texPreview.getWidth() / frames;
        int frameH = texPreview.getHeight();
        previewFrames = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            previewFrames[i] = new TextureRegion(texPreview, i * frameW, 0, frameW, frameH);
        }
        previewStateTime = 0f;

        if (previewDrawable == null) previewDrawable = new TextureRegionDrawable(previewFrames[0]);
        else previewDrawable.setRegion(previewFrames[0]);

        if (imgPreview == null) imgPreview = new Image(previewDrawable);
        else imgPreview.setDrawable(previewDrawable);
    }

    private void construirFondo() {
        try {
            texFondo = new Texture(Gdx.files.internal("Fondos/menu_principal.png"));
            texFondo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            imgFondo = new Image(texFondo);
            imgFondo.setFillParent(true);
            stage.addActor(imgFondo);

            Table overlay = new Table();
            overlay.setFillParent(true);
            overlay.setBackground(skin.newDrawable("white", 0f, 0f, 0f, 0.45f));
            stage.addActor(overlay);
        } catch (Exception ignored) {
            // sin fondo: ok
        }
    }

    private void construirUI() {
        Label titulo = new Label("Elegí tu personaje", skin);
        titulo.setFontScale(1.2f);

        SelectBox<Genero> selGenero = new SelectBox<>(skin);
        selGenero.setItems(Genero.values());
        selGenero.setSelected(game.getGeneroSeleccionado());

        SelectBox<Estilo> selEstilo = new SelectBox<>(skin);
        selEstilo.setItems(Estilo.values());
        selEstilo.setSelected(game.getEstiloSeleccionado());

        // Preview inicial
        actualizarPreview(selGenero.getSelected(), selEstilo.getSelected());

        TextButton btnContinuar = new TextButton("Continuar", skin);
        TextButton btnVolver = new TextButton("Volver", skin);

        lblEsperando = new Label("Aguardando conexion...", skin);
        lblEsperando.setVisible(false);

        selGenero.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                actualizarPreview(selGenero.getSelected(), selEstilo.getSelected());
            }
        });

        selEstilo.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                actualizarPreview(selGenero.getSelected(), selEstilo.getSelected());
            }
        });

        btnContinuar.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setGeneroSeleccionado(selGenero.getSelected());
                game.setEstiloSeleccionado(selEstilo.getSelected());

                // Flujo actual: Jugar = Online
                game.setModoOnline(true);

                // Mostrar feedback de que el cliente está esperando al servidor
                esperandoConexion = true;
                esperandoTime = 0f;
                if (lblEsperando != null) lblEsperando.setVisible(true);

                // Evitar dobles clicks / cambios mientras conecta
                selGenero.setDisabled(true);
                selEstilo.setDisabled(true);
                btnContinuar.setDisabled(true);
            }
        });

        btnVolver.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                if (pantallaVolver != null) game.cambiarPantalla(pantallaVolver);
                else game.cambiarPantalla(new MenuPrincipal(game));
            }
        });

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Table card = new Table(skin);
        card.setBackground(skin.newDrawable("white", 0f, 0f, 0f, 0.35f));
        card.pad(24);

        card.add(titulo).padBottom(14).row();

        // Preview
        if (imgPreview != null) {
            card.add(imgPreview).size(128, 128).padBottom(14).row();
        }

        card.add(new Label("Género", skin)).left().row();
        card.add(selGenero).width(280).height(44).padBottom(14).row();

        card.add(new Label("Estilo", skin)).left().row();
        card.add(selEstilo).width(280).height(44).padBottom(22).row();

        Table botones = new Table();
        botones.add(btnVolver).width(130).height(46).padRight(12);
        botones.add(btnContinuar).width(130).height(46);

        card.add(botones).row();

        // Mensaje de estado al conectar
        if (lblEsperando != null) {
            card.add(lblEsperando).padTop(14).center();
        }

        root.add(card);
        stage.addActor(root);
    }

    // Mensaje de espera
    private Label lblEsperando;

    // Preview del personaje
    private Texture texPreview;

    // animación de quieto (preview)
    private TextureRegion[] previewFrames;
}