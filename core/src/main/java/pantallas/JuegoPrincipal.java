package pantallas;

import com.badlogic.gdx.Screen;

import io.github.principal.Principal;
import juego.Partida;
import red.ClientThread;
import red.RedPartidaCliente;

public class JuegoPrincipal implements Screen {

    private final Principal game;
    private Partida partida;

    // ✅ red
    private ClientThread client;

    public JuegoPrincipal(Principal game) {
        this.game = game;
    }

    @Override
    public void show() {
        System.out.println("[JP] show()");

        boolean online = game.isModoOnline();
        System.out.println("[JP] game.isModoOnline() = " + online);

        game.audio.playJuego();

        partida = new Partida(game);
        System.out.println("[JP] Partida creada: " + partida);

        // Si no existe este método, comentá esta línea, pero dejá el print de arriba.
        partida.setModoOnline(online);

        System.out.println("[JP] antes del if ONLINE");

        if (online) {
            System.out.println("[JP] ONLINE: creando ClientThread");

            RedPartidaCliente redController = partida.getRedController();
            System.out.println("[JP] redController = " + redController);

            ClientThread client = new ClientThread(redController);
            System.out.println("[JP] ClientThread INSTANCIADO = " + client);

            partida.setClient(client);
            System.out.println("[JP] setClient OK");

            System.out.println("[JP] Thread state BEFORE start = " + client.getState());
            client.start();
            System.out.println("[JP] Thread state AFTER start = " + client.getState());

            client.sendMessage("Connect");
            System.out.println("[JP] Connect enviado");
        } else {
            System.out.println("[JP] ONLINE DESACTIVADO: no se crea ClientThread");
        }

        partida.startGame();
    }




    @Override
    public void render(float delta) {
        partida.render(delta);

        if (partida.consumirVictoriaSolicitada()) {
            cerrarRed();
            game.cambiarPantalla(new PantallaGanaste(game));
            return;
        }

        if (partida.consumirGameOverSolicitado()) {
            cerrarRed();
            game.cambiarPantalla(new PantallaGameOver(game));
        }
    }

    private void cerrarRed() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (partida != null) partida.resize(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        cerrarRed();
    }

    @Override
    public void dispose() {
        cerrarRed();
        if (partida != null) {
            partida.dispose();
            partida = null;
        }
    }
}
