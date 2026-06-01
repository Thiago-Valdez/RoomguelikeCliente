package juego.inicializacion;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import camara.CamaraDeSala;
import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;
import entidades.Entidad;
import entidades.GestorDeEntidades;
import entidades.datos.*;
import entidades.enemigos.*;
import entidades.items.*;
import entidades.personajes.*;
import entidades.sprites.*;
import fisica.BotonesDesdeTiled;
import fisica.ColisionesDesdeTiled;
import fisica.FisicaMundo;
import interfaces.hud.HudJuego;
import juego.sistemas.SistemaSpritesEntidades;
import mapa.botones.*;
import mapa.generacion.*;
import mapa.model.*;
import mapa.puertas.*;

/**
* Construye y deja listo el contexto inicial de una partida.
*
* Regla: paredes/botones desde Tiled, y el generador solo crea sensores de puertas.
*/
public final class InicializadorPartida {
    public static ContextoPartida crearContextoInicial(int nivel) {

        // Render
        SpriteBatch batch = new SpriteBatch();
        ShapeRenderer shapeRendererMundo = new ShapeRenderer();

        // Mapa lógico
        GeneradorMapa.Configuracion cfg = new GeneradorMapa.Configuracion();
        cfg.nivel = Math.max(1, nivel);
        cfg.semilla = System.currentTimeMillis();

        List<Habitacion> todasLasHabitaciones = Arrays.asList(Habitacion.values());
        GrafoPuertas grafo = new GrafoPuertas(todasLasHabitaciones, new Random(cfg.semilla));

        GeneradorMapa generador = new GeneradorMapa(cfg, grafo);
        DisposicionMapa disposicion = generador.generar();
        List<Habitacion> salasDelPiso = generador.salasDelPiso;

        Habitacion salaActual = disposicion.salaInicio();
        disposicion.descubrir(salaActual);

        ControlPuzzlePorSala controlPuzzle = new ControlPuzzlePorSala();
        controlPuzzle.alEntrarASala(salaActual);

        CamaraDeSala camaraSala = new CamaraDeSala(512f, 512f);
        camaraSala.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camaraSala.setFactorLerp(0f);
        camaraSala.centrarEn(salaActual);

        // Fisica + mapa Tiled
        World world = new World(new Vector2(0, 0), true);
        FisicaMundo fisica = new FisicaMundo(world);

        TiledMap mapaTiled = new TmxMapLoader().load("TMX/mapa.tmx");
        OrthogonalTiledMapRenderer mapaRenderer = new OrthogonalTiledMapRenderer(mapaTiled, 1f);

        // Paredes/botones desde Tiled
        ColisionesDesdeTiled.crearColisiones(mapaTiled, world);

        // Puertas (sensores desde generador)
        List<InicializadorSensoresPuertas.RegistroPuerta> puertas = new ArrayList<>();

        InicializadorSensoresPuertas.generarSensoresPuertas(
        fisica,
        disposicion,
        registro -> {
            // ESTE ES EL CALLBACK: se ejecuta por cada puerta creada
            puertas.add(registro);
        }
        );

        // Entidades / jugadores
        float baseX = salaActual.gridX * salaActual.ancho;
        float baseY = salaActual.gridY * salaActual.alto;
        float px = baseX + salaActual.ancho / 2f;
        float py = baseY + salaActual.alto / 2f;

        Jugador jugador1 = new Jugador(1, "Jugador 1", Genero.MASCULINO, Estilo.CLASICO);
        Jugador jugador2 = new Jugador(2, "Jugador 2", Genero.FEMENINO, Estilo.CLASICO);

        GestorDeEntidades gestorEntidades = new GestorDeEntidades(world);
        gestorEntidades.registrarJugador(jugador1);
        gestorEntidades.registrarJugador(jugador2);

        gestorEntidades.crearOReposicionarJugador(1, salaActual, px - 32f, py);
        gestorEntidades.crearOReposicionarJugador(2, salaActual, px + 32f, py);

        // Spawn enemigos desde Tiled (solo sala actual)
        EnemigosDesdeTiled.crearEnemigosDesdeMapa(mapaTiled, salaActual, world, gestorEntidades);

        // Condición de combate/jefe: enemigos vivos (para bloqueo en COMBATE/JEFE)
        controlPuzzle.setEnemigosVivos(salaActual, gestorEntidades.getEnemigosDeSala(salaActual).size());

        // Cámara al promedio de jugadores
        Vector2 p1 = jugador1.getCuerpoFisico().getPosition();
        Vector2 p2 = jugador2.getCuerpoFisico().getPosition();
        camaraSala.centrarEn((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f);

        ControlJugador controlJugador1 = new ControlJugador(jugador1, Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D);
        ControlJugador controlJugador2 = new ControlJugador(jugador2, Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT);

        GestorSalas gestorSalas = new GestorSalas(disposicion, fisica, camaraSala, gestorEntidades);

        // HUD
        HudJuego hud = new HudJuego(disposicion, jugador1);
        hud.actualizarSalaActual(salaActual);

        return new ContextoPartida(
        world,
        fisica,
        batch,
        shapeRendererMundo,
        mapaTiled,
        mapaRenderer,
        camaraSala,
        disposicion,
        salaActual,
        controlPuzzle,
        salasDelPiso,
        puertas,
        gestorEntidades,
        gestorSalas,
        jugador1,
        jugador2,
        controlJugador1,
        controlJugador2,
        hud
        );
    }

    private InicializadorPartida() {}

    public static ContextoPartida crearContextoInicial() {
        return crearContextoInicial(1);
    }

    public static ContextoPartida crearContextoNivel(    int nivel,
    long semilla,
    Jugador jugador1Existente,
    Jugador jugador2Existente) {

        // Render
        SpriteBatch batch = new SpriteBatch();
        ShapeRenderer shapeRendererMundo = new ShapeRenderer();

        // Mapa lógico
        GeneradorMapa.Configuracion cfg = new GeneradorMapa.Configuracion();
        cfg.nivel = Math.max(1, nivel);
        cfg.semilla = semilla; // ✅ NO timeMillis en online

        List<Habitacion> todasLasHabitaciones = Arrays.asList(Habitacion.values());
        GrafoPuertas grafo = new GrafoPuertas(todasLasHabitaciones, new Random(cfg.semilla));

        GeneradorMapa generador = new GeneradorMapa(cfg, grafo);
        DisposicionMapa disposicion = generador.generar();
        List<Habitacion> salasDelPiso = generador.salasDelPiso;

        Habitacion salaActual = disposicion.salaInicio();
        disposicion.descubrir(salaActual);

        ControlPuzzlePorSala controlPuzzle = new ControlPuzzlePorSala();
        controlPuzzle.alEntrarASala(salaActual);

        CamaraDeSala camaraSala = new CamaraDeSala(512f, 512f);
        camaraSala.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camaraSala.setFactorLerp(0f);
        camaraSala.centrarEn(salaActual);

        // Fisica + mapa Tiled
        World world = new World(new Vector2(0, 0), true);
        FisicaMundo fisica = new FisicaMundo(world);

        TiledMap mapaTiled = new TmxMapLoader().load("TMX/mapa.tmx");
        OrthogonalTiledMapRenderer mapaRenderer = new OrthogonalTiledMapRenderer(mapaTiled, 1f);

        ColisionesDesdeTiled.crearColisiones(mapaTiled, world);

        // Puertas (sensores desde generador)
        List<InicializadorSensoresPuertas.RegistroPuerta> puertas = new ArrayList<>();

        InicializadorSensoresPuertas.generarSensoresPuertas(
        fisica,
        disposicion,
        puertas::add
        );

        // Entidades / jugadores (REUSO)
        float baseX = salaActual.gridX * salaActual.ancho;
        float baseY = salaActual.gridY * salaActual.alto;
        float px = baseX + salaActual.ancho / 2f;
        float py = baseY + salaActual.alto / 2f;

        Jugador jugador1 = (jugador1Existente != null)
        ? jugador1Existente
        : new Jugador(1, "Jugador 1", Genero.MASCULINO, Estilo.CLASICO);

        Jugador jugador2 = (jugador2Existente != null)
        ? jugador2Existente
        : new Jugador(2, "Jugador 2", Genero.FEMENINO, Estilo.CLASICO);

        // Bodies viejos no sirven si el world cambia
        jugador1.setCuerpoFisico(null);
        jugador2.setCuerpoFisico(null);

        GestorDeEntidades gestorEntidades = new GestorDeEntidades(world);
        gestorEntidades.registrarJugador(jugador1);
        gestorEntidades.registrarJugador(jugador2);

        gestorEntidades.crearOReposicionarJugador(1, salaActual, px - 32f, py);
        gestorEntidades.crearOReposicionarJugador(2, salaActual, px + 32f, py);

        // Spawn enemigos desde Tiled (solo sala actual)
        EnemigosDesdeTiled.crearEnemigosDesdeMapa(mapaTiled, salaActual, world, gestorEntidades);

        controlPuzzle.setEnemigosVivos(salaActual, gestorEntidades.getEnemigosDeSala(salaActual).size());

        // Cámara al promedio de jugadores
        Vector2 p1 = jugador1.getCuerpoFisico().getPosition();
        Vector2 p2 = jugador2.getCuerpoFisico().getPosition();
        camaraSala.centrarEn((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f);

        // Controles nuevos, apuntan a los jugadores persistentes
        ControlJugador controlJugador1 = new ControlJugador(jugador1, Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D);
        ControlJugador controlJugador2 = new ControlJugador(jugador2, Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT);

        GestorSalas gestorSalas = new GestorSalas(disposicion, fisica, camaraSala, gestorEntidades);

        // ✅ Reaplicar items/efectos persistentes
        jugador1.reaplicarEfectosDeItems();
        jugador2.reaplicarEfectosDeItems();

        // HUD
        HudJuego hud = new HudJuego(disposicion, jugador1);
        hud.actualizarSalaActual(salaActual);

        return new ContextoPartida(
        world,
        fisica,
        batch,
        shapeRendererMundo,
        mapaTiled,
        mapaRenderer,
        camaraSala,
        disposicion,
        salaActual,
        controlPuzzle,
        salasDelPiso,
        puertas,
        gestorEntidades,
        gestorSalas,
        jugador1,
        jugador2,
        controlJugador1,
        controlJugador2,
        hud
        );
    }
}