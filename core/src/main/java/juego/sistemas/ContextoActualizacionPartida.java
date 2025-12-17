package juego.sistemas;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.physics.box2d.World;

import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import red.RedPartidaCliente;
import juego.eventos.ColaEventos;
import entidades.items.Item;
import entidades.personajes.Jugador;
import mapa.botones.BotonVisual;
import mapa.generacion.DisposicionMapa;
import mapa.model.Habitacion;

/**
 * Contexto del loop (update) de la partida.
 * Agrupa dependencias y colas para evitar firmas gigantes en los sistemas.
 *
 * NOTA: este contexto es mutable y se reutiliza cada frame.
 */
public final class ContextoActualizacionPartida {

    public float delta;

    // Estado actual
    public Habitacion salaActual;

    // Jugadores + input
    public Jugador jugador1;
    public Jugador jugador2;
    public ControlJugador controlJugador1;
    public ControlJugador controlJugador2;

    // Cola unificada de eventos del juego
    public ColaEventos eventos;

    // Helpers para dedupe por frame (se pueden mover a un sistema luego)
    public Set<Item> itemsYaProcesados;
    public Set<Integer> jugadoresDanioFrame;

    // Visuales
    public List<BotonVisual> botonesVisuales;

    // Dependencias de sala / mapa
    public ControlPuzzlePorSala controlPuzzle;
    public GestorSalas gestorSalas;
    public DisposicionMapa disposicion;
    public BiConsumer<Habitacion, Habitacion> notificarCambioSala;

    public TiledMap mapaTiled;
    public World world;

    // Red (solo ONLINE)
    public RedPartidaCliente redPartida;

    public boolean esOnline = true;
}
