package entidades;

import java.util.*;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import entidades.enemigos.Enemigo;
import entidades.items.Item;
import entidades.items.ItemTipo;
import entidades.personajes.Jugador;
import mapa.model.Habitacion;
import mapa.model.TipoSala;
import mapa.puertas.PuertaVisual;

public class GestorDeEntidades {

    private final World world;

    // ✅ ahora soporta N jugadores
    private final Map<Integer, Jugador> jugadores = new HashMap<>();

    private final Map<Habitacion, List<PuertaVisual>> puertasPorSala = new HashMap<>();

    // Ítems tirados en el mundo
    private final List<Item> itemsMundo = new ArrayList<>();
    private final Map<Item, Body> cuerposItems = new HashMap<>();

    // Para no respawnear infinitamente ítems de BOTIN
    private final Set<Habitacion> botinesConItem = new HashSet<>();

    // ===================== ENEMIGOS =====================
    private final List<Enemigo> enemigosMundo = new ArrayList<>();
    private final Map<Enemigo, Body> cuerposEnemigos = new HashMap<>();
    private final Map<Habitacion, List<Enemigo>> enemigosPorSala = new HashMap<>();

    public GestorDeEntidades(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }

    // ===================== JUGADORES =====================

    public void registrarJugador(Jugador jugador) {
        if (jugador == null) return;
        jugadores.put(jugador.getId(), jugador);
    }

    public Jugador getJugador(int id) {
        return jugadores.get(id);
    }

    public Collection<Jugador> getJugadores() {
        return Collections.unmodifiableCollection(jugadores.values());
    }

    public Body getCuerpoJugador(int id) {
        Jugador j = jugadores.get(id);
        return (j != null) ? j.getCuerpoFisico() : null;
    }

    public Body crearOReposicionarJugador(int id, Habitacion sala, float px, float py) {
        Jugador jugador = jugadores.get(id);
        if (jugador == null) return null;

        Body body = jugador.getCuerpoFisico();

        if (body == null) {
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.DynamicBody;
            bd.position.set(px, py);
            bd.fixedRotation = true;

            body = world.createBody(bd);

            CircleShape shape = new CircleShape();
            shape.setRadius(12f);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            fd.friction = 0f;

            Fixture f = body.createFixture(fd);

            // ✅ tag opcional solo para debug (NO es fuente de verdad)
            f.setUserData("jugador");

            shape.dispose();

            // ✅ esto pone body.userData = jugador
            jugador.setCuerpoFisico(body);

            System.out.println("[GestorEntidades] Jugador" + id + " creado en (" + px + "," + py + ")");
        } else {
            body.setTransform(px, py, body.getAngle());
            body.setLinearVelocity(0f, 0f);

            System.out.println("[GestorEntidades] Jugador" + id + " movido a (" + px + "," + py + ")");
        }

        return body;
    }

    /**
     * ✅ NUEVO: spawn por defecto (centro de sala con offset)
     * Todo está en píxeles.
     */
    public Vector2 calcularSpawnParaJugador(int jugadorId, Habitacion sala) {
        if (sala == null) return new Vector2(0f, 0f);

        float baseX = sala.gridX * sala.ancho;
        float baseY = sala.gridY * sala.alto;

        float cx = baseX + sala.ancho / 2f;
        float cy = baseY + sala.alto / 2f;

        float off = 32f;

        if (jugadorId == 1) return new Vector2(cx - off, cy);
        if (jugadorId == 2) return new Vector2(cx + off, cy);

        return new Vector2(cx, cy);
    }

    /**
     * ✅ Respawn seguro en el World actual (cuando recreás World al cambiar de nivel)
     */
    public void forzarRespawnJugadoresEnWorldActual(
        Jugador j1,
        Jugador j2,
        Habitacion sala
    ) {
        Vector2 spawn = new Vector2(
            sala.gridX * sala.ancho + sala.ancho / 2f,
            sala.gridY * sala.alto  + sala.alto  / 2f
        );

        if (j1 != null && j1.getCuerpoFisico() != null) {
            j1.getCuerpoFisico().setTransform(spawn.x - 32f, spawn.y, 0);
            j1.getCuerpoFisico().setLinearVelocity(0, 0);
        }

        if (j2 != null && j2.getCuerpoFisico() != null) {
            j2.getCuerpoFisico().setTransform(spawn.x + 32f, spawn.y, 0);
            j2.getCuerpoFisico().setLinearVelocity(0, 0);
        }
    }


    // ===================== ENEMIGOS =====================

    public void registrarEnemigo(Habitacion sala, Enemigo enemigo) {
        if (enemigo == null) return;

        enemigosMundo.add(enemigo);

        if (enemigo.getCuerpoFisico() != null) {
            cuerposEnemigos.put(enemigo, enemigo.getCuerpoFisico());
        }

        if (sala != null) {
            enemigosPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(enemigo);
        }
    }

    public List<Enemigo> getEnemigosMundo() {
        return Collections.unmodifiableList(enemigosMundo);
    }

    public List<Enemigo> getEnemigosDeSala(Habitacion sala) {
        if (sala == null) return Collections.emptyList();
        List<Enemigo> lista = enemigosPorSala.get(sala);
        if (lista == null || lista.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(lista);
    }

    public void eliminarTodosLosEnemigos() {
        for (Enemigo e : new ArrayList<>(enemigosMundo)) {
            eliminarEnemigo(e);
        }
        enemigosPorSala.clear();
    }

    public void eliminarEnemigosDeSala(Habitacion sala) {
        if (sala == null) return;

        List<Enemigo> lista = enemigosPorSala.get(sala);
        if (lista == null || lista.isEmpty()) return;

        List<Enemigo> copia = new ArrayList<>(lista);
        for (Enemigo e : copia) {
            eliminarEnemigo(e);
        }

        enemigosPorSala.remove(sala);
    }

    public void eliminarEnemigo(Enemigo enemigo) {
        if (enemigo == null) return;

        Body b = cuerposEnemigos.remove(enemigo);
        if (b == null) b = enemigo.getCuerpoFisico();

        if (b != null) {
            world.destroyBody(b);
        }

        enemigosMundo.remove(enemigo);

        for (List<Enemigo> lista : enemigosPorSala.values()) {
            lista.remove(enemigo);
        }

        enemigosPorSala.values().removeIf(List::isEmpty);
    }

    public void actualizarEnemigos(float delta, Jugador j1, Jugador j2) {
        for (Enemigo e : enemigosMundo) {
            e.actualizar(delta, j1, j2);
        }
    }

    // ===================== PUERTAS VISUALES =====================

    public void registrarPuertaVisual(Habitacion sala, PuertaVisual pv) {
        puertasPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(pv);
    }

    public List<PuertaVisual> getPuertasVisuales(Habitacion sala) {
        List<PuertaVisual> l = puertasPorSala.get(sala);
        return l != null ? l : Collections.emptyList();
    }

    // ===================== ITEMS / UPDATE =====================

    public void actualizar(float delta, Habitacion salaActual) {
        if (salaActual != null && salaActual.tipo == TipoSala.BOTIN) {
            intentarSpawnearItemEnBotin(salaActual);
        }
    }

    private void intentarSpawnearItemEnBotin(Habitacion salaBotin) {
        if (botinesConItem.contains(salaBotin)) return;

        Item item = ItemTipo.generarAleatorioPorRareza();
        if (item == null) return;

        float baseX = salaBotin.gridX * salaBotin.ancho;
        float baseY = salaBotin.gridY * salaBotin.alto;

        float px = baseX + salaBotin.ancho / 2f;
        float py = baseY + salaBotin.alto / 2f;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(px, py);
        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(12f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;

        Fixture fixture = body.createFixture(fd);
        shape.dispose();

        fixture.setUserData(item);

        itemsMundo.add(item);
        cuerposItems.put(item, body);
        botinesConItem.add(salaBotin);
    }

    /** ✅ Coop real: el item se aplica al jugador que lo recogió */
    public void recogerItem(int jugadorId, Item item) {
        if (item == null) return;

        Jugador jugador = jugadores.get(jugadorId);
        if (jugador == null) return;

        jugador.agregarObjeto(item);
        jugador.reaplicarEfectosDeItems(); // ✅ esto es clave para stats (velocidad, vidaMax, etc.)

        Body body = cuerposItems.remove(item);
        if (body != null) world.destroyBody(body);
        itemsMundo.remove(item);
    }

    public List<Item> getItemsMundo() {
        return Collections.unmodifiableList(itemsMundo);
    }

    public Body getCuerpoItem(Item item) {
        return cuerposItems.get(item);
    }

    // ===================== RENDER =====================

    public void render(SpriteBatch batch) {
        // futuro: dibujar sprites de jugadores/items/enemigos
    }
}
