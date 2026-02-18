package entidades.personajes;

import java.util.*;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;

import entidades.Entidad;
import entidades.datos.Estilo;
import entidades.datos.Genero;
import entidades.items.Item;
import entidades.items.ItemTipo;

public class Jugador extends Entidad {

    private final int id;

    // Estética / apariencia
    private Genero genero;
    private Estilo estilo;

    // Stats específicos de jugador
    private int vida;
    private int vidaMaxima;

    private float velocidadBase = 100f;
    private float velocidadActual = 100f;

    private boolean puedeMoverse = true;
    private float cooldownDanio = 0f;

    // Inventario simple (ítems pasivos)
    private final List<Item> objetos = new ArrayList<>();

    // =========================
    // ✅ Animación por desplazamiento real (online-friendly)
    // =========================
    private final Vector2 ultimaPosAnim = new Vector2(Float.NaN, Float.NaN);
    private final Vector2 deltaAnim = new Vector2();

    public Jugador(int id, String nombre, Genero generoInicial, Estilo estiloInicial) {
        super(nombre, 100f, null);
        this.id = id;

        this.genero = (generoInicial != null) ? generoInicial : Genero.MASCULINO;
        this.estilo = (estiloInicial != null) ? estiloInicial : Estilo.CLASICO;

        this.vidaMaxima = 6;
        this.vida = 6;
    }

    public int getId() { return id; }

    // ------------------ Estética ------------------

    public Genero getGenero() { return genero; }

    public void setGenero(Genero genero) {
        if (genero != null) this.genero = genero;
    }

    public Estilo getEstilo() { return estilo; }

    public void setEstilo(Estilo estilo) {
        if (estilo != null) this.estilo = estilo;
    }

    public String getClaveSpriteBase() {
        return "player_" + genero.getSufijoSprite() + "_" + estilo.getSufijoSprite();
    }

    // ------------------ Vida ------------------

    public int getVida() { return vida; }

    public void setVida(int vida) {
        if (vida < 0) vida = 0;
        if (vida > vidaMaxima) vida = vidaMaxima;
        this.vida = vida;
    }

    public int getVidaMaxima() { return vidaMaxima; }

    public void setVidaMaxima(int vidaMaxima) {
        if (vidaMaxima < 1) vidaMaxima = 1;
        this.vidaMaxima = vidaMaxima;
        if (vida > vidaMaxima) vida = vidaMaxima;
    }


    // ------------------ Física ------------------

    @Override
    public void setCuerpoFisico(Body cuerpoFisico) {
        super.setCuerpoFisico(cuerpoFisico);
        if (this.cuerpoFisico != null) {
            this.cuerpoFisico.setUserData(this); // ✅ fuente de verdad
        }
        // ✅ inicializar tracker de anim al asignar body
        resetAnimPos();
    }

    public boolean puedeMoverse() { return puedeMoverse && viva; }

    public void recibirDanio() {
        if (!viva || enMuerte || inmune) return;

        vida--;
        enMuerte = true;
        puedeMoverse = false;
        tiempoMuerte = 0f;

        if (cuerpoFisico != null) {
            cuerpoFisico.setLinearVelocity(0f, 0f);
            cuerpoFisico.setAngularVelocity(0f);
            cuerpoFisico.setType(BodyDef.BodyType.KinematicBody);
        }

        if (vida <= 0) viva = false;
    }

    public void updateEstado(float delta) {
        if (enMuerte) {
            tiempoMuerte += delta;

            if (tiempoMuerte >= 3f) {
                enMuerte = false;
                puedeMoverse = true;
                inmune = true;
                tiempoInmunidad = 0f;
            }

            if (cuerpoFisico != null) {
                cuerpoFisico.setType(BodyDef.BodyType.DynamicBody);
            }
        }

        if (inmune) {
            tiempoInmunidad += delta;
            if (tiempoInmunidad >= 2f) inmune = false;
        }
    }

    public boolean puedeRecibirDanio() {
        return estaViva() && !enMuerte && !inmune && cooldownDanio <= 0f;
    }

    public void tick(float delta) {
        if (cooldownDanio > 0f) cooldownDanio -= delta;
        updateEstado(delta);
    }

    public float getVelocidad() { return velocidadActual; }

    public void marcarHitCooldown(float segundos) {
        cooldownDanio = Math.max(cooldownDanio, segundos);
    }

    // ------------------ Inventario ------------------

    public List<Item> getObjetos() {
        return Collections.unmodifiableList(objetos);
    }

    public void agregarObjeto(Item item) {
        if (item == null) return;
        objetos.add(item);
        reaplicarEfectosDeItems();
    }

    public void removerObjeto(Item item) {
        if (objetos.remove(item)) {
            reaplicarEfectosDeItems();
        }
    }

    public void reaplicarEfectosDeItems() {
        this.vidaMaxima = 6;
        this.velocidad = 100f;

        if (vida > vidaMaxima) vida = vidaMaxima;

        for (Item item : objetos) item.aplicarModificacion(this);
    }

    // =========================
    // ✅ ONLINE / HUD (server-driven)
    // =========================

    /**
     * Setea inventario desde servidor SIN aplicar efectos locales.
     * Formato: "TIPO1,TIPO2,..." (puede venir vacío).
     */
    public void setInventarioRemoto(String tiposCsv) {
        objetos.clear();

        if (tiposCsv == null) return;
        String s = tiposCsv.trim();
        if (s.isEmpty()) return;

        String[] parts = s.split(",");
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.isEmpty()) continue;
            try {
                ItemTipo tipo = ItemTipo.valueOf(t);
                // Crear un Item "dummy" sin efecto (solo para HUD)
                Item inst = tipo.crearInstancia();
                objetos.add(new Item(inst.getNombre(), tipo, null));
            } catch (Exception ignored) {
            }
        }
    }

    // =========================
    // ✅ Animación ONLINE: delta real por frame
    // =========================

    /** Devuelve el delta de posición desde el último frame (para decidir animación). */
    public Vector2 calcularDeltaAnim() {
        Body b = getCuerpoFisico();
        if (b == null) {
            deltaAnim.setZero();
            return deltaAnim;
        }

        Vector2 p = b.getPosition();

        // primer frame: inicializa
        if (Float.isNaN(ultimaPosAnim.x)) {
            ultimaPosAnim.set(p);
            deltaAnim.setZero();
            return deltaAnim;
        }

        deltaAnim.set(p).sub(ultimaPosAnim);
        ultimaPosAnim.set(p);
        return deltaAnim;
    }

    /**
     * Llamalo después de un teleport (puerta / UpdateRoom) para que no detecte
     * un “walk” falso por un salto grande.
     */
    public void resetAnimPos() {
        Body b = getCuerpoFisico();
        if (b == null) return;
        ultimaPosAnim.set(b.getPosition());
        deltaAnim.setZero();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entidad)) return false;
        return true;
    }

    @Override
    public final int hashCode() {
        return Integer.hashCode(id);
    }
}
