package control.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import entidades.personajes.Jugador;

public class ControlJugador {

    private final Jugador jugador;

    private final int keyUp;
    private final int keyDown;
    private final int keyLeft;
    private final int keyRight;

    private static boolean enPausa = false;  // Flag para la pausa

    // Evita allocaciones por frame
    private final Vector2 dir = new Vector2();

    public ControlJugador(Jugador jugador, int keyUp, int keyDown, int keyLeft, int keyRight) {
        this.jugador = jugador;
        this.keyUp = keyUp;
        this.keyDown = keyDown;
        this.keyLeft = keyLeft;
        this.keyRight = keyRight;
    }

    public static void setPausa(boolean pausa) {
        enPausa = pausa;
    }

    public void actualizar(float delta) {
        Body cuerpo = jugador.getCuerpoFisico();
        if (cuerpo == null) return;
        float vstat = jugador.getVelocidad();
        Vector2 v0 = cuerpo.getLinearVelocity().cpy();

        cuerpo.setLinearVelocity(dir.x * vstat, dir.y * vstat);

        Vector2 v1 = cuerpo.getLinearVelocity().cpy();
        //Gdx.app.log("SPD", "stat=" + vstat + " before=" + v0 + " after=" + v1);

        if (enPausa || !jugador.puedeMoverse()) {
            cuerpo.setLinearVelocity(0, 0);
            return;
        }

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(keyUp)) dy += 1;
        if (Gdx.input.isKeyPressed(keyDown)) dy -= 1;
        if (Gdx.input.isKeyPressed(keyLeft)) dx -= 1;
        if (Gdx.input.isKeyPressed(keyRight)) dx += 1;

        dir.set(dx, dy);

        if (dir.len2() == 0) {
            cuerpo.setLinearVelocity(0, 0);
            return;
        }

        dir.nor();

        float velocidad = jugador.getVelocidad();

        // Si tu mundo usa PPM, descomentá:
        // velocidad /= Constantes.PPM;

        cuerpo.setLinearVelocity(dir.x * velocidad, dir.y * velocidad);
    }

}
