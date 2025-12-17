package camara;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import mapa.model.Habitacion;

/**
 * Cámara ortográfica que siempre queda centrada en la habitación actual.
 * - Soporta "snap" instantáneo o una transición suave (lerp).
 * - Usa FitViewport para mantener relación de aspecto.
 *
 * Unidades de mundo: píxeles (asumiendo que tus habitaciones usan px).
 */
public class CamaraDeSala {

    private final OrthographicCamera camara;
    private final Viewport viewport;

    // Objetivo de la interpolación (centro objetivo)
    private float objetivoX;
    private float objetivoY;

    // Config de lerp (0 = salto instantáneo; 1 = no se mueve)
    private float factorLerp = 0f; // por defecto: salto instantáneo

    // Tamaño lógico del mundo visible (en px, por ejemplo, igual a una sala)
    private final float anchoMundoVisible;
    private final float altoMundoVisible;

    public CamaraDeSala(float anchoMundoVisible, float altoMundoVisible) {
        this.anchoMundoVisible = anchoMundoVisible;
        this.altoMundoVisible = altoMundoVisible;

        this.camara = new OrthographicCamera();
        this.viewport = new FitViewport(anchoMundoVisible, altoMundoVisible, camara);

        // Centro inicial (0,0); actualizaremos cuando fijemos sala.
        this.objetivoX = 0f;
        this.objetivoY = 0f;
        camara.position.set(objetivoX, objetivoY, 0f);
        camara.update();
    }

    /** Llama en tu Screen.resize(w,h). */
    public void resize(int width, int height) {
        viewport.update(width, height, true /* center camera */);
    }

    public OrthographicCamera getCamara() { return camara; }
    public Viewport getViewport() { return viewport; }

    /**
     * Define el "snapping" o transición:
     * - factor = 0  -> snap instantáneo
     * - factor ~ 0.1..0.25 -> transición suave rápida
     * - factor ~ 0.5 -> muy suave (lento)
     */
    public void setFactorLerp(float factor) {
        this.factorLerp = factor;
    }

    /**
     * Centra instantáneamente la cámara en la habitación dada.
     */

    /** Centra la cámara en el centro de una habitación (en píxeles). */
    public void centrarEn(Habitacion habitacion) {
        if (habitacion == null) return;

        float cx = habitacion.gridX * habitacion.ancho + habitacion.ancho / 2f;
        float cy = habitacion.gridY * habitacion.alto  + habitacion.alto  / 2f;

        centrarEn(cx, cy);
    }

    public void centrarEn(float x, float y) {
        this.objetivoX = x;
        this.objetivoY = y;
        camara.position.set(x, y, 0f);
        camara.update();
    }

    /**
     * Configura el objetivo y deja que el update haga el lerp.
     * Si factorLerp = 0, el resultado es equivalente a centrarEn (salto instantáneo).
     */
    public void moverHacia(Habitacion habitacion) {
        this.objetivoX = centroX(habitacion);
        this.objetivoY = centroY(habitacion);
        if (factorLerp == 0f) {
            camara.position.set(objetivoX, objetivoY, 0f);
            camara.update();
        }
    }

    /**
     * Llamar una vez por frame (ej. en render()) si usás transición suave.
     */
    public void update(float delta) {
        if (factorLerp <= 0f) {
            // Snap duro al objetivo
            camara.position.set(objetivoX, objetivoY, 0f);
        } else {
            camara.position.x += (objetivoX - camara.position.x) * factorLerp * delta;
            camara.position.y += (objetivoY - camara.position.y) * factorLerp * delta;
        }
        camara.update();
    }

    // ==== Cálculo de centro de habitación en mundo ====

    private static float centroX(Habitacion h) {
        float worldX = h.gridX * h.ancho;
        return worldX + h.ancho * 0.5f;
    }

    private static float centroY(Habitacion h) {
        float worldY = h.gridY * h.alto;
        return worldY + h.alto * 0.5f;
    }


}
