package entidades.enemigos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.*;

import entidades.GestorDeEntidades;
import mapa.model.Habitacion;

public class EnemigosDesdeTiled {

    // Ajustes base (los podés tunear después)
    private static final String LAYER_ENEMIGOS = "enemigos";
    private static final float RADIO_ENEMIGO = 12f;     // en pixeles (como tu jugador)
    private static final float DENSIDAD = 1f;
    private static final float FRICCION = 0f;

    private EnemigosDesdeTiled() {}

    /**
     * Spawnea enemigos definidos en Tiled para la sala actual.
     *
     * Propiedades esperadas en cada objeto (layer "enemigos"):
     * - sala (string)                -> nombre EXACTO del enum Habitacion (ej: ACERTIJO_3)
     * - nombre (string)              -> nombre/tipo de enemigo
     * - velocidad (float)            -> velocidad de movimiento (pixeles/seg)
     * - jugadorObjetivo (int)        -> 0=mas cercano, 1 o 2 fijo
     *
     * IMPORTANTE: este método asume que Box2D está en pixeles (como vos).
     */
    public static void crearEnemigosDesdeMapa(
        TiledMap map,
        Habitacion salaActual,
        World world,
        GestorDeEntidades gestor
    ) {
        if (map == null || world == null || gestor == null) return;
        if (salaActual == null) return;

        MapLayer layer = map.getLayers().get(LAYER_ENEMIGOS);
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {

            // Por simplicidad: usamos RectangleMapObject
            if (!(obj instanceof RectangleMapObject)) continue;

            // ✅ FILTRO POR SALA (nuevo)
            Habitacion salaDelEnemigo = leerSala(obj);
            if (salaDelEnemigo == null) continue;
            if (salaDelEnemigo != salaActual) continue;

            Rectangle rect = ((RectangleMapObject) obj).getRectangle();

            // Centro del rectángulo (posición spawn)
            float x = rect.x + rect.width / 2f;
            float y = rect.y + rect.height / 2f;

            String nombre = getString(obj, "nombre", "enemigo");
            float velocidad = getFloat(obj, "velocidad", 120f);
            int jugadorObjetivo = getInt(obj, "jugadorObjetivo", 0);

            // Crear body + fixture
            Body body = crearBodyEnemigo(world, x, y);

            // Crear instancia Enemigo
            Enemigo enemigo = new Enemigo(nombre, velocidad, body, jugadorObjetivo);

            // Registrar en gestor y asociar a sala
            gestor.registrarEnemigo(salaActual, enemigo);
        }
    }

    private static Habitacion leerSala(MapObject obj) {
        String salaProp = getString(obj, "sala", null);
        if (salaProp == null || salaProp.isBlank()) {
            Gdx.app.log("ENEMIGOS", "Objeto enemigo sin propiedad 'sala' -> ignorado: " + obj.getName());
            return null;
        }

        try {
            return Habitacion.valueOf(salaProp.trim());
        } catch (IllegalArgumentException e) {
            Gdx.app.log("ENEMIGOS", "Sala inválida en Tiled: '" + salaProp + "' (debe matchear Habitacion enum)");
            return null;
        }
    }

    private static Body crearBodyEnemigo(World world, float x, float y) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);
        bd.fixedRotation = true;

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(RADIO_ENEMIGO);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = DENSIDAD;
        fd.friction = FRICCION;
        fd.isSensor = true; // ✅ CLAVE: no empuja al jugador


        Fixture f = body.createFixture(fd);

        // tag opcional debug (la fuente de verdad es body.userData = Enemigo)
        f.setUserData("enemigo");

        shape.dispose();

        return body;
    }

    // ===================== Helpers de lectura propiedades =====================

    private static String getString(MapObject obj, String key, String def) {
        Object v = obj.getProperties().get(key);
        return (v != null) ? String.valueOf(v) : def;
    }

    private static int getInt(MapObject obj, String key, int def) {
        Object v = obj.getProperties().get(key);
        if (v == null) return def;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }

    private static float getFloat(MapObject obj, String key, float def) {
        Object v = obj.getProperties().get(key);
        if (v == null) return def;
        try {
            return Float.parseFloat(String.valueOf(v));
        } catch (Exception e) {
            return def;
        }
    }
}
