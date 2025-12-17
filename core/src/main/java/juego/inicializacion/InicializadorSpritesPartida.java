package juego.inicializacion;

import entidades.GestorDeEntidades;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesJugador;
import juego.sistemas.SistemaSpritesEntidades;

/**
 * Se encarga de inicializar y registrar los sprites asociados a jugadores y enemigos.
 * Mantiene la lógica de sprites fuera de Partida para que ésta quede como orquestador.
 */
public final class InicializadorSpritesPartida {

    private InicializadorSpritesPartida() {}

    public static SistemaSpritesEntidades crearSistemaSprites(GestorDeEntidades gestorEntidades, Jugador jugador1, Jugador jugador2) {
        SistemaSpritesEntidades sistemaSprites = new SistemaSpritesEntidades(gestorEntidades);

        // Jugadores (mantengo los mismos offsets y tamaños que estabas usando)
        sistemaSprites.registrar(jugador1, new SpritesJugador(jugador1, 48, 48), +6f, -2f);
        sistemaSprites.registrar(jugador2, new SpritesJugador(jugador2, 48, 48), +6f, -2f);

        // Enemigos (los vivos al momento de iniciar)
        sistemaSprites.registrarSpritesDeEnemigosVivos();


        return sistemaSprites;
    }
}
