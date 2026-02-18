package entidades.sprites;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import entidades.datos.Estilo;
import entidades.datos.Genero;
import entidades.personajes.Jugador;

/**
* Sprite de jugador que:
* - elige idle/move por delta de posición (NO por linearVelocity)
* - mantiene ultimaMiradaDerecha por dirección/DeltaX
* - respeta estado de muerte del jugador
*/
public class SpritesJugador extends SpritesEntidad {
    private Genero ultimoGeneroCargado = null;

    private final Jugador jugador;

    private Estilo ultimoEstiloCargado = null;

    private boolean usarOverrideMoviendo = false;

    private float lastX, lastY;

    @Override
    public void render(SpriteBatch batch) {
        if (batch == null) return;
        if (entidad == null || entidad.getCuerpoFisico() == null) return;

        TextureRegion frame = elegirFrame();
        if (frame == null) return;

        var pos = entidad.getCuerpoFisico().getPosition();

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

    @Override
    public void update(float delta) {
        recargarSiCambioApariencia();
        super.update(delta);
        // ✅ muerte sincronizada con estado real del jugador
        if (jugador.estaEnMuerte()) {
            iniciarMuerte();
        } else {
            if (estaEnMuerte() || muerteTerminada()) {
                detenerMuerte();
            }
        }
    }

    public SpritesJugador(Jugador jugador, int frameW, int frameH) {
        super(jugador, frameW, frameH);
        this.jugador = jugador;

        ultimoGeneroCargado = jugador.getGenero();
        ultimoEstiloCargado = jugador.getEstilo();

        cargar();
        construirAnimaciones();
    }

    @Override
    protected String pathMovimiento() {
        return elegirPath("movimiento");
    }

    @Override
    protected String pathMuerte() {
        return elegirPath("muerte");
    }

    @Override
    protected String pathQuieto() {
        return elegirPath("quieto");
    }

    private String baseConEstilo() {
        String base = (jugador.getGenero() == Genero.FEMENINO) ? "jugador_fem" : "jugador_masc";
        int estiloNum = (jugador.getEstilo() != null) ? (jugador.getEstilo().ordinal() + 1) : 1;
        return base + estiloNum; // ej: jugador_fem2
    }

    private String baseSinEstilo() {
        return (jugador.getGenero() == Genero.FEMENINO) ? "jugador_fem" : "jugador_masc";
    }

    private String elegirPath(String sufijo) {
        String conEstilo = "Jugadores/" + baseConEstilo() + "_" + sufijo + ".png";
        if (Gdx.files.internal(conEstilo).exists()) return conEstilo;

        return "Jugadores/" + baseSinEstilo() + "_" + sufijo + ".png";
    }

    private boolean calcularSeMuevePorDelta() {
        var pos = entidad.getCuerpoFisico().getPosition();
        if (!inicializadoDelta) {
            inicializadoDelta = true;
            lastX = pos.x;
            lastY = pos.y;
            return false;
        }

        float dx = pos.x - lastX;
        float dy = pos.y - lastY;

        // Guardar para próximo frame
        lastX = pos.x;
        lastY = pos.y;

        // “dominante” para orientación horizontal
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0.0001f) ultimaMiradaDerecha = true;
            else if (dx < -0.0001f) ultimaMiradaDerecha = false;
        } else {
            // si te pasaron dirección explícita, mantenela
            if ("RIGHT".equalsIgnoreCase(direccionOverride)) ultimaMiradaDerecha = true;
            if ("LEFT".equalsIgnoreCase(direccionOverride)) ultimaMiradaDerecha = false;
        }

        return (dx * dx + dy * dy) > EPS2;
    }

    private void recargarSiCambioApariencia() {
        Genero g = jugador.getGenero();
        Estilo e = jugador.getEstilo();

        if (g == ultimoGeneroCargado && e == ultimoEstiloCargado) return;

        try {
            if (texQuieto != null) texQuieto.dispose();
            if (texMovimiento != null) texMovimiento.dispose();
            if (texMuerte != null) texMuerte.dispose();
        } catch (Exception ignored) {}

        texQuieto = null;
        texMovimiento = null;
        texMuerte = null;

        // reset anim timers para evitar "saltos"
        stateTime = 0f;
        tiempoAnim = 0f;
        muerteTime = 0f;

        cargar();
        construirAnimaciones();

        ultimoGeneroCargado = g;
        ultimoEstiloCargado = e;
    }

    /**
    * Opcional: "RIGHT", "LEFT", "UP", "DOWN"
    * (Para tu sprite actual, solo afecta la mirada izquierda/derecha).
    */
    public void setDireccion(String dir) {
        this.direccionOverride = dir;
        if ("RIGHT".equalsIgnoreCase(dir)) ultimaMiradaDerecha = true;
        if ("LEFT".equalsIgnoreCase(dir)) ultimaMiradaDerecha = false;
    }

    /**
    * Si lo llamás desde tu SistemaSpritesEntidades, forzás idle/move sin depender de física.
    */
    public void setMoviendo(boolean moviendo) {
        this.usarOverrideMoviendo = true;
        this.moviendoOverride = moviendo;
    }

    /**
    * Si querés volver al modo automático (delta de posición), llamá esto.
    */
    public void clearOverride() {
        this.usarOverrideMoviendo = false;
        this.direccionOverride = null;
    }

    // --------- control de anim (online-friendly) ----------
    private boolean moviendoOverride = false;

    // Umbral anti-jitter (ajustalo si hace falta)
    private static final float EPS2 = 0.000001f;

    // ✅ CLAVE: elegimos frame por delta de posición, no por velocidad.
    @Override
    protected TextureRegion elegirFrame() {

        if (entidad == null || entidad.getCuerpoFisico() == null) return null;

        if (enMuerte) {
            if (animMuerte == null) return null;
            return animMuerte.getKeyFrame(tiempoAnim, false);
        }

        boolean seMueve = (usarOverrideMoviendo) ? moviendoOverride : calcularSeMuevePorDelta();

        if (seMueve) {
            if (animMovimiento == null) return null;
            return animMovimiento.getKeyFrame(tiempoAnim, true);
        } else {
            if (animQuieto == null) return null;
            return animQuieto.getKeyFrame(tiempoAnim, true);
        }
    }

    private String direccionOverride = null; // "RIGHT","LEFT","UP","DOWN" (opcional)

    private boolean inicializadoDelta = false;
}