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

    public Body getCuerpoItem(Item item) {
        return cuerposItems.get(item);
    }

    public Body getCuerpoJugador(int id) {
        Jugador j = jugadores.get(id);
        return (j != null) ? j.getCuerpoFisico() : null;
    }

    public Collection<Jugador> getJugadores() {
        return Collections.unmodifiableCollection(jugadores.values());
    }

    public GestorDeEntidades(World world) {
        this.world = world;
    }

    public Jugador getJugador(int id) {
        return jugadores.get(id);
    }

    public List<Enemigo> getEnemigosDeSala(Habitacion sala) {
        if (sala == null) return Collections.emptyList();
        List<Enemigo> lista = enemigosPorSala.get(sala);
        if (lista == null || lista.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(lista);
    }

    public List<Enemigo> getEnemigosMundo() {
        return Collections.unmodifiableList(enemigosMundo);
    }

    public List<Item> getItemsMundo() {
        return Collections.unmodifiableList(itemsMundo);
    }

    public List<PuertaVisual> getPuertasVisuales(Habitacion sala) {
        List<PuertaVisual> l = puertasPorSala.get(sala);
        return l != null ? l : Collections.emptyList();
    }

    public World getWorld() {
        return world;
    }

    public void actualizarEnemigos(float delta, Jugador j1, Jugador j2) {
        for (Enemigo e : enemigosMundo) {
            e.actualizar(delta, j1, j2);
        }
    }

    public void despawnEnemyOnline(int enemyId) {
        Enemigo e = enemigoPorIdOnline.remove(enemyId);
        if (e == null) return;
        eliminarEnemigo(e);
    }

    public void despawnItemOnline(int itemId) {
        Item item = itemPorIdOnline.remove(itemId);
        if (item == null) return;
        removerItemDelMundo(item);
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

    public void eliminarTodosLosEnemigos() {
        for (Enemigo e : new ArrayList<>(enemigosMundo)) {
            eliminarEnemigo(e);
        }
        enemigosPorSala.clear();
    }

    private final Map<Habitacion, List<PuertaVisual>> puertasPorSala = new HashMap<>();

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

    private final Map<Enemigo, Body> cuerposEnemigos = new HashMap<>();

    private final Map<Habitacion, List<Enemigo>> enemigosPorSala = new HashMap<>();

    private final Map<Item, Body> cuerposItems = new HashMap<>();

    /**
    * Crea o reubica el body de un jugador en ESTE world, usando el Jugador recibido.
    * No depende de mapas internos.
    */
    private void respawnJugadorEnWorld(Jugador jugador, float px, float py, int idDebug) {
        if (jugador == null) return;

        Body body = jugador.getCuerpoFisico();

        if (body == null || body.getWorld() != world) {
            // Si el body es null o pertenece a otro World (nivel anterior), creamos uno nuevo en este World.
            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.DynamicBody;
            bd.position.set(px, py);
            bd.fixedRotation = true;

            Body newBody = world.createBody(bd);

            CircleShape shape = new CircleShape();
            shape.setRadius(12f);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.density = 1f;
            fd.friction = 0f;

            Fixture f = newBody.createFixture(fd);
            f.setUserData("jugador");

            shape.dispose();

            // ✅ vincula body.userData = jugador también
            jugador.setCuerpoFisico(newBody);

            System.out.println("[GestorEntidades] Jugador" + idDebug + " respawn (nuevo body) en (" + px + "," + py + ")");
        } else {
            // Body válido en este World: solo teletransportamos
            body.setTransform(px, py, body.getAngle());
            body.setLinearVelocity(0f, 0f);
            body.setAwake(true);

            System.out.println("[GestorEntidades] Jugador" + idDebug + " respawn (mover) a (" + px + "," + py + ")");
        }
    }

    /**
    * Remueve un item del mundo sin aplicar efectos a ningún jugador.
    * Útil para ONLINE autoritativo.
    */
    public void removerItemDelMundo(Item item) {
        if (item == null) return;
        Body body = cuerposItems.remove(item);
        if (body != null) world.destroyBody(body);
        itemsMundo.remove(item);
    }

    /**
    * ✅ NUEVO: spawn por defecto (centro de sala con offset)
    * Todo está en píxeles.
    */
    public Vector2 calcularSpawnParaJugador(int jugadorId, Habitacion sala) {
        if (sala == null) return new Vector2(0f, 0f);

        // ✅ Tamaño real de sala en el mundo (coincide con tu lógica de puertas)
        final float ROOM_W = 512f;
        final float ROOM_H = 512f;

        float cx = sala.gridX * ROOM_W + ROOM_W / 2f;
        float cy = sala.gridY * ROOM_H + ROOM_H / 2f;

        float off = 32f;

        if (jugadorId == 1) return new Vector2(cx - off, cy);
        if (jugadorId == 2) return new Vector2(cx + off, cy);

        return new Vector2(cx, cy);
    }

    /**
    * ✅ Respawn seguro en el World actual (cuando recreás World al cambiar de nivel)
    * ✅ IMPORTANTE: usa los Jugador que recibe (j1/j2), NO el mapa interno "jugadores"
    */
    public void forzarRespawnJugadoresEnWorldActual(Jugador j1, Jugador j2, Habitacion salaActual) {
        if (salaActual == null) return;
        if (world == null) return;

        Vector2 spawn1 = calcularSpawnParaJugador(1, salaActual);
        Vector2 spawn2 = calcularSpawnParaJugador(2, salaActual);

        // ✅ Re-crear o mover el body del Jugador REAL (el que usa Partida)
        respawnJugadorEnWorld(j1, spawn1.x, spawn1.y, 1);
        respawnJugadorEnWorld(j2, spawn2.x, spawn2.y, 2);

        // ✅ mantener stats después de recrear bodies / nivel
        if (j1 != null) j1.reaplicarEfectosDeItems();
        if (j2 != null) j2.reaplicarEfectosDeItems();
    }

    /** Actualiza posición de enemigo por red. */
    public void updateEnemyOnline(int enemyId, float px, float py) {
        Enemigo e = enemigoPorIdOnline.get(enemyId);
        if (e == null) return;
        Body b = e.getCuerpoFisico();
        if (b == null) return;
        b.setTransform(px, py, b.getAngle());
        b.setLinearVelocity(0f, 0f);
    }

    /** ✅ Coop real: el item se aplica al jugador que lo recogió */
    public void recogerItem(int jugadorId, Item item) {
        if (item == null) return;

        Jugador jugador = jugadores.get(jugadorId);
        if (jugador == null) return;

        jugador.agregarObjeto(item);
        jugador.reaplicarEfectosDeItems(); // ✅ esto es clave para stats (velocidad, vidaMax, etc.)

        removerItemDelMundo(item);
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

    // ===================== ENEMIGOS =====================
    private final List<Enemigo> enemigosMundo = new ArrayList<>();

    // ===================== ITEMS / UPDATE =====================

    public void actualizar(float delta, Habitacion salaActual) {
        if (salaActual != null && salaActual.tipo == TipoSala.BOTIN) {
            intentarSpawnearItemEnBotin(salaActual);
        }
    }

    // ===================== JUGADORES =====================

    public void registrarJugador(Jugador jugador) {
        if (jugador == null) return;
        jugadores.put(jugador.getId(), jugador);
    }

    // ===================== PUERTAS VISUALES =====================

    public void registrarPuertaVisual(Habitacion sala, PuertaVisual pv) {
        puertasPorSala.computeIfAbsent(sala, k -> new ArrayList<>()).add(pv);
    }

    // ===================== RENDER =====================

    public void render(SpriteBatch batch) {
        // futuro: dibujar sprites de jugadores/items/enemigos
    }

    // ONLINE (server-driven)

    /** Spawnea un item que viene por red. No aplica efectos, solo existe para render / HUD. */
    public void spawnItemOnline(int itemId, ItemTipo tipo, float px, float py) {
        if (itemId <= 0 || tipo == null) return;
        if (itemPorIdOnline.containsKey(itemId)) return;

        // Creamos un Item “real” para reutilizar el render existente (sprites por tipo, etc.).
        // IMPORTANTE: en online NO vamos a aplicar sus efectos desde el cliente.
        Item item = tipo.crearInstancia();

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(px, py);
        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(12f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;
        Fixture fx = body.createFixture(fd);
        shape.dispose();

        fx.setUserData(item);

        itemsMundo.add(item);
        cuerposItems.put(item, body);
        itemPorIdOnline.put(itemId, item);
    }

    // ONLINE: Enemigos (server-driven)

    /** Spawnea un enemigo que viene por red. En online NO corre IA local: solo existe para render. */
    public void spawnEnemyOnline(int enemyId, String nombre, Habitacion sala, float px, float py) {
        if (enemyId <= 0) return;
        if (enemigoPorIdOnline.containsKey(enemyId)) return;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.KinematicBody; // lo movemos a mano por snapshots
        bd.position.set(px, py);
        bd.fixedRotation = true;
        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(12f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;
        Fixture fx = body.createFixture(fd);
        shape.dispose();

        fx.setUserData("enemigo");

        Enemigo e = new Enemigo((nombre != null && !nombre.isEmpty()) ? nombre : ("Enemigo" + enemyId), 0f, body, 0);
        body.setUserData(e);

        registrarEnemigo(sala, e);
        enemigoPorIdOnline.put(enemyId, e);
    }

    // Para no respawnear infinitamente ítems de BOTIN
    private final Set<Habitacion> botinesConItem = new HashSet<>();

    // Ítems tirados en el mundo
    private final List<Item> itemsMundo = new ArrayList<>();

    // ✅ ONLINE: ids estables para enemigos sincronizados por red (SpawnEnemy/UpdateEnemy/DespawnEnemy)
    private final Map<Integer, Enemigo> enemigoPorIdOnline = new HashMap<>();

    // ✅ ONLINE: ids estables para items sincronizados por red (SpawnItem/DespawnItem)
    private final Map<Integer, Item> itemPorIdOnline = new HashMap<>();

    // ✅ ahora soporta N jugadores
    private final Map<Integer, Jugador> jugadores = new HashMap<>();
}