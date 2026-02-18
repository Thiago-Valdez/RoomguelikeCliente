package juego.inicializacion;

import java.util.List;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.World;

import camara.CamaraDeSala;
import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.GestorDeEntidades;
import entidades.personajes.Jugador;
import fisica.FisicaMundo;
import interfaces.hud.HudJuego;
import mapa.generacion.DisposicionMapa;
import mapa.model.Habitacion;

/**
* Contenedor de TODAS las dependencias ya construidas para iniciar una partida.
*
* Esto permite extraer la inicialización fuera de {@code Partida} sin tener que
* hacer setters sueltos ni exponer campos.
*/
public final class ContextoPartida {
    public final Jugador jugador1;

    public final ControlJugador controlJugador1;

    public final ControlJugador controlJugador2;

    public final ControlPuzzlePorSala controlPuzzle;

    public final FisicaMundo fisica;

    public final GestorSalas gestorSalas;

    public final Habitacion salaActual;

    public final Jugador jugador2;

    public final List<Habitacion> salasDelPiso;

    public final List<InicializadorSensoresPuertas.RegistroPuerta> puertas;

    public final OrthogonalTiledMapRenderer mapaRenderer;

    public final ShapeRenderer shapeRendererMundo;

    // Cámara
    public final CamaraDeSala camaraSala;

    // Entidades / jugadores
    public final GestorDeEntidades gestorEntidades;

    // HUD
    public final HudJuego hud;

    // Lógico
    public final DisposicionMapa disposicion;

    // Mapa
    public final TiledMap mapaTiled;

    // Mundo físico
    public final World world;

    // Render
    public final SpriteBatch batch;

    public ContextoPartida(
    World world,
    FisicaMundo fisica,
    SpriteBatch batch,
    ShapeRenderer shapeRendererMundo,
    TiledMap mapaTiled,
    OrthogonalTiledMapRenderer mapaRenderer,
    CamaraDeSala camaraSala,
    DisposicionMapa disposicion,
    Habitacion salaActual,
    ControlPuzzlePorSala controlPuzzle,
    List<Habitacion> salasDelPiso, List<InicializadorSensoresPuertas.RegistroPuerta> puertas,
    GestorDeEntidades gestorEntidades,
    GestorSalas gestorSalas,
    Jugador jugador1,
    Jugador jugador2,
    ControlJugador controlJugador1,
    ControlJugador controlJugador2,
    HudJuego hud
    ) {
        this.world = world;
        this.fisica = fisica;
        this.batch = batch;
        this.shapeRendererMundo = shapeRendererMundo;
        this.mapaTiled = mapaTiled;
        this.mapaRenderer = mapaRenderer;
        this.camaraSala = camaraSala;
        this.disposicion = disposicion;
        this.salaActual = salaActual;
        this.controlPuzzle = controlPuzzle;
        this.salasDelPiso = salasDelPiso;
        this.puertas = puertas;
        this.gestorEntidades = gestorEntidades;
        this.gestorSalas = gestorSalas;
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.controlJugador1 = controlJugador1;
        this.controlJugador2 = controlJugador2;
        this.hud = hud;
    }
}