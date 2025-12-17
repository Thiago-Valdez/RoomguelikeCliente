package fisica;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.*;

import mapa.generacion.DisposicionMapa;
import mapa.model.Direccion;
import mapa.model.Habitacion;
import mapa.puertas.EspecificacionPuerta;

public class GeneradorSensoresPuertas {

    public interface ListenerPuerta {
        void onPuertaCreada(Fixture fixture,
                            Habitacion origen,
                            Habitacion destino,
                            Direccion direccion);
    }

    private final World world;
    private final List<Habitacion> camino;
    private final Set<Habitacion> caminoSet;
    private final DisposicionMapa disposicion;

    private static final float GROSOR_MURO = 16f;

    private static final float ANCHO_PUERTA = 96f;
    private static final float ALTO_PUERTA  = 96f;

    /** 🔥 MUY IMPORTANTE: corre el sensor hacia adentro para que no se active desde la sala vecina*/
    private static final float OFFSET_SENSOR = 12f;

    public GeneradorSensoresPuertas(FisicaMundo fisica, DisposicionMapa disposicion) {
        this.world = fisica.world();
        this.disposicion = disposicion;
        this.camino = disposicion.getCamino();
        this.caminoSet = new HashSet<>(camino);
    }

    public void generar(ListenerPuerta listener) {
        Gdx.app.log("GEN_PUERTAS", "Camino tiene " + camino.size() + " salas");

        for (Habitacion h : camino) {
            crearSensoresPuertas(h, listener);
        }
    }

    private void crearMuro(float cx, float cy, float halfW, float halfH) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(cx, cy);
        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0f;
        fd.restitution = 0f;
        fd.density = 0f;

        body.createFixture(fd);
        shape.dispose();
    }

    private void crearSensoresPuertas(Habitacion origen, ListenerPuerta listener) {

        float baseX = origen.gridX * origen.ancho;
        float baseY = origen.gridY * origen.alto;

        for (var entry : origen.puertas.entrySet()) {
            Direccion dir = entry.getKey();
            EspecificacionPuerta spec = entry.getValue();

            Habitacion destino = disposicion.getDestinoEnPiso(origen, dir);

            if (destino == null || !caminoSet.contains(destino)) {
                Gdx.app.log("GEN_PUERTAS",
                    "Puerta BLOQUEADA desde " + origen.nombreVisible +
                        " por " + dir + " (sin destino en piso)");
                crearBloqueoDePuerta(origen, dir, spec);
                continue;
            }

            // Centro de la puerta en mundo (píxeles)
            float px = baseX + spec.localX;
            float py = baseY + spec.localY;

            // 🔥 Corrimiento hacia adentro para que no se active desde la sala vecina
            switch (dir) {
                case NORTE -> py -= OFFSET_SENSOR;
                case SUR   -> py += OFFSET_SENSOR;
                case ESTE  -> px -= OFFSET_SENSOR;
                case OESTE -> px += OFFSET_SENSOR;
            }

            float halfW, halfH;
            switch (dir) {
                case NORTE, SUR -> { halfW = ANCHO_PUERTA / 2f; halfH = GROSOR_MURO; }
                case ESTE, OESTE -> { halfW = GROSOR_MURO; halfH = ALTO_PUERTA / 2f; }
                default -> { halfW = ANCHO_PUERTA / 2f; halfH = ALTO_PUERTA / 2f; }
            }

            BodyDef bd = new BodyDef();
            bd.type = BodyDef.BodyType.StaticBody;
            bd.position.set(px, py);
            Body body = world.createBody(bd);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(halfW, halfH);

            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.isSensor = true;

            Fixture fixture = body.createFixture(fd);
            shape.dispose();

            if (listener != null) listener.onPuertaCreada(fixture, origen, destino, dir);

            Gdx.app.log("GEN_PUERTAS",
                "Puerta creada: " + origen.nombreVisible +
                    " --" + dir + "--> " + destino.nombreVisible);
        }
    }

    private void crearBloqueoDePuerta(Habitacion origen, Direccion dir, EspecificacionPuerta spec) {
        float baseX = origen.gridX * origen.ancho;
        float baseY = origen.gridY * origen.alto;

        float px = baseX + spec.localX;
        float py = baseY + spec.localY;

        // 🔥 También lo metemos adentro para que no “tape” del lado de la sala vecina
        switch (dir) {
            case NORTE -> py -= OFFSET_SENSOR;
            case SUR   -> py += OFFSET_SENSOR;
            case ESTE  -> px -= OFFSET_SENSOR;
            case OESTE -> px += OFFSET_SENSOR;
        }

        float halfW, halfH;
        switch (dir) {
            case NORTE, SUR -> { halfW = ANCHO_PUERTA / 2f; halfH = GROSOR_MURO / 2f; }
            case ESTE, OESTE -> { halfW = GROSOR_MURO / 2f; halfH = ALTO_PUERTA / 2f; }
            default -> { halfW = ANCHO_PUERTA / 2f; halfH = ALTO_PUERTA / 2f; }
        }

        crearMuro(px, py, halfW, halfH);
    }
}

