package entidades.sprites;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import entidades.enemigos.Enemigo;

/**
* Sprite de enemigo que:
* - elige idle/move por delta de posición (NO por linearVelocity)
*   porque en online el cliente aplica setTransform() y deja velocity en 0.
* - mantiene la mirada izquierda/derecha según el movimiento real.
*/
public class SpritesEnemigo extends SpritesEntidad {
    private static final float MOVING_HOLD_S = 0.18f; // ~4 frames a 60fps / 1 tick de red

    // smoothing factor (mayor = más pegado, menor = más suave)
    private static final float SMOOTHING = 14f;

    private boolean inicializadoDelta = false;

    private float lastX, lastY;

    private float renderX, renderY;

    private float targetX, targetY;

    @Override
    public void render(SpriteBatch batch) {
        if (batch == null) return;
        if (entidad == null || entidad.getCuerpoFisico() == null) return;

        TextureRegion frame = elegirFrame();
        if (frame == null) return;

        // Render con posición suavizada (evita teleport visual).
        float px;
        float py;
        if (inicializadoSuavizado) {
            px = renderX;
            py = renderY;
        } else {
            var pos = entidad.getCuerpoFisico().getPosition();
            px = pos.x;
            py = pos.y;
        }

        float w = frame.getRegionWidth();
        float h = frame.getRegionHeight();

        float baseX = px - w / 2f;
        float y = py - anclaPie + offsetY;

        float ox = ultimaMiradaDerecha ? offsetX : -offsetX;

        if (ultimaMiradaDerecha) {
            batch.draw(frame, baseX + ox, y, w, h);
        } else {
            batch.draw(frame, baseX + ox + w, y, -w, h);
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (entidad == null || entidad.getCuerpoFisico() == null) return;
        var pos = entidad.getCuerpoFisico().getPosition();

        // init
        if (!inicializadoSuavizado) {
            inicializadoSuavizado = true;
            renderX = targetX = pos.x;
            renderY = targetY = pos.y;
        } else {
            targetX = pos.x;
            targetY = pos.y;

            // Lerp exponencial aprox: alpha = 1 - exp(-k*dt)
            float alpha = 1f - (float) Math.exp(-SMOOTHING * delta);
            if (alpha < 0f) alpha = 0f;
            if (alpha > 1f) alpha = 1f;

            renderX += (targetX - renderX) * alpha;
            renderY += (targetY - renderY) * alpha;
        }

        // Anti-parpadeo: si hubo movimiento real desde el último update, mantenemos "moving" un rato.
        if (!inicializadoDelta) {
            inicializadoDelta = true;
            lastX = targetX;
            lastY = targetY;
        } else {
            float dx = targetX - lastX;
            float dy = targetY - lastY;
            lastX = targetX;
            lastY = targetY;

            // flip por movimiento horizontal real
            if (Math.abs(dx) > 0.0001f) {
                ultimaMiradaDerecha = dx >= 0f;
            }

            if ((dx * dx + dy * dy) > EPS2) {
                movingHold = MOVING_HOLD_S;
            }
        }

        if (movingHold > 0f) {
            movingHold -= delta;
            if (movingHold < 0f) movingHold = 0f;
        }
    }

    public SpritesEnemigo(Enemigo enemigo, int frameW, int frameH) {
        super(enemigo, frameW, frameH);
        cargar();
        construirAnimaciones();
    }

    @Override
    protected String pathMovimiento() {
        Enemigo e = (Enemigo) entidad;
        return "Enemigos/" + e.getNombre() + "_movimiento.png";
    }

    @Override
    protected String pathMuerte() {
        Enemigo e = (Enemigo) entidad;
        return "Enemigos/" + e.getNombre() + "_muerte.png";
    }

    @Override
    protected String pathQuieto() {
        Enemigo e = (Enemigo) entidad;
        return "Enemigos/" + e.getNombre() + "_quieto.png";
    }

    // Suavizado + anti-parpadeo (online)
    // En online los enemigos suelen actualizarse a ~20Hz y se aplican con setTransform(),
    // lo que se ve como "teleport" y además hace que el estado idle/move parpadee.
    // Solución:
    // 1) Render con posición suavizada (lerp) hacia la posición real del body.
    // 2) Linger de "movimiento" por un corto tiempo después de detectar un update.

    private boolean inicializadoSuavizado = false;

    // Umbral anti-jitter (ajustable)
    private static final float EPS2 = 0.000001f;

    // cuánto seguimos en anim de movimiento aunque no lleguen nuevos updates
    private float movingHold = 0f;

    // ✅ CLAVE: elegimos frame por delta de posición, no por velocidad.
    @Override
    protected TextureRegion elegirFrame() {
        if (entidad == null || entidad.getCuerpoFisico() == null) return null;

        if (enMuerte) {
            if (animMuerte == null) return null;
            return animMuerte.getKeyFrame(tiempoAnim, false);
        }

        boolean seMueve = (movingHold > 0f);

        if (seMueve) {
            if (animMovimiento == null) return null;
            return animMovimiento.getKeyFrame(tiempoAnim, true);
        }

        if (animQuieto == null) return null;
        return animQuieto.getKeyFrame(tiempoAnim, true);
    }
}