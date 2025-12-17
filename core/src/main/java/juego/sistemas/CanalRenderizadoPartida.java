package juego.sistemas;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import camara.CamaraDeSala;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesEntidad;
import fisica.FisicaMundo;
import interfaces.hud.HudJuego;
import mapa.model.Habitacion;
import mapa.puertas.PuertaVisual;
import mapa.trampilla.TrampillaVisual;

/**
 * Se encarga SOLO del render (mapa, puertas, sprites, debug y HUD).
 * No actualiza gameplay.
 */
public final class CanalRenderizadoPartida {

    private final CamaraDeSala camaraSala;
    private final OrthogonalTiledMapRenderer mapaRenderer;
    private final ShapeRenderer shapeRendererMundo;
    private final SpriteBatch batch;
    private final FisicaMundo fisica;
    private final HudJuego hud;
    private final GestorDeEntidades gestorEntidades;
    private final SistemaSpritesEntidades sprites;
    private ShapeRenderer debugRenderer = new ShapeRenderer();


    private final List<PuertaVisual> puertasVisuales = new ArrayList<>();

    public CanalRenderizadoPartida(
        CamaraDeSala camaraSala,
        OrthogonalTiledMapRenderer mapaRenderer,
        ShapeRenderer shapeRendererMundo,
        SpriteBatch batch,
        FisicaMundo fisica,
        HudJuego hud,
        GestorDeEntidades gestorEntidades,
        SistemaSpritesEntidades sprites
    ) {
        this.camaraSala = camaraSala;
        this.mapaRenderer = mapaRenderer;
        this.shapeRendererMundo = shapeRendererMundo;
        this.batch = batch;
        this.fisica = fisica;
        this.hud = hud;
        this.gestorEntidades = gestorEntidades;
        this.sprites = sprites;
    }

    public void setPuertasVisuales(List<PuertaVisual> puertasVisuales) {
        this.puertasVisuales.clear();
        if (puertasVisuales != null) this.puertasVisuales.addAll(puertasVisuales);
    }

    public void render(
        float delta,
        Habitacion salaActual,
        boolean debugFisica,
        Jugador jugador1,
        Jugador jugador2,
        List<mapa.botones.BotonVisual> botonesVisuales,
        TrampillaVisual trampillaVisual

    ) {
        if (camaraSala == null) return;

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // =====================
        // Mapa Tiled
        // =====================
        if (mapaRenderer != null) {
            mapaRenderer.setView(camaraSala.getCamara());
            mapaRenderer.render();
        }

        // =====================
        // Sprites (puertas, botones, enemigos, jugadores)
        // =====================
        if (batch != null) {
            batch.setProjectionMatrix(camaraSala.getCamara().combined);
            batch.begin();
            batch.enableBlending();
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


            // Puertas (sprite) - se dibujan con la textura asignada a cada PuertaVisual
            if (puertasVisuales != null && !puertasVisuales.isEmpty()) {
                for (PuertaVisual pv : puertasVisuales) {
                    if (pv == null) continue;

                    TextureRegion tr = pv.frameActual();
                    if (tr != null) {
                        batch.draw(tr, pv.x, pv.y, pv.width, pv.height);
                    }
                }
            }

            // Botones (antes que enemigos/jugadores) - se dibujan al tamaño del rect en Tiled
            if (botonesVisuales != null && salaActual != null) {
                for (mapa.botones.BotonVisual bv : botonesVisuales) {
                    if (bv == null) continue;
                    if (bv.sala != salaActual) continue;

                    float x = bv.posCentro.x - bv.w / 2f;
                    float y = bv.posCentro.y - bv.h / 2f;

                    TextureRegion fr = bv.frameActual();
                    if (fr != null) {
                        batch.draw(fr, x, y, bv.w, bv.h);
                    }
                }
            }

            // Trampilla (fin de nivel) - solo si existe y pertenece a la sala actual
            if (trampillaVisual != null && salaActual != null) {
                TextureRegion tr = trampillaVisual.region();
                if (tr != null) {
                    batch.draw(tr, trampillaVisual.x, trampillaVisual.y, trampillaVisual.w, trampillaVisual.h);
                }
            }

            // Enemigos primero (atrás)
            if (gestorEntidades != null && sprites != null) {
                for (Enemigo e : gestorEntidades.getEnemigosMundo()) {
                    SpritesEntidad s = sprites.get(e);
                    if (s != null) {
                        s.update(delta);
                        s.render(batch);
                    }
                }
            }

            // Jugadores después
            if (sprites != null) {
                SpritesEntidad s1 = sprites.get(jugador1);
                if (s1 != null) {
                    s1.update(delta);
                    s1.render(batch);
                }

                SpritesEntidad s2 = sprites.get(jugador2);
                if (s2 != null) {
                    s2.update(delta);
                    s2.render(batch);
                }
            }

            sprites.limpiarItemsDesaparecidos();
            sprites.renderItems(batch, salaActual);



            batch.end();
        }

        // =====================
        // Debug fisica (opcional)
        // =====================
        if (debugFisica && fisica != null) {
            fisica.debugDraw(camaraSala.getCamara());
        }

        // =====================
        // Debug puertas (solo si querés ver hitboxes) - opcional
        // =====================
        if (debugFisica && shapeRendererMundo != null && gestorEntidades != null) {
            shapeRendererMundo.setProjectionMatrix(camaraSala.getCamara().combined);
            shapeRendererMundo.begin(ShapeRenderer.ShapeType.Line);
            //gestorEntidades.renderPuertas(shapeRendererMundo, salaActual);
            shapeRendererMundo.end();
        }

        // =====================
        // HUD (siempre al final)
        // =====================
        if (hud != null) {
            hud.render();
        }
    }

    public void dibujarDebugBodies(Camera camara, ShapeRenderer debugRenderer, Body... bodies) {
        if (camara == null || debugRenderer == null || bodies == null) return;

        debugRenderer.setProjectionMatrix(camara.combined);
        debugRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Body body : bodies) {
            if (body == null) continue;
            Vector2 pos = body.getPosition();
            debugRenderer.rect(pos.x - 2f, pos.y - 2f, 4f, 4f);
        }

        debugRenderer.end();
    }
}
