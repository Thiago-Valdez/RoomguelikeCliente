package juego.contactos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import entidades.enemigos.Enemigo;
import entidades.personajes.Jugador;
import juego.Partida;
import juego.eventos.EventoBoton;
import juego.eventos.EventoFinNivel;
import juego.eventos.EventoPickup;
import juego.eventos.EventoPuerta;
import mapa.botones.DatosBoton;
import mapa.model.Habitacion;
import mapa.puertas.DatosPuerta;
import mapa.trampilla.DatosTrampilla;

/**
* ContactListener dedicado al gameplay.
*
* Importante: NO modificamos Box2D dentro del callback.
* Solo encolamos eventos y el update de Partida los procesa.
*/
public final class EnrutadorContactosPartida implements ContactListener {
    private final Partida partida;

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        // ✅ ONLINE: las puertas las decide el SERVER (no encolamos EventoPuerta en cliente)
        if (partida.getRedController() != null && partida.getRedController().isModoOnline()) {
            // igual dejamos pickups/botones/daño para migrarlos después
        } else {
            // Puertas (robusto: puerta puede venir en A o B)
            encolarContactoPuerta(a, b);
            encolarContactoPuerta(b, a);
        }

        // Pickups
        encolarPickup(a, b);
        encolarPickup(b, a);

        // Botones (DOWN)
        encolarBoton(a, b, true);
        encolarBoton(b, a, true);

        // Trampilla (fin de nivel)
        encolarFinNivel(a, b);
        encolarFinNivel(b, a);

        // Daño
        detectarDanioJugadorEnemigo(a, b);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // Botones (UP)
        encolarBoton(a, b, false);
        encolarBoton(b, a, false);
    }

    public EnrutadorContactosPartida(Partida partida) {
        this.partida = partida;
    }

    private int getJugadorId(Fixture fx) {
        if (fx == null) return -1;
        Body b = fx.getBody();
        if (b == null) return -1;

        Object ud = b.getUserData();
        if (ud instanceof Jugador j) return j.getId();
        return -1;
    }

    private void detectarDanioJugadorEnemigo(Fixture a, Fixture b) {
        if (a == null || b == null) return;

        Object ua = a.getBody() != null ? a.getBody().getUserData() : null;
        Object ub = b.getBody() != null ? b.getBody().getUserData() : null;

        if (ua instanceof Jugador j && ub instanceof Enemigo e) {
            Vector2 pe = e.getCuerpoFisico().getPosition();
            partida.encolarDanioJugador(j.getId(), pe.x, pe.y);
            return;
        }
        if (ua instanceof Enemigo e && ub instanceof Jugador j) {
            Vector2 pe = e.getCuerpoFisico().getPosition();
            partida.encolarDanioJugador(j.getId(), pe.x, pe.y);
        }
    }

    // BOTONES
    private void encolarBoton(Fixture jugadorFx, Fixture otroFx, boolean down) {
        if (jugadorFx == null || otroFx == null) return;

        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof DatosBoton db) {
            partida.getEventos().publicar(new EventoBoton(db, jugadorId, down));
        }
    }

    // FIN NIVEL
    private void encolarFinNivel(Fixture jugadorFx, Fixture otroFx) {
        if (jugadorFx == null || otroFx == null) return;

        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof DatosTrampilla dt) {
            if (dt.sala() == partida.getSalaActual()) {
                partida.getEventos().publicar(new EventoFinNivel(dt.sala()));
            }
        }
    }

    // PICKUPS
    private void encolarPickup(Fixture jugadorFx, Fixture otroFx) {
        if (jugadorFx == null || otroFx == null) return;

        int jugadorId = getJugadorId(jugadorFx);
        if (jugadorId == -1) return;

        Object ud = otroFx.getUserData();
        if (ud instanceof entidades.items.Item item) {
            partida.getEventos().publicar(new EventoPickup(item, jugadorId));
        }
    }

    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}

    // PUERTAS (FIX)
    private void encolarContactoPuerta(Fixture posiblePuertaFx, Fixture otroFx) {
        if (posiblePuertaFx == null || otroFx == null) return;

        // si hay transición en cooldown, no generamos más eventos
        if (partida.getSistemaTransicionSala() != null
        && partida.getSistemaTransicionSala().bloqueoActivo()) {
            return;
        }

        Object ud = posiblePuertaFx.getUserData();
        if (!(ud instanceof DatosPuerta puerta)) return;

        int jugadorId = getJugadorId(otroFx);
        if (jugadorId == -1) return;

        Habitacion salaActual = partida.getSalaActual();
        if (salaActual == null) return;

        // ✅ regla correcta:
        // el sensor de esta puerta pertenece a la sala ORIGEN (lo corrés hacia adentro con OFFSET_SENSOR).
        // Si no estoy en la sala ORIGEN, ignoro.
        if (puerta.origen() != salaActual) return;

        // publicar evento
        partida.getEventos().publicar(new EventoPuerta(puerta, jugadorId));

        // DEBUG útil (dejalo hasta que funcione)
        Gdx.app.log("PUERTA",
        "EventoPuerta: J" + jugadorId + " " + puerta.origen() + " -> " + puerta.destino()
        );
    }
}