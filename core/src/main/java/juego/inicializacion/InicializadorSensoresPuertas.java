package juego.inicializacion;

import fisica.FisicaMundo;
import fisica.GeneradorSensoresPuertas;
import juego.Box2dUtils;
import mapa.botones.*;
import mapa.generacion.*;
import mapa.model.*;
import mapa.puertas.*;

/**
 * Crea los sensores de puertas (Box2D) para TODA la disposición.
 *
 * Regla del proyecto: el generador solo crea puertas/sensores; las paredes vienen de Tiled.
 */
public final class InicializadorSensoresPuertas {

    private InicializadorSensoresPuertas() {}

    public static void generarSensoresPuertas(FisicaMundo fisica, DisposicionMapa disposicion, java.util.function.Consumer<RegistroPuerta> onRegistro) {
        if (fisica == null || disposicion == null || onRegistro == null) return;

        GeneradorSensoresPuertas genPuertas = new GeneradorSensoresPuertas(fisica, disposicion);
        genPuertas.generar((fixture, origen, destino, dir) -> {
            Box2dUtils.Aabb bb = Box2dUtils.aabb(fixture);
            PuertaVisual visual = new PuertaVisual(bb.minX(), bb.minY(), bb.width(), bb.height());
            DatosPuerta datos = new DatosPuerta(origen, destino, dir, visual);
            fixture.setUserData(datos);
            onRegistro.accept(new RegistroPuerta(fixture, origen, visual));
        });
    }

    /**
     * Pequeño DTO para registrar puertas visuales.
     */
    public record RegistroPuerta(com.badlogic.gdx.physics.box2d.Fixture fixture, Habitacion origen, PuertaVisual visual) {}
}