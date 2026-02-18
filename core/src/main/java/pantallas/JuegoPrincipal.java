package pantallas;

import com.badlogic.gdx.Screen;

import io.github.principal.Principal;
import juego.Partida;
import red.ClientThread;
import red.RedPartidaCliente;

public class JuegoPrincipal implements Screen {
    private ClientThread client;

    private final Principal game;

    private Partida partida;

    @Override
    public void dispose() {
        cerrarRed();
        if (partida != null) {
            partida.dispose();
            partida = null;
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        cerrarRed();
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

    @Override
    public void resize(int width, int height) {
        if (partida != null) partida.resize(width, height);
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

            this.client = new ClientThread(redController);
            System.out.println("[JP] ClientThread INSTANCIADO = " + client);

            partida.setClient(client);
            System.out.println("[JP] setClient OK");

            System.out.println("[JP] Thread state BEFORE start = " + client.getState());
            client.start();
            System.out.println("[JP] Thread state AFTER start = " + client.getState());

            String genero = (game.getGeneroSeleccionado() != null) ? game.getGeneroSeleccionado().name() : "MASCULINO";
            String estilo = (game.getEstiloSeleccionado() != null) ? game.getEstiloSeleccionado().name() : "CLASICO";
            redController.setMiAparienciaDeseada(genero, estilo);
            client.sendMessage("Connect:" + genero + ":" + estilo);
            System.out.println("[JP] Connect enviado (apariencia)");
        } else {
            System.out.println("[JP] ONLINE DESACTIVADO: no se crea ClientThread");
        }

        partida.startGame();
    }

    public JuegoPrincipal(Principal game) {
        this.game = game;
    }

    private void cerrarRed() {
        if (client != null) {
            ClientThread c = client;
            client = null;
            c.close();

            try {
                c.join(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
