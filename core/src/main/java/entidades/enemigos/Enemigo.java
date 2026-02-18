package entidades.enemigos;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import entidades.Entidad;
import entidades.personajes.Jugador;

/**
* Enemigo básico.
* - No tiene vida (se elimina al resolver puzzle de sala).
* - Persigue al jugador objetivo (1/2) o al más cercano (0).
*/
public class Enemigo extends Entidad {
    @Override
    public void actualizar(float delta) {
        // intencional: se actualiza con contexto de jugadores
    }

    public Enemigo(String nombre, float velocidad, Body cuerpoFisico, int jugadorObjetivo) {
        super(nombre, velocidad, cuerpoFisico);
        this.jugadorObjetivo = jugadorObjetivo;
        if (this.cuerpoFisico != null) {
            this.cuerpoFisico.setUserData(this);
        }
    }

    public int getJugadorObjetivo() { return jugadorObjetivo; }
    public void setJugadorObjetivo(int jugadorObjetivo) { this.jugadorObjetivo = jugadorObjetivo; }

    public float getDistanciaMinima() { return distanciaMinima; }
    public void setDistanciaMinima(float distanciaMinima) {
        if (distanciaMinima < 0f) distanciaMinima = 0f;
        this.distanciaMinima = distanciaMinima;
    }

    public void actualizar(float delta, Jugador jugador1, Jugador jugador2) {
        if (cuerpoFisico == null) return;

        Jugador objetivo = elegirObjetivo(jugador1, jugador2);
        if (objetivo == null || objetivo.getCuerpoFisico() == null) {
            detenerFisico();
            return;
        }

        // ✅ posiciones desde Body
        Vector2 miPos = cuerpoFisico.getPosition();
        Vector2 posObj = objetivo.getCuerpoFisico().getPosition();

        float dx = posObj.x - miPos.x;
        float dy = posObj.y - miPos.y;

        // distancia^2 (sin sqrt)
        float d2 = dx * dx + dy * dy;
        float min2 = distanciaMinima * distanciaMinima;

        if (d2 <= min2) {
            detenerFisico();
            return;
        }

        // dirección normalizada
        tmpDir.set(dx, dy).nor();

        // velocidad = stat velocidad (tu "velocidad" de Entidad)
        float v = getVelocidad();

        cuerpoFisico.setLinearVelocity(tmpDir.x * v, tmpDir.y * v);
    }

    private Jugador elegirObjetivo(Jugador j1, Jugador j2) {
        if (jugadorObjetivo == 1) return j1;
        if (jugadorObjetivo == 2) return j2;

        // 0 u otro: más cercano válido
        if (j1 == null && j2 == null) return null;
        if (j1 == null) return j2;
        if (j2 == null) return j1;

        if (cuerpoFisico == null) return j1; // fallback

        Vector2 miPos = cuerpoFisico.getPosition();

        Body b1 = j1.getCuerpoFisico();
        Body b2 = j2.getCuerpoFisico();
        if (b1 == null && b2 == null) return null;
        if (b1 == null) return j2;
        if (b2 == null) return j1;

        Vector2 p1 = b1.getPosition();
        Vector2 p2 = b2.getPosition();

        float d1 = (p1.x - miPos.x) * (p1.x - miPos.x) + (p1.y - miPos.y) * (p1.y - miPos.y);
        float d2 = (p2.x - miPos.x) * (p2.x - miPos.x) + (p2.y - miPos.y) * (p2.y - miPos.y);

        return (d1 <= d2) ? j1 : j2;
    }

    private void detenerFisico() {
        if (cuerpoFisico != null) cuerpoFisico.setLinearVelocity(0f, 0f);
    }

    /**
    * 0 = el más cercano
    * 1 = jugador 1
    * 2 = jugador 2
    */
    private int jugadorObjetivo;

    // Parámetros simples para evitar vibración / “pegarse” al jugador
    private float distanciaMinima = 0.35f; // unidades del mundo Box2D

    // tmp para no alocar
    private final Vector2 tmpDir = new Vector2();
}