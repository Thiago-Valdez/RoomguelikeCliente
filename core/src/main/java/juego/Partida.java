package juego;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;

import camara.CamaraDeSala;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import config.OpcionesPanel;
import control.input.ControlJugador;
import control.puzzle.ControlPuzzlePorSala;
import control.salas.GestorSalas;

import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.items.Item;
import entidades.items.ItemTipo;
import entidades.personajes.Jugador;

import entidades.sprites.SpritesEntidad;
import fisica.BotonesDesdeTiled;
import fisica.ColisionesDesdeTiled;
import fisica.FisicaMundo;

import interfaces.hud.HudJuego;
import interfaces.listeners.ListenerCambioSala;

import io.github.principal.Principal;

import juego.contactos.EnrutadorContactosPartida;
import juego.eventos.EventoPuerta;
import juego.inicializacion.ContextoPartida;
import juego.inicializacion.InicializadorPartida;
import juego.inicializacion.InicializadorSensoresPuertas;
import juego.inicializacion.InicializadorSpritesPartida;

import juego.sistemas.CanalRenderizadoPartida;
import juego.sistemas.ProcesadorColasEventos;
import juego.sistemas.SistemaActualizacionPartida;
import juego.sistemas.ContextoActualizacionPartida;
import juego.sistemas.SistemaFinNivel;
import juego.sistemas.SistemaSpritesEntidades;
import juego.sistemas.SistemaTransicionSala;

import juego.eventos.ColaEventos;
import juego.eventos.EventoDanio;
import juego.eventos.EventoFinNivel;

import mapa.botones.BotonVisual;
import mapa.generacion.DisposicionMapa;
import mapa.model.Habitacion;
import mapa.puertas.PuertaVisual;

// ✅ RED (modelo profe)
import red.ClientThread;
import red.RedPartidaCliente;

public class Partida {

    private final Principal game;

    // --- Mundo físico (único) ---
    private World world;
    private FisicaMundo fisica;

    // --- Render ---
    private SpriteBatch batch;
    private ShapeRenderer shapeRendererMundo;
    private ShapeRenderer debugRenderer = new ShapeRenderer();

    // --- Mapa Tiled ---
    private TiledMap mapaTiled;
    private OrthogonalTiledMapRenderer mapaRenderer;

    // --- Cámara ---
    private CamaraDeSala camaraSala;

    // --- Mapa lógico ---
    private DisposicionMapa disposicion;
    private Habitacion salaActual;
    private ControlPuzzlePorSala controlPuzzle;

    // --- Gestión ---
    private GestorSalas gestorSalas;
    private GestorDeEntidades gestorEntidades;

    // --- Jugadores ---
    private Jugador jugador1;
    private Jugador jugador2;
    private ControlJugador controlJugador1;
    private ControlJugador controlJugador2;

    // --- HUD ---
    private HudJuego hud;
    public List<Habitacion> salasDelPiso;

    private Stage pauseStage;
    private Skin skin;
    private boolean opcionesAbiertas = false;
    private com.badlogic.gdx.InputProcessor inputAnterior;

    // --- Listeners de cambio de sala (HUD, logs, etc.) ---
    private final List<ListenerCambioSala> listenersCambioSala = new ArrayList<>();

    // --- Cola unificada de eventos (evita modificar Box2D dentro del callback) ---
    private final ColaEventos eventos = new ColaEventos();
    private final Set<Item> itemsYaProcesados = new HashSet<>();
    private final Set<Integer> jugadoresDanioFrame = new HashSet<>();

    private final List<BotonVisual> botonesVisuales = new ArrayList<>();
    private Texture texBotonRojo;
    private Texture texBotonAzul;
    private TextureRegion[][] framesBotonRojo;
    private TextureRegion[][] framesBotonAzul;

    private final List<PuertaVisual> puertasVisuales = new ArrayList<>();
    private Texture texPuertaAbierta;
    private Texture texPuertaCerrada;
    private TextureRegion regPuertaAbierta;
    private TextureRegion regPuertaCerrada;

    private final control.puzzle.SincronizadorSalaOnline syncSalaOnline =
        new control.puzzle.SincronizadorSalaOnline();


    // Trampilla (visual)
    private Texture texTrampilla;
    private TextureRegion regTrampilla;

    private Map<ItemTipo, TextureRegion> spritesItems = new HashMap<>();
    private Map<ItemTipo, Texture> texturasItems = new HashMap<>();

    private boolean gameOverSolicitado = false;

    private static final int NIVEL_FINAL = 3;
    private boolean victoriaSolicitada = false;

    // Persisten durante toda la run (NO se reinician entre niveles)
    private Jugador jugador1Persistente;
    private Jugador jugador2Persistente;
    private boolean runInicializada = false;

    // --- Sistemas (extraídos) ---
    private final SistemaTransicionSala sistemaTransicionSala = new SistemaTransicionSala();
    private final ProcesadorColasEventos procesadorColasEventos = new ProcesadorColasEventos();
    private final SistemaFinNivel sistemaFinNivel = new SistemaFinNivel();

    private SistemaSpritesEntidades sistemaSprites;
    private SistemaActualizacionPartida sistemaActualizacion;
    private final ContextoActualizacionPartida ctxUpdate = new ContextoActualizacionPartida();
    private CanalRenderizadoPartida canalRenderizado;

    // --- Flags ---
    private boolean debugFisica = true;
    private int nivelActual = 1;

    // =====================
    // ✅ ONLINE SYNC (seed/nivel)
    // =====================
    private long seedActual = 0L;
    private boolean esperandoStartOnline = false;

    // =====================
    // RED (cliente) - refactor
    // =====================
    private final RedPartidaCliente redPartida = new RedPartidaCliente();
    private ClientThread client; // guardo referencia para poder cerrarlo en dispose

    public Partida(Principal game) {
        this.game = game;
    }

    public RedPartidaCliente getRedController() {
        return redPartida;
    }

    public void setClient(ClientThread client) {
        this.client = client;
        redPartida.setClient(client);
    }

    public void setModoOnline(boolean online) {
        redPartida.setModoOnline(online);

        if (!online) {
            esperandoStartOnline = false;
            seedActual = 0L;
            ControlJugador.setPausa(false);
        } else {
            // online: hasta recibir Start no inicializamos mundo
            esperandoStartOnline = true;
        }
    }

    public void startGame() {
        nivelActual = 1;

        if (redPartida.isModoOnline()) {
            // ✅ ONLINE: esperamos Start:seed:nivel
            esperandoStartOnline = true;
            Gdx.app.log("NET", "startGame(): ONLINE -> esperando Start del server...");
            return;
        }

        // ✅ LOCAL: seed propia
        seedActual = System.currentTimeMillis();
        initNivel();
    }

    // ==========================
    // EVENTOS: CAMBIO DE SALA
    // ==========================

    public void agregarListenerCambioSala(ListenerCambioSala listener) {
        if (listener != null && !listenersCambioSala.contains(listener)) {
            listenersCambioSala.add(listener);
        }
    }

    private void notificarCambioSala(Habitacion salaAnterior, Habitacion salaNueva) {
        for (ListenerCambioSala listener : listenersCambioSala) {
            listener.salaCambiada(salaAnterior, salaNueva);
        }
    }

    // ==========================
    // INIT
    // ==========================

    private void resetearEstadoNivel() {
        eventos.clear();
        itemsYaProcesados.clear();
        jugadoresDanioFrame.clear();

        botonesVisuales.clear();
        puertasVisuales.clear();

        framesBotonRojo = null;
        framesBotonAzul = null;
    }

    private void initRunSiHaceFalta() {
        if (runInicializada) return;

        jugador1Persistente = new Jugador(1, "J1", null, null);
        jugador2Persistente = new Jugador(2, "J2", null, null);

        runInicializada = true;
    }

    public void initNivel() {
        resetearEstadoNivel();
        initRunSiHaceFalta();

        // ✅ siempre usamos persistentes si existen
        Jugador j1 = (jugador1 != null) ? jugador1 : jugador1Persistente;
        Jugador j2 = (jugador2 != null) ? jugador2 : jugador2Persistente;

        // ✅ ONLINE/LOCAL: siempre entra por crearContextoNivel con seedActual

        redPartida.setMundoListo(false);
        ContextoPartida ctx = InicializadorPartida.crearContextoNivel(
            nivelActual,
            seedActual,
            j1,
            j2
        );
        aplicarContexto(ctx);

        // ✅ asegurar bodies en el world actual y spawnear en la sala actual (especialmente al cambiar de nivel)
        // Importante: al recrear el World en un nuevo nivel, hay que recrear/respawnear los bodies de los jugadores.
        if (gestorEntidades != null && jugador1 != null && jugador2 != null && salaActual != null) {
            gestorEntidades.forzarRespawnJugadoresEnWorldActual(jugador1, jugador2, salaActual);
        }

        // ✅ Mundo listo recién después de recrear World + bodies (evita crash nativo Box2D)
        redPartida.setMundoListo(true);


        // ✅ ONLINE: el cliente NO spawnea ni simula enemigos.
        // Los enemigos vienen por red (SpawnEnemy/UpdateEnemy) desde el server.
        if (redPartida.isModoOnline() && gestorEntidades != null) {
            gestorEntidades.eliminarTodosLosEnemigos();
            if (controlPuzzle != null && salaActual != null) {
                controlPuzzle.setEnemigosVivos(salaActual, 0);
            }
        }

        ColisionesDesdeTiled.crearColisiones(mapaTiled, world);


        initOverlayOpciones();

        cargarSpritesBotones();
        cargarSpritesPuertas();
        cargarSpriteTrampilla();
        cargarSpritesItems();

        if (texBotonRojo != null) texBotonRojo.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        if (texBotonAzul != null) texBotonAzul.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        BotonesDesdeTiled.crearBotones(mapaTiled, world, framesBotonRojo, framesBotonAzul, botonesVisuales);

        // 2) Sprites
        sistemaSprites = InicializadorSpritesPartida.crearSistemaSprites(gestorEntidades, jugador1, jugador2);

        // 3) Sensores de puertas
        InicializadorSensoresPuertas.generarSensoresPuertas(fisica, disposicion, reg -> {
            PuertaVisual pv = reg.visual();
            pv.setFrames(regPuertaAbierta, regPuertaCerrada);
            puertasVisuales.add(pv);
            gestorEntidades.registrarPuertaVisual(reg.origen(), reg.visual());
        });

        // 4) Listeners HUD
        agregarListenerCambioSala(hud);

        // 5) Sistemas
        sistemaActualizacion = new SistemaActualizacionPartida(
            gestorEntidades,
            fisica,
            camaraSala,
            sistemaTransicionSala,
            procesadorColasEventos,
            sistemaSprites,
            redPartida
        );

        canalRenderizado = new CanalRenderizadoPartida(
            camaraSala,
            mapaRenderer,
            shapeRendererMundo,
            batch,
            fisica,
            hud,
            gestorEntidades,
            sistemaSprites
        );

        // 6) Contactos
        fisica.setContactListener(new EnrutadorContactosPartida(this));

        // 7) Reaplicar ítems
        jugador1.reaplicarEfectosDeItems();
        jugador2.reaplicarEfectosDeItems();

        // 8) asegurar bodies

        // ✅ si venís haciendo debug, dejalo (no afecta)
        SpritesEntidad s1 = sistemaSprites.get(jugador1);
        SpritesEntidad s2 = sistemaSprites.get(jugador2);
        //System.out.println("[INIT] sprite J1=" + s1 + " J2=" + s2);
        //System.out.println("[CAM] " + camaraSala.getCamara().position);
        //System.out.println("[POS] " + jugador1.getCuerpoFisico().getPosition());

        esperandoStartOnline = false;
        Gdx.app.log("INIT", "Nivel " + nivelActual + " inicializado con seed=" + seedActual);
    }

    private void cargarSpriteTrampilla() {
        try {
            texTrampilla = new Texture(Gdx.files.internal("Trampilla/trampilla.png"));
            texTrampilla.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            regTrampilla = new TextureRegion(texTrampilla);
        } catch (Exception ex) {
            texTrampilla = null;
            regTrampilla = null;
        }
    }

    private void aplicarContexto(ContextoPartida ctx) {
        this.world = ctx.world;
        this.fisica = ctx.fisica;

        this.batch = ctx.batch;
        this.shapeRendererMundo = ctx.shapeRendererMundo;

        this.mapaTiled = ctx.mapaTiled;
        this.mapaRenderer = ctx.mapaRenderer;

        this.camaraSala = ctx.camaraSala;

        this.disposicion = ctx.disposicion;
        this.salaActual = ctx.salaActual;
        this.controlPuzzle = ctx.controlPuzzle;
        this.salasDelPiso = ctx.salasDelPiso;

        this.gestorEntidades = ctx.gestorEntidades;
        this.gestorSalas = ctx.gestorSalas;

        this.jugador1 = ctx.jugador1;
        this.jugador2 = ctx.jugador2;
        this.controlJugador1 = ctx.controlJugador1;
        this.controlJugador2 = ctx.controlJugador2;

        this.hud = ctx.hud;

        if (this.mapaTiled != null && this.world != null) {
            ColisionesDesdeTiled.crearColisiones(this.mapaTiled, this.world);
        } else {
            Gdx.app.log("INIT", "NO colisiones: mapaTiled/world null en aplicarContexto()");
        }
    }

    private void cargarSpritesBotones() {
        texBotonRojo = new Texture(Gdx.files.internal("Botones/boton_rojo.png"));
        texBotonAzul = new Texture(Gdx.files.internal("Botones/boton_azul.png"));

        framesBotonRojo = TextureRegion.split(
            texBotonRojo,
            texBotonRojo.getWidth() / 2,
            texBotonRojo.getHeight()
        );

        framesBotonAzul = TextureRegion.split(
            texBotonAzul,
            texBotonAzul.getWidth() / 2,
            texBotonAzul.getHeight()
        );

        if (framesBotonRojo.length < 1 || framesBotonRojo[0].length < 2) {
            throw new IllegalStateException("Spritesheet rojo inválido. Se esperaba 1x2 (UP/DOWN).");
        }
        if (framesBotonAzul.length < 1 || framesBotonAzul[0].length < 2) {
            throw new IllegalStateException("Spritesheet azul inválido. Se esperaba 1x2 (UP/DOWN).");
        }
    }

    private void cargarSpritesPuertas() {
        texPuertaAbierta = new Texture(Gdx.files.internal("Puertas/puerta_abierta.png"));
        texPuertaCerrada = new Texture(Gdx.files.internal("Puertas/puerta_cerrada.png"));

        texPuertaAbierta.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texPuertaCerrada.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        regPuertaAbierta = new TextureRegion(texPuertaAbierta);
        regPuertaCerrada = new TextureRegion(texPuertaCerrada);
    }

    private void cargarSpritesItems() {
        for (ItemTipo tipo : ItemTipo.values()) {
            String archivo = "items/" + tipo.name().toLowerCase() + ".png";
            Gdx.app.log("ITEM_SPRITE", "Cargando: " + archivo);

            Texture tex = new Texture(Gdx.files.internal(archivo));
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

            texturasItems.put(tipo, tex);
            spritesItems.put(tipo, new TextureRegion(tex));
        }
    }

    // ==========================
    // GETTERS mínimos
    // ==========================

    public Habitacion getSalaActual() { return salaActual; }
    public SistemaTransicionSala getSistemaTransicionSala() { return sistemaTransicionSala; }
    public ColaEventos getEventos() { return eventos; }

    public void aplicarDanioPorEnemigo(Jugador jugador, Enemigo enemigo) {
        if (jugador == null) return;
        if (jugador.estaEnMuerte() || jugador.esInmune() || !jugador.estaViva()) return;

        jugador.recibirDanio();

        if (sistemaSprites != null) sistemaSprites.iniciarMuerte(jugador);

        if (jugador.getCuerpoFisico() != null) {
            jugador.getCuerpoFisico().setLinearVelocity(0f, 0f);
        }
    }

    public void encolarDanioJugador(int jugadorId, float ex, float ey) {
        if (jugadorId <= 0) return;
        eventos.publicar(new EventoDanio(jugadorId, ex, ey));
    }

    // ==========================
    // LOOP
    // ==========================

    private void prepararContextoActualizacion(float delta) {
        ctxUpdate.delta = delta;
        ctxUpdate.salaActual = salaActual;

        ctxUpdate.jugador1 = jugador1;
        ctxUpdate.jugador2 = jugador2;
        ctxUpdate.controlJugador1 = controlJugador1;
        ctxUpdate.controlJugador2 = controlJugador2;

        ctxUpdate.eventos = eventos;
        ctxUpdate.itemsYaProcesados = itemsYaProcesados;
        ctxUpdate.jugadoresDanioFrame = jugadoresDanioFrame;

        ctxUpdate.botonesVisuales = botonesVisuales;

        ctxUpdate.controlPuzzle = controlPuzzle;
        ctxUpdate.gestorSalas = gestorSalas;
        ctxUpdate.disposicion = disposicion;
        ctxUpdate.notificarCambioSala = this::notificarCambioSala;
        ctxUpdate.mapaTiled = mapaTiled;
        ctxUpdate.world = world;
        ctxUpdate.redPartida = redPartida;
    }

    public void render(float delta) {

        // ✅ ONLINE: si todavía no llegó Start, no inicializamos ni renderizamos mundo
        if (redPartida.isModoOnline()) {
            if (redPartida.consumirStartRecibido()) {

                nivelActual = redPartida.getNivelServidor();
                seedActual = redPartida.getSeedServidor();

                int w = Gdx.graphics.getWidth();
                int h = Gdx.graphics.getHeight();

                if (sistemaFinNivel != null) {
                sistemaFinNivel.reset();
            }
            disposeNivel();
                initNivel();

                // ✅ ONLINE: el HUD debe venir SIEMPRE del server.
                // Reseteamos estado local y pedimos snapshot apenas el mundo está listo.
                redPartida.resetHudSincronizado();
                redPartida.enviarReadyOnline();

                // ✅ ONLINE: cada cliente muestra el HUD de SU jugador.
                int myId = redPartida.getMiPlayerId();
                if (hud != null && myId > 0) {
                    hud.setJugador(myId == 2 ? jugador2 : jugador1);
                }

                syncSalaOnline.reset();
                resize(w, h);
                return;
            }

            if (world == null) {
                // todavía esperando Start
                return;
            }
        }

        if (jugador1 != null && jugador1.getCuerpoFisico() != null) {
            var p = jugador1.getCuerpoFisico().getPosition();
            var c = camaraSala.getCamara().position;
            System.out.println("[DBG] CAM=(" + c.x + "," + c.y + ")  J1=(" + p.x + "," + p.y + ")");
        }

        if (world == null) return;

        // Toggle opciones con ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (opcionesAbiertas) cerrarOpciones();
            else abrirOpciones();
        }

        // ✅ ONLINE: mandar input + aplicar updates
        redPartida.enviarInputOnline(opcionesAbiertas, gameOverSolicitado);
        redPartida.aplicarUpdatesPendientes(jugador1, jugador2);

        // ✅ ONLINE: el HUD debe ser autoritativo del server (no mostrar defaults locales)
        if (redPartida.isModoOnline() && hud != null) {
            hud.setSincronizando(!redPartida.isHudSincronizado());
        } else if (hud != null) {
            hud.setSincronizando(false);
        }
        redPartida.aplicarRoomClearPendiente(controlPuzzle, sistemaSprites, salaActual);
        int loserId = redPartida.consumirGameOverLoserId();
        if (loserId > 0) {
            notificarGameOver();
        }


        // ✅ MVP: mostrar vida del otro jugador (solo UI)
        if (redPartida.isModoOnline() && hud != null) {
            int oid = redPartida.getOtherPlayerId();
            if (oid > 0) {
                hud.setOtherState(oid, redPartida.getOtherVida(), redPartida.getOtherVidaMax());
            }
        }


// ✅ ONLINE: aplicar cambio de sala enviado por el server
        if (redPartida.isModoOnline()) {
            String cambio = redPartida.consumirCambioSala(); // formato: DESTINO:DIR:PLAYER
            if (cambio != null && disposicion != null && salaActual != null) {
                try {
                    String[] p = cambio.split(":");
                    if (p.length >= 1) {
                        Habitacion destino = Habitacion.valueOf(p[0]);

                        if (destino != null && destino != salaActual) {
                            Habitacion anterior = salaActual;
                            salaActual = destino;

                            // mapa descubierto + HUD listeners
                            disposicion.descubrir(salaActual);
                            notificarCambioSala(anterior, salaActual);

                            // ✅ reset de puzzle/estado de sala
                            syncSalaOnline.onSalaCambiada(salaActual, true, controlPuzzle);

                            // ✅ cámara (solo visual)
                            if (camaraSala != null) camaraSala.centrarEn(salaActual);

                            // ✅ muy importante: avisar a redPartida
                            redPartida.onCambioSalaAplicado();
                        }
                    }
                } catch (Exception e) {
                    Gdx.app.log("NET", "Error aplicando UpdateRoom '" + cambio + "': " + e.getMessage());
                }
            }
        }
        eventos.limpiar(EventoPuerta.class);




        canalRenderizado.setPuertasVisuales(puertasVisuales);
        sincronizarEstadoPuertasVisuales();

        if (sistemaActualizacion != null) {
            prepararContextoActualizacion(delta);
            salaActual = sistemaActualizacion.actualizar(ctxUpdate);
            ctxUpdate.salaActual = salaActual;
        }

        actualizarGameOver(delta);
        if (gameOverSolicitado) return;

        if (controlPuzzle != null && gestorEntidades != null && salaActual != null) {
            int vivos = gestorEntidades.getEnemigosDeSala(salaActual).size();
            controlPuzzle.setEnemigosVivos(salaActual, vivos);
        }

        if (sistemaFinNivel != null) {
            sistemaFinNivel.actualizar(salaActual, controlPuzzle, fisica, regTrampilla);
        }

        final boolean[] avanzar = {false};
        eventos.drenar(EventoFinNivel.class, ev -> {
            if (ev.sala() == salaActual) avanzar[0] = true;
        });
        if (avanzar[0]) {
            // ✅ ONLINE: el server es autoritativo. El cliente NO avanza el nivel por su cuenta,
            // porque eso desincroniza seed/nivel/posiciones.
            if (redPartida.isModoOnline()) {
                redPartida.enviarNextLevelRequest(); // fallback (el server igual puede detectarlo por colisión)
            } else {
                avanzarAlSiguienteNivel();
            }
            return;
        }

        if (canalRenderizado != null) {
            canalRenderizado.render(
                delta,
                salaActual,
                debugFisica,
                jugador1,
                jugador2,
                botonesVisuales,
                (sistemaFinNivel != null) ? sistemaFinNivel.getTrampillaVisual() : null
            );
        }

        registrarSpritesItemsNuevos();

        if (opcionesAbiertas && pauseStage != null) {
            pauseStage.act(delta);
            pauseStage.draw();
        }

        Body b1 = jugador1.getCuerpoFisico();
        Body b2 = jugador2.getCuerpoFisico();
        if (jugador1 != null) jugador1.resetAnimPos();
        if (jugador2 != null) jugador2.resetAnimPos();
        canalRenderizado.dibujarDebugBodies(camaraSala.getCamara(), debugRenderer, b1, b2);
    }

    private void avanzarAlSiguienteNivel() {
        if (sistemaFinNivel != null && fisica != null) sistemaFinNivel.limpiar(fisica);

        if (nivelActual >= NIVEL_FINAL) {
            victoriaSolicitada = true;
            return;
        }

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        nivelActual++;

        // ✅ LOCAL: nueva seed por nivel
        if (!redPartida.isModoOnline()) {
            seedActual = System.currentTimeMillis();
        }

        disposeNivel();
        initNivel();
        resize(w, h);
    }

    private void initOverlayOpciones() {
        pauseStage = new Stage(new ScreenViewport());
        skin = new Skin(Gdx.files.internal("uiskin.json"));

        Table fondo = new Table();
        fondo.setFillParent(true);
        fondo.setBackground(skin.newDrawable("white", 0, 0, 0, 0.6f));
        fondo.center();

        OpcionesPanel panel = new OpcionesPanel(game, skin);

        TextButton cerrar = new TextButton("Cerrar", skin);
        cerrar.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                cerrarOpciones();
            }
        });

        Table contenedor = new Table();
        contenedor.add(panel.getRoot()).padBottom(20).row();
        contenedor.add(cerrar).width(220).height(55);

        fondo.add(contenedor);
        pauseStage.addActor(fondo);
    }

    private void abrirOpciones() {
        if (opcionesAbiertas) return;
        opcionesAbiertas = true;

        inputAnterior = Gdx.input.getInputProcessor();

        ControlJugador.setPausa(true);
        Gdx.input.setInputProcessor(pauseStage);
    }

    private void cerrarOpciones() {
        opcionesAbiertas = false;

        if (!redPartida.isModoOnline()) {
            ControlJugador.setPausa(false);
        }

        if (inputAnterior != null) {
            Gdx.input.setInputProcessor(inputAnterior);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    // ==========================
    // LIFECYCLE
    // ==========================

    public void resize(int width, int height) {
        if (camaraSala != null) camaraSala.getViewport().update(width, height, true);
        if (hud != null) hud.resize(width, height);
        if (pauseStage != null) pauseStage.getViewport().update(width, height, true);
    }

    public void dispose() {
        if (opcionesAbiertas) cerrarOpciones();

        // ✅ cerrar red (thread y socket)
        if (client != null) {
            try { client.close(); } catch (Exception ignored) {}
            client = null;
        }

        if (mapaRenderer != null) mapaRenderer.dispose();
        if (mapaTiled != null) mapaTiled.dispose();
        if (batch != null) batch.dispose();
        if (shapeRendererMundo != null) shapeRendererMundo.dispose();
        if (fisica != null) fisica.dispose();
        if (hud != null) hud.dispose();

        if (pauseStage != null) { pauseStage.dispose(); pauseStage = null; }
        if (skin != null) { skin.dispose(); skin = null; }

        if (sistemaSprites != null) {
            sistemaSprites.dispose();
        }

        if (texBotonRojo != null) texBotonRojo.dispose();
        if (texBotonAzul != null) texBotonAzul.dispose();

        if (texPuertaAbierta != null) texPuertaAbierta.dispose();
        if (texPuertaCerrada != null) texPuertaCerrada.dispose();

        if (texTrampilla != null) texTrampilla.dispose();

        for (Texture t : texturasItems.values()) {
            if (t != null) t.dispose();
        }
        texturasItems.clear();
        spritesItems.clear();

        resetearEstadoNivel();
        world = null;
    }

    private void disposeNivel() {

        // ✅ CRÍTICO: si los jugadores persisten entre niveles, sus bodies NO pueden quedar apuntando al World viejo
        if (jugador1 != null) jugador1.setCuerpoFisico(null);
        if (jugador2 != null) jugador2.setCuerpoFisico(null);

        // (opcional pero recomendable) también limpiar controles si dependen del body
        // if (controlJugador1 != null) controlJugador1.setBody(null);
        // if (controlJugador2 != null) controlJugador2.setBody(null);

        if (mapaRenderer != null) { mapaRenderer.dispose(); mapaRenderer = null; }
        if (mapaTiled != null) { mapaTiled.dispose(); mapaTiled = null; }

        if (batch != null) { batch.dispose(); batch = null; }
        if (shapeRendererMundo != null) { shapeRendererMundo.dispose(); shapeRendererMundo = null; }

        if (fisica != null) { fisica.dispose(); fisica = null; }
        world = null;

        if (hud != null) { hud.dispose(); hud = null; }

        if (sistemaSprites != null) { sistemaSprites.dispose(); sistemaSprites = null; }
        if (sistemaActualizacion != null) { sistemaActualizacion = null; }
        if (canalRenderizado != null) { canalRenderizado = null; }

        if (texBotonRojo != null) { texBotonRojo.dispose(); texBotonRojo = null; }
        if (texBotonAzul != null) { texBotonAzul.dispose(); texBotonAzul = null; }

        if (texPuertaAbierta != null) { texPuertaAbierta.dispose(); texPuertaAbierta = null; }
        if (texPuertaCerrada != null) { texPuertaCerrada.dispose(); texPuertaCerrada = null; }

        if (texTrampilla != null) { texTrampilla.dispose(); texTrampilla = null; }

        for (Texture t : texturasItems.values()) {
            if (t != null) t.dispose();
        }
        texturasItems.clear();
        spritesItems.clear();

        resetearEstadoNivel();
    }

    private void sincronizarEstadoPuertasVisuales() {
        if (controlPuzzle == null || salaActual == null) return;

        boolean bloqueada = controlPuzzle.estaBloqueada(salaActual);

        if (gestorEntidades != null) {
            List<PuertaVisual> puertasSala = gestorEntidades.getPuertasVisuales(salaActual);
            for (PuertaVisual pv : puertasSala) {
                if (pv == null) continue;
                pv.setAbierta(!bloqueada);
            }
            return;
        }

        for (PuertaVisual pv : puertasVisuales) {
            if (pv == null) continue;
            pv.setAbierta(!bloqueada);
        }
    }

    private void registrarSpritesItemsNuevos() {
        for (Item item : gestorEntidades.getItemsMundo()) {
            if (item == null) continue;
            if (sistemaSprites.tieneItemRegistrado(item)) continue;

            TextureRegion region = spritesItems.get(item.getTipo());
            if (region == null) continue;

            sistemaSprites.registrarItem(
                item,
                region,
                16f,
                16f,
                0f,
                0f
            );
        }
    }

    public void notificarGameOver() {
        gameOverSolicitado = true;
    }

    public boolean consumirGameOverSolicitado() {
        if (!gameOverSolicitado) return false;
        gameOverSolicitado = false;
        return true;
    }

    private void actualizarGameOver(float delta) {
        if (gameOverSolicitado) return;

        boolean j1Muerto = (jugador1 != null && !jugador1.estaViva());
        boolean j2Muerto = (jugador2 != null && !jugador2.estaViva());

        if (j1Muerto || j2Muerto) {
            gameOverSolicitado = true;
        }
    }

    public boolean consumirVictoriaSolicitada() {
        if (!victoriaSolicitada) return false;
        victoriaSolicitada = false;
        return true;
    }
}
