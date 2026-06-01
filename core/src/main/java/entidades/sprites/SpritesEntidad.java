package entidades.sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import entidades.Entidad;

public abstract class SpritesEntidad {
    protected Animation<TextureRegion> animQuieto;

    protected Array<TextureRegion> framesQuieto;

    protected Texture texQuieto;

    protected TextureRegion fallbackQuieto;

    protected boolean enMuerte = false;

    protected boolean ultimaMiradaDerecha = true;

    protected final Entidad entidad;

    protected final int frameW;

    protected float offsetX = 0f;

    protected float stateTime = 0f;

    protected Animation<TextureRegion> animMovimiento;

    protected Animation<TextureRegion> animMuerte;

    protected Array<TextureRegion> framesMovimiento;

    protected Array<TextureRegion> framesMuerte;

    protected Texture texMovimiento;

    protected Texture texMuerte;

    protected TextureRegion fallbackMovimiento;

    protected TextureRegion fallbackMuerte;

    protected boolean muerteFinalizada = false;

    protected final int frameH;

    protected float muerteTime = 0f;

    protected float offsetY = 0f;

    protected float tiempoAnim = 0f;

    public boolean estaEnMuerte() {
        return enMuerte;
    }

    public boolean muerteTerminada() {
        return muerteFinalizada;
    }

    public void detenerMuerte() {
        enMuerte = false;
        muerteFinalizada = false;
        muerteTime = 0f;
        stateTime = 0f;
    }

    public void dispose() {
        if (texQuieto != null) texQuieto.dispose();
        if (texMovimiento != null) texMovimiento.dispose();
        if (texMuerte != null) texMuerte.dispose();
    }

    public void iniciarMuerte() {
        if (enMuerte || animMuerte == null) return;

        enMuerte = true;
        muerteFinalizada = false;
        muerteTime = 0f;
        stateTime = 0f;
    }

    public void render(SpriteBatch batch) {
        if (batch == null || entidad == null || entidad.getCuerpoFisico() == null) return;

        // ✅ ÚNICA fuente de verdad
        Vector2 pos = entidad.getCuerpoFisico().getPosition();
        float vx = entidad.getCuerpoFisico().getLinearVelocity().x;

        if (Math.abs(vx) > 0.001f) {
            ultimaMiradaDerecha = vx >= 0f;
        }

        TextureRegion frame = elegirFrame();
        if (frame == null) return;

        float w = frame.getRegionWidth();
        float h = frame.getRegionHeight();

        float baseX = pos.x - w / 2f;
        float y = pos.y - anclaPie + offsetY;

        float ox = ultimaMiradaDerecha ? offsetX : -offsetX;

        if (ultimaMiradaDerecha) {
            batch.draw(frame, baseX + ox, y, w, h);
        } else {
            batch.draw(frame, baseX + ox + w, y, -w, h);
        }
    }

    protected Array<TextureRegion> split(Texture tex) {
        Array<TextureRegion> frames = new Array<>();
        TextureRegion[][] grid = TextureRegion.split(tex, frameW, frameH);

        for (TextureRegion[] row : grid) {
            for (TextureRegion r : row) {
                frames.add(r);
            }
        }
        return frames;
    }

    protected SpritesEntidad(Entidad entidad, int frameW, int frameH) {
        this.entidad = entidad;
        this.frameW = frameW;
        this.frameH = frameH;
    }

    protected TextureRegion elegirFrame() {
        if (enMuerte) {
            return (animMuerte != null) ? animMuerte.getKeyFrame(muerteTime, false) : fallbackMuerte;
        }

        if (entidad == null || entidad.getCuerpoFisico() == null) {
            return fallbackQuieto;
        }

        boolean moviendo = entidad.getCuerpoFisico().getLinearVelocity().len2() > 0.01f;

        if (moviendo && animMovimiento != null) {
            return animMovimiento.getKeyFrame(stateTime, true);
        }

        return animQuieto != null ? animQuieto.getKeyFrame(stateTime, true) : fallbackQuieto;
    }

    protected abstract String pathQuieto();

    protected void construirAnimaciones() {
        framesQuieto = split(texQuieto);
        framesMovimiento = split(texMovimiento);

        animQuieto = new Animation<>(duracionQuieto(), framesQuieto, Animation.PlayMode.LOOP);
        animMovimiento = new Animation<>(duracionMovimiento(), framesMovimiento, Animation.PlayMode.LOOP);

        fallbackQuieto = !framesQuieto.isEmpty() ? framesQuieto.first() : new TextureRegion(texQuieto);
        fallbackMovimiento = !framesMovimiento.isEmpty() ? framesMovimiento.first() : new TextureRegion(texMovimiento);

        if (texMuerte != null) {
            framesMuerte = split(texMuerte);
            animMuerte = new Animation<>(duracionMuerte(), framesMuerte, Animation.PlayMode.NORMAL);
            fallbackMuerte = !framesMuerte.isEmpty() ? framesMuerte.first() : new TextureRegion(texMuerte);
        }
    }

    protected String pathMuerte() { return null; }

    protected float duracionQuieto() { return 0.20f; }
    protected float duracionMovimiento() { return 0.12f; }
    protected float duracionMuerte() { return 0.10f; }

    // CARGA

    protected void cargar() {
        texQuieto = new Texture(Gdx.files.internal(pathQuieto()));
        texMovimiento = new Texture(Gdx.files.internal(pathMovimiento()));

        String pm = pathMuerte();
        if (pm != null && !pm.isBlank()) {
            texMuerte = new Texture(Gdx.files.internal(pm));
        }
    }

    protected abstract String pathMovimiento();

    /** Ancla al “pie” (en coords del mundo Box2D). */
    protected float anclaPie = 12f;

    // ESTADO

    public void setOffset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    // UPDATE / RENDER

    public void update(float delta) {
        stateTime += delta;
        tiempoAnim += delta;

        if (enMuerte && animMuerte != null) {
            muerteTime += delta;
            float dur = animMuerte.getAnimationDuration();
            if (muerteTime >= dur) {
                muerteTime = dur;
                muerteFinalizada = true;
            }
        }
    }
}