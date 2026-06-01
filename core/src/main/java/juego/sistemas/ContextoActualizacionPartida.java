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
    public TiledMap mapaTiled;

    public boolean esOnline = true;

    public float delta;

    public BiConsumer<Habitacion, Habitacion> notificarCambioSala;

    public ControlJugador controlJugador1;

    public ControlJugador controlJugador2;

    public DisposicionMapa disposicion;

    public GestorSalas gestorSalas;

    public Jugador jugador2;

    public Set<Integer> jugadoresDanioFrame;

    public World world;

    // Cola unificada de eventos del juego
    public ColaEventos eventos;

    // Dependencias de sala / mapa
    public ControlPuzzlePorSala controlPuzzle;

    // Estado actual
    public Habitacion salaActual;

    // Helpers para dedupe por frame (se pueden mover a un sistema luego)
    public Set<Item> itemsYaProcesados;

    // Jugadores + input
    public Jugador jugador1;

    // Red (solo ONLINE)
    public RedPartidaCliente redPartida;

    // Visuales
    public List<BotonVisual> botonesVisuales;
}