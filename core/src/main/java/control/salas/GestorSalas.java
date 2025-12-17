package control.salas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;

import camara.CamaraDeSala;
import entidades.GestorDeEntidades;
import entidades.personajes.Jugador;
import fisica.FisicaMundo;
import mapa.botones.*;
import mapa.generacion.*;
import mapa.model.*;
import mapa.puertas.*;

public class GestorSalas {

    private final DisposicionMapa disposicion;
    private final CamaraDeSala camara;
    private final GestorDeEntidades gestorEntidades;

    public GestorSalas(DisposicionMapa disposicion,
                       FisicaMundo fisica,
                       CamaraDeSala camara,
                       GestorDeEntidades gestorEntidades) {

        this.disposicion = disposicion;
        this.camara = camara;
        this.gestorEntidades = gestorEntidades;
        // La sala inicial y el descubrimiento se manejan desde Partida.
    }

    /**
     * Cambio de sala cuando el jugador toca una puerta.
     * @param puerta DatosPuerta del sensor tocado
     * @param jugadorQueCruzoId id del jugador (1..N) que disparó el cambio
     */
    public Habitacion irASalaVecinaPorPuerta(Habitacion salaActual, DatosPuerta puerta, int jugadorQueCruzoId) {
        if (puerta == null) return null;

        // Dirección efectiva según desde qué lado se cruza
        Direccion dirEfectiva;

        if (salaActual == puerta.origen()) {
            dirEfectiva = puerta.direccion();
        } else if (salaActual == puerta.destino()) {
            dirEfectiva = puerta.direccion().opuesta();
        } else {
            Gdx.app.log("GestorSalas",
                "ERROR: puerta no pertenece a salaActual=" + salaActual.nombreVisible +
                    " (origen=" + puerta.origen().nombreVisible + ", destino=" + puerta.destino().nombreVisible + ")");
            return null;
        }

        // Fuente de verdad: conexiones del piso
        Habitacion nuevaSala = disposicion.getDestinoEnPiso(salaActual, dirEfectiva);
        if (nuevaSala == null) {
            Gdx.app.log("GestorSalas",
                "Puerta bloqueada (sin destino en piso): " + salaActual.nombreVisible + " por " + dirEfectiva);
            return null;
        }

        Direccion entrada = dirEfectiva.opuesta();

        // ✅ Reposicionar jugadores dentro de la nueva sala
        colocarJugadoresEnSalaPorPuerta(nuevaSala, entrada, jugadorQueCruzoId);

        // Cámara (tu estilo actual: centrada por sala)
        if (camara != null) camara.centrarEn(nuevaSala);

        Gdx.app.log("SALA", "Entraste a: " + nuevaSala.nombreVisible +
            " @(" + nuevaSala.gridX + "," + nuevaSala.gridY + ")");

        return nuevaSala;
    }

    private void colocarJugadoresEnSalaPorPuerta(Habitacion sala, Direccion entrada, int jugadorQueCruzoId) {

        // Calculamos el punto base de spawn (entrada dentro de la sala)
        float[] p = calcularPuntoEntrada(sala, entrada);
        float px = p[0];
        float py = p[1];

        // Jugador que cruzó: justo en la entrada
        colocarJugadorEn(px, py, jugadorQueCruzoId);

        // El resto: cerca, con offset lateral para evitar solaparse y re-tocar sensores
        float lateral = 48f;

        for (Jugador j : gestorEntidades.getJugadores()) {
            int id = j.getId();
            if (id == jugadorQueCruzoId) continue;

            float ox = px;
            float oy = py;

            // offset perpendicular a la entrada
            switch (entrada) {
                case NORTE, SUR -> ox += lateral;
                case ESTE, OESTE -> oy += lateral;
            }

            colocarJugadorEn(ox, oy, id);

            // si hay más de 2 jugadores, apilamos offsets
            lateral += 48f;
        }
    }

    private float[] calcularPuntoEntrada(Habitacion sala, Direccion entrada) {

        float baseX = sala.gridX * sala.ancho;
        float baseY = sala.gridY * sala.alto;

        EspecificacionPuerta ep = sala.puertas.get(entrada);

        float px, py;

        if (ep == null) {
            // fallback al centro
            px = baseX + sala.ancho / 2f;
            py = baseY + sala.alto / 2f;
            return new float[]{px, py};
        }

        px = baseX + ep.localX;
        py = baseY + ep.localY;

        // Offset grande para no re-tocar el sensor al aparecer
        float offset = 64f;
        switch (entrada) {
            case NORTE -> py -= offset;
            case SUR   -> py += offset;
            case ESTE  -> px -= offset;
            case OESTE -> px += offset;
        }

        return new float[]{px, py};
    }

    private void colocarJugadorEn(float x, float y, int jugadorId) {
        Jugador j = gestorEntidades.getJugador(jugadorId);
        if (j == null) return;

        Body b = j.getCuerpoFisico();
        if (b == null) return;

        b.setLinearVelocity(0, 0);
        b.setTransform(x, y, 0);

        Gdx.app.log("REUBICACION",
            "Jugador" + jugadorId + " colocado en (" + x + "," + y + ")");
    }
}