package juego.sistemas;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;

import control.puzzle.ControlPuzzlePorSala;
import fisica.FisicaMundo;
import mapa.model.Habitacion;
import mapa.model.TipoSala;
import mapa.trampilla.DatosTrampilla;
import mapa.trampilla.TrampillaVisual;

/**
 * Encapsula el comportamiento de "fin de nivel":
 * - En salas JEFE: spawnea una trampilla (sensor + visual) cuando la sala queda resuelta.
 * - Permite limpiar la trampilla al salir de la sala.
 */
public final class SistemaFinNivel {

    private Body trampillaBody;
    private TrampillaVisual trampillaVisual;
    private Habitacion salaTrampilla;

    public void actualizar(Habitacion salaActual,
                           ControlPuzzlePorSala controlPuzzle,
                           FisicaMundo fisica,
                           TextureRegion regionTrampilla) {

        if (salaActual == null || fisica == null) return;

        // Si cambiamos de sala y había trampilla, la destruimos.
        if (salaTrampilla != null && salaActual != salaTrampilla) {
            limpiar(fisica);
        }

        if (salaActual.tipo != TipoSala.JEFE) return;
        if (controlPuzzle == null) return;

        // Solo spawnea cuando la sala JEFE queda resuelta (enemigos=0 y ambos botones presionados).
        if (trampillaBody == null && controlPuzzle.estaResuelta(salaActual)) {
            spawnTrampilla(salaActual, fisica, regionTrampilla);
        }
    }

    public TrampillaVisual getTrampillaVisual() {
        return trampillaVisual;
    }

    public boolean hayTrampilla() {
        return trampillaBody != null;
    }

    public void limpiar(FisicaMundo fisica) {
        if (fisica == null) return;
        if (trampillaBody != null) {
            fisica.destruirBody(trampillaBody);
        }
        trampillaBody = null;
        trampillaVisual = null;
        salaTrampilla = null;
    }

    private void spawnTrampilla(Habitacion sala, FisicaMundo fisica, TextureRegion regionTrampilla) {
        float size = 16f;
        float baseX = sala.gridX * sala.ancho;
        float baseY = sala.gridY * sala.alto;
        float x = baseX + sala.ancho / 2f - size / 2f;
        float y = baseY + sala.alto / 2f - size / 2f;

        DatosTrampilla datos = new DatosTrampilla(sala);
        trampillaBody = fisica.crearSensorCaja(x, y, size, size, datos);
        salaTrampilla = sala;

        // Visual opcional: si no hay textura aún, igual spawnea el sensor.
        if (regionTrampilla != null) {
            trampillaVisual = new TrampillaVisual(x, y, size, size, regionTrampilla);
        }
    }
}
