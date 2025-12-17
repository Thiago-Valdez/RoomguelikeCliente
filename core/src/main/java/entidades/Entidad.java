package entidades;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Clase base para compartir atributos y comportamiento común
 * entre Jugador y Enemigo.
 *
 * Nota: NO maneja input ni IA. Eso vive en ControlJugador / ControlEnemigo.
 */
public abstract class Entidad {

    protected String nombre;
    protected float velocidad;          // unidades/segundo (escala según tu mundo Box2D)
    protected Body cuerpoFisico;

    // --- Estado vital ---
    protected boolean viva = true;
    protected boolean enMuerte = false;
    protected boolean inmune = false;

    protected float tiempoMuerte = 0f;
    protected float tiempoInmunidad = 0f;


    protected final Vector2 tmpDir = new Vector2();
    protected final Vector2 tmpPos = new Vector2();
    private final Vector2 ultimaPos = new Vector2();


    protected Entidad(String nombre, float velocidad, Body cuerpoFisico) {
        this.nombre = nombre;
        this.velocidad = velocidad;
        this.cuerpoFisico = cuerpoFisico;
    }

    // ---------- Getters / setters ----------
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public float getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(float velocidad) {
        this.velocidad = velocidad;
    }

    public Body getCuerpoFisico() {
        return cuerpoFisico;
    }

    public void setCuerpoFisico(Body cuerpoFisico) {
        this.cuerpoFisico = cuerpoFisico;
        if (cuerpoFisico != null) cuerpoFisico.setUserData(this);
    }

    public Vector2 getUltimaPos(){
        return ultimaPos;
    }

    //public final int getId() { return id; }

   /* public String getSpriteId() {
        return spriteId;
    }

    public void setSpriteId(String spriteId) {
        this.spriteId = spriteId;
    }*/

    // ---------- Helpers comunes ----------

    public Vector2 getPosicionFisica() {
        if (cuerpoFisico == null) return null;
        return cuerpoFisico.getPosition();
    }

    public float getX() {
        return cuerpoFisico != null ? cuerpoFisico.getPosition().x : 0f;
    }

    public float getY() {
        return cuerpoFisico != null ? cuerpoFisico.getPosition().y : 0f;
    }



    public void detener() {
        if (cuerpoFisico == null) return;
        cuerpoFisico.setLinearVelocity(0f, 0f);
    }








    /**
     * Mueve la entidad hacia una posición objetivo usando velocidad lineal fija.
     * Si querés “llegar y frenar” o steering más fino, después lo ajustamos.
     */
    public void moverHacia(Vector2 objetivoPos) {
        if (cuerpoFisico == null || objetivoPos == null) return;

        Vector2 pos = cuerpoFisico.getPosition();
        tmpDir.set(objetivoPos).sub(pos);

        // Si está demasiado cerca, no se mueve (evita vibración)
        if (tmpDir.len2() < 0.0001f) {
            detener();
            return;
        }

        tmpDir.nor().scl(velocidad);

        // Importante: esto replica el patrón típico que ya usás en ControlJugador
        // (velocidad constante sin fuerzas).
        cuerpoFisico.setLinearVelocity(tmpDir);
    }

    public boolean estaViva() { return viva; }
    public boolean estaEnMuerte() { return enMuerte; }
    public boolean esInmune() { return inmune; }

    /**
     * Hook de actualización. Cada subclase puede implementar su lógica.
     * - Jugador: puede quedar vacío (si lo actualizás desde ControlJugador)
     * - Enemigo: IA / timers / etc.
     */
    public void actualizar(float delta) {
        // por defecto, no hace nada
    }
}
