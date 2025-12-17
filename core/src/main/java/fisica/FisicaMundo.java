package fisica;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

/**
 * Mundo de Box2D + debug renderer.
 * Volvemos a usar PIXELES como unidad de Box2D.
 */
public class FisicaMundo {

    // Podés dejarlo por compatibilidad, pero no lo usamos más para escalar.
    public static final float PPM = 100f;
    private float accumulator = 0f;
    private static final float FIXED_TIMESTEP = 1f / 120f;
    private static final int VELOCITY_ITERS = 6;
    private static final int POSITION_ITERS = 2;
    private static final float MAX_FRAME_TIME = 0.25f; // anti “spiral of death”


    private final World world;
    private final Box2DDebugRenderer debugRenderer;

    /**
     * NO crea un World nuevo: usa el que le pasás.
     */
    public FisicaMundo(World world) {
        this.world = world;
        this.debugRenderer = new Box2DDebugRenderer();
    }

    public World world() {
        return world;
    }

    /**
     * Crea un sensor rectangular estático en PIXELES.
     * (x,y) es la esquina inferior-izquierda.
     */
    public Body crearSensorCaja(float x, float y, float w, float h, Object fixtureUserData) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(x + w / 2f, y + h / 2f);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(w / 2f, h / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(fixtureUserData);

        shape.dispose();
        return body;
    }

    public void destruirBody(Body body) {
        if (body == null) return;
        world.destroyBody(body);
    }

    public void setContactListener(ContactListener listener) {
        world.setContactListener(listener);
    }

    public void step(float delta) {
        // cap para evitar que si el juego se cuelga (alt-tab, breakpoint) explote la física
        float frameTime = Math.min(delta, MAX_FRAME_TIME);
        accumulator += frameTime;

        while (accumulator >= FIXED_TIMESTEP) {
            world.step(FIXED_TIMESTEP, VELOCITY_ITERS, POSITION_ITERS);
            accumulator -= FIXED_TIMESTEP;
        }
    }


    /**
     * Debug de Box2D: ahora SIN escalas raras.
     * La cámara ya está en píxeles y los cuerpos también.
     */
    public void debugDraw(OrthographicCamera camara) {
        //debugRenderer.render(world, camara.combined);
    }

    /**
     * Debug específico del jugador: solo logea bodies que sean el jugador.
     */
    public void debugLogJugador() {
        Array<Body> bodies = new Array<>();
        world.getBodies(bodies);

        int encontrados = 0;

        for (Body b : bodies) {

            boolean esJugador = false;

            // Opción 1: body.userData es el Jugador
            Object udBody = b.getUserData();
            if (udBody instanceof entidades.personajes.Jugador) {
                esJugador = true;
            }

            // Opción 2: alguna fixture tiene userData = "jugador"
            if (!esJugador) {
                for (Fixture fx : b.getFixtureList()) {
                    Object udFx = fx.getUserData();
                    if ("jugador".equals(udFx)) {
                        esJugador = true;
                        break;
                    }
                }
            }

            if (!esJugador) {
                continue; // ignoramos todo lo que no sea el jugador
            }

            encontrados++;
            Vector2 pos = b.getPosition();

            com.badlogic.gdx.Gdx.app.log(
                "DEBUG_JUGADOR",
                "Body jugador: type=" + b.getType() +
                    " pos=(" + pos.x + ", " + pos.y + ")" +
                    " worldHash=" + System.identityHashCode(world) +
                    " bodyHash=" + System.identityHashCode(b)
            );
        }

        if (encontrados == 0) {
            com.badlogic.gdx.Gdx.app.log("DEBUG_JUGADOR", "No se encontró ningún body de jugador en el world");
        }
    }

    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
    }
}
