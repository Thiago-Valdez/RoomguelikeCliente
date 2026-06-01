package juego.sistemas;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Body;

import entidades.Entidad;
import entidades.GestorDeEntidades;
import entidades.enemigos.Enemigo;
import entidades.items.Item;
import entidades.personajes.Jugador;
import entidades.sprites.SpritesEnemigo;
import entidades.sprites.SpritesEntidad;
import mapa.model.Habitacion;

/**
* Centraliza el manejo de sprites (jugadores + enemigos) y la cola de muertes animadas.
* + Render simple de ITEMS (sin usar SpritesEntidad).
*/
public final class SistemaSpritesEntidades {
    private final GestorDeEntidades gestorEntidades;

    public SistemaSpritesEntidades(GestorDeEntidades gestorEntidades) {
        this.gestorEntidades = gestorEntidades;
    }

    public SpritesEntidad get(Entidad e) {
        return spritesPorEntidad.get(e);
    }

    public boolean tieneItemRegistrado(Item item) {
        return visualPorItem.containsKey(item);
    }

    public void actualizarAnimJugadores(float delta) {
        for (Map.Entry<Entidad, SpritesEntidad> entry : spritesPorEntidad.entrySet()) {
            Entidad ent = entry.getKey();
            SpritesEntidad sp = entry.getValue();
            if (!(ent instanceof Jugador j) || sp == null) continue;

            var d = j.calcularDeltaAnim();
            float dx = d.x;
            float dy = d.y;

            // umbral anti-jitter (ajustalo si querés)
            boolean seMueve = (dx * dx + dy * dy) > 0.000001f;

            if (sp instanceof entidades.sprites.SpritesJugador sj) {
                sj.setMoviendo(seMueve);

                if (seMueve) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        sj.setDireccion(dx > 0 ? "RIGHT" : "LEFT");
                    } else {
                        sj.setDireccion(dy > 0 ? "UP" : "DOWN");
                    }
                }
            }
        }
    }

    public void detenerMuerte(Entidad e) {
        SpritesEntidad sp = spritesPorEntidad.get(e);
        if (sp != null) sp.detenerMuerte();
    }

    public void dispose() {
        for (SpritesEntidad s : spritesPorEntidad.values()) {
            if (s != null) s.dispose();
        }
        spritesPorEntidad.clear();
        enemigosEnMuerte.clear();
        visualPorItem.clear();
    }

    public void iniciarMuerte(Entidad e) {
        SpritesEntidad sp = spritesPorEntidad.get(e);
        if (sp != null) sp.iniciarMuerte();
    }

    public void limpiarColaMuertes() {
        enemigosEnMuerte.clear();
    }

    public void limpiarSpritesDeEntidadesMuertas() {
        if (gestorEntidades == null) return;

        Iterator<Map.Entry<Entidad, SpritesEntidad>> it = spritesPorEntidad.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entidad, SpritesEntidad> entry = it.next();
            Entidad ent = entry.getKey();

            if (ent == null) {
                if (entry.getValue() != null) entry.getValue().dispose();
                it.remove();
                continue;
            }

            if (ent instanceof Jugador) continue;

            if (ent instanceof Enemigo enemigo) {
                boolean sigueVivo = gestorEntidades.getEnemigosMundo().contains(enemigo);
                if (!sigueVivo) {
                    if (entry.getValue() != null) entry.getValue().dispose();
                    it.remove();
                }
            }
        }
    }

    public void registrarSpritesDeEnemigosVivos() {
        if (gestorEntidades == null) return;
        for (Enemigo e : gestorEntidades.getEnemigosMundo()) {
            if (e == null) continue;
            if (spritesPorEntidad.containsKey(e)) continue;

            SpritesEnemigo se = new SpritesEnemigo(e, 48, 48);
            se.setOffset(0f, -2f);
            spritesPorEntidad.put(e, se);
        }
    }

    private final Map<Entidad, SpritesEntidad> spritesPorEntidad = new HashMap<>();

    private final Set<Enemigo> enemigosEnMuerte = new HashSet<>();

    /**
    * Limpia visuals de items que ya no están en el mundo.
    * Llamar 1 vez por frame o cada tanto (barato).
    */
    public void limpiarItemsDesaparecidos() {
        if (gestorEntidades == null) return;

        Iterator<Map.Entry<Item, ItemVisual>> it = visualPorItem.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Item, ItemVisual> e = it.next();
            Item item = e.getKey();
            if (item == null) {
                it.remove();
                continue;
            }

            // Si el item ya no está en itemsMundo, lo removemos del mapa visual
            boolean sigue = gestorEntidades.getItemsMundo().contains(item);
            if (!sigue) it.remove();
        }
    }

    /**
    * Renderiza items SOLO de la sala actual (según posición del body dentro del rectángulo de sala).
    * Llamar desde CanalRenderizadoPartida, cuando el batch ya está "begin()".
    */
    public void renderItems(SpriteBatch batch, Habitacion salaActual) {
        if (batch == null || gestorEntidades == null || salaActual == null) return;

        float minX = salaActual.gridX * salaActual.ancho;
        float minY = salaActual.gridY * salaActual.alto;
        float maxX = minX + salaActual.ancho;
        float maxY = minY + salaActual.alto;

        for (Item item : gestorEntidades.getItemsMundo()) {
            ItemVisual vis = visualPorItem.get(item);
            if (item == null || vis == null || vis.region == null) continue;

            Body b = gestorEntidades.getCuerpoItem(item);
            if (b == null) continue;

            float x = b.getPosition().x;
            float y = b.getPosition().y;

            // Solo dibujar si el item está dentro de la sala actual
            if (x < minX || x > maxX || y < minY || y > maxY) continue;

            float drawX = x - vis.w / 2f + vis.offX;
            float drawY = y - vis.h / 2f + vis.offY;

            batch.draw(vis.region, drawX, drawY, vis.w, vis.h);
        }
    }

    /** Acceso solo lectura (para render). */
    public Map<Entidad, SpritesEntidad> getMapaSprites() {
        return spritesPorEntidad;
    }

    /** Dispara muerte animada para todos los enemigos de una sala. */
    public void matarEnemigosDeSalaConAnim(Habitacion sala) {
        if (gestorEntidades == null) return;
        if (sala == null) return;

        for (Enemigo e : gestorEntidades.getEnemigosDeSala(sala)) {
            SpritesEntidad sp = spritesPorEntidad.get(e);
            if (sp != null) {
                sp.iniciarMuerte();
                enemigosEnMuerte.add(e);
            } else {
                gestorEntidades.eliminarEnemigo(e);
            }
        }
    }

    /** Elimina realmente enemigos cuya animación ya terminó. */
    public void procesarEnemigosEnMuerte() {
        if (gestorEntidades == null) return;
        if (enemigosEnMuerte.isEmpty()) return;

        Iterator<Enemigo> it = enemigosEnMuerte.iterator();
        while (it.hasNext()) {
            Enemigo e = it.next();
            SpritesEntidad sp = spritesPorEntidad.get(e);

            if (sp == null) {
                it.remove();
                continue;
            }

            if (sp.muerteTerminada()) {
                gestorEntidades.eliminarEnemigo(e);
                sp.dispose();
                spritesPorEntidad.remove(e);
                it.remove();
            }
        }
    }

    // ===== ITEMS (nuevo) =====
    private static class ItemVisual {
        TextureRegion region;
        float w, h;
        float offX, offY;

        ItemVisual(TextureRegion region, float w, float h, float offX, float offY) {
            this.region = region;
            this.w = w;
            this.h = h;
            this.offX = offX;
            this.offY = offY;
        }
    }

    // ITEMS (nuevo)

    /**
    * Registra un visual para un item puntual (instancia).
    * Esto NO usa SpritesEntidad; dibuja directo con batch.draw(region,...)
    */
    public void registrarItem(Item item, TextureRegion region, float w, float h, float offX, float offY) {
        if (item == null || region == null) return;
        visualPorItem.put(item, new ItemVisual(region, w, h, offX, offY));
    }

    // JUGADORES / ENEMIGOS

    public void registrar(Entidad e, SpritesEntidad sprite, float offX, float offY) {
        if (e == null || sprite == null) return;
        sprite.setOffset(offX, offY);
        spritesPorEntidad.put(e, sprite);
    }

    // sprite por item (instancia)
    private final Map<Item, ItemVisual> visualPorItem = new HashMap<>();
}