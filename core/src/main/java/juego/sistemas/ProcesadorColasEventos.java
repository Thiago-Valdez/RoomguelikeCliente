package juego.sistemas;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

import control.puzzle.ControlPuzzlePorSala;
import entidades.GestorDeEntidades;
import juego.eventos.*;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesEntidad;
import mapa.botones.DatosBoton;
import mapa.model.Direccion;
import mapa.model.Habitacion;
import mapa.puertas.DatosPuerta;

/**
 * Procesa colas de eventos (pickup, botones, daño) para evitar modificar Box2D dentro de callbacks
 * y mantener la lógica del frame centralizada.
 */
public final class ProcesadorColasEventos {

    public void procesarItemsPendientes(
            ColaEventos eventos,
            Set<entidades.items.Item> itemsYaProcesados,
            GestorDeEntidades gestorEntidades
    ) {
        if (eventos == null || eventos.isEmpty()) return;

        itemsYaProcesados.clear();

        eventos.drenar(EventoPickup.class, ev -> {
            if (!itemsYaProcesados.add(ev.item())) return;
            gestorEntidades.recogerItem(ev.jugadorId(), ev.item());
        });
    }

    public void procesarBotonesPendientes(
        ColaEventos eventos,
        Habitacion salaActual,
        ControlPuzzlePorSala controlPuzzle,
        Consumer<Habitacion> matarEnemigosDeSalaConAnim,
        List<mapa.botones.BotonVisual> botonesVisuales
    ) {
        if (eventos == null || eventos.isEmpty()) return;

        if (controlPuzzle == null) {
            // Si todavía no hay puzzle, descartamos eventos de botones.
            eventos.limpiar(EventoBoton.class);
            return;
        }

        eventos.drenar(EventoBoton.class, ev -> {
            DatosBoton boton = ev.boton();
            int jugadorId = ev.jugadorId();

            if (boton.sala() != salaActual) return;

            boolean valido = (jugadorId == boton.jugadorId());
            if (!valido) return;

            // ✅ VISUAL: marcar DOWN/UP para TODOS los botones de esa sala + jugador
            if (botonesVisuales != null) {
                boolean down = ev.down();
                for (mapa.botones.BotonVisual bv : botonesVisuales) {
                    if (bv == null) continue;
                    if (bv.sala != salaActual) continue;
                    if (bv.jugadorId != jugadorId) continue;
                    bv.presionado = down;
                }
            }

            // ✅ LÓGICA PUZZLE
            if (ev.down()) {
                boolean desbloqueo = controlPuzzle.botonDown(salaActual, boton.jugadorId());
                if (desbloqueo) {
                    Gdx.app.log("PUZZLE", "Sala desbloqueada: " + salaActual.nombreVisible);

                    if (matarEnemigosDeSalaConAnim != null) {
                        matarEnemigosDeSalaConAnim.accept(salaActual);
                    }
                }

            } else {
                controlPuzzle.botonUp(salaActual, boton.jugadorId());
            }
        });
    }


    public void procesarDaniosPendientes(
            ColaEventos eventos,
            Set<Integer> jugadoresDanioFrame,
            GestorDeEntidades gestorEntidades,
            SistemaSpritesEntidades sprites
    ) {
        if (eventos == null || eventos.isEmpty()) return;

        jugadoresDanioFrame.clear();

        eventos.drenar(EventoDanio.class, ev -> {
            int id = ev.jugadorId();
            if (!jugadoresDanioFrame.add(id)) return; // evita daño duplicado en el mismo frame

            Jugador j = gestorEntidades.getJugador(id);
            if (j == null) return;

            // respetar inmune / enMuerte / muerto
            if (!j.estaViva() || j.estaEnMuerte() || j.esInmune()) return;

            Body body = j.getCuerpoFisico();
            if (body == null) return;

            // =========================
            // 1) Separación anti-loop (antes de congelar)
            // =========================
            float px = body.getPosition().x;
            float py = body.getPosition().y;

            float dx = px - ev.ex();
            float dy = py - ev.ey();

            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) {
                dx = 1f;
                dy = 0f;
                len2 = 1f;
            }

            float invLen = (float)(1.0 / Math.sqrt(len2));
            dx *= invLen;
            dy *= invLen;

            float separacion = 40f; // px
            body.setTransform(px + dx * separacion, py + dy * separacion, body.getAngle());
            body.setLinearVelocity(0f, 0f);
            body.setAngularVelocity(0f);

            // =========================
            // 2) Aplicar daño + cooldown anti re-hit
            // =========================
            j.recibirDanio();
            j.marcarHitCooldown(1.0f);

            // =========================
            // 3) Animación + feedback
            // =========================
            SpritesEntidad sp = (sprites != null) ? sprites.get(j) : null;
            if (sp != null) {
                sp.iniciarMuerte();
            }
        });
    }

    public void procesarPuertasPendientes(
        ColaEventos eventos,
        GestorDeEntidades gestorEntidades,
        Habitacion salaActual,
        java.util.function.Consumer<Habitacion> onCambioSala,
        boolean esOnline,
        java.util.function.Consumer<EventoPuerta> onPuertaOnline
    ) {
        if (eventos == null || eventos.isEmpty()) return;

        eventos.drenar(EventoPuerta.class, ev -> {
            DatosPuerta datos = ev.puerta();
            int jugadorId = ev.jugadorId();

            // seguridad: solo puertas de la sala actual (ORIGEN)
            if (datos.origen() != salaActual) return;

            // ONLINE: no teletransportar local, solo avisar al server
            if (esOnline) {
                if (onPuertaOnline != null) onPuertaOnline.accept(ev);
                return;
            }

            // OFFLINE: teletransporto real
            Jugador jugador = gestorEntidades.getJugador(jugadorId);
            if (jugador == null || jugador.getCuerpoFisico() == null) return;

            Vector2 spawn = calcularSpawnPuerta(datos);

            Body body = jugador.getCuerpoFisico();
            body.setTransform(spawn.x, spawn.y, body.getAngle());
            body.setLinearVelocity(0f, 0f);
            body.setAngularVelocity(0f);

            if (onCambioSala != null) onCambioSala.accept(datos.destino());
        });
    }


    private Vector2 calcularSpawnPuerta(DatosPuerta datos) {

        Habitacion destino = datos.destino();
        Direccion dir = datos.direccion();

        float margen = 32f; // separación de la puerta
        float baseX = destino.gridX * destino.ancho;
        float baseY = destino.gridY * destino.alto;

        return switch (dir) {
            case NORTE -> new Vector2(
                baseX + destino.ancho / 2f,
                baseY + margen
            );
            case SUR -> new Vector2(
                baseX + destino.ancho / 2f,
                baseY + destino.alto - margen
            );
            case ESTE -> new Vector2(
                baseX + margen,
                baseY + destino.alto / 2f
            );
            case OESTE -> new Vector2(
                baseX + destino.ancho - margen,
                baseY + destino.alto / 2f
            );
        };
    }


}
