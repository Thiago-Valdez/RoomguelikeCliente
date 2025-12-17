package interfaces.hud;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import entidades.items.Item;
import entidades.items.ItemTipo;
import entidades.personajes.Jugador;
import interfaces.listeners.ListenerCambioSala;
import mapa.generacion.DisposicionMapa;
import mapa.minimapa.LayoutMinimapa;
import mapa.minimapa.PosMini;
import mapa.model.Habitacion;

public class HudJuego implements Disposable, ListenerCambioSala {

    private static final float HUD_W = 960f;
    private static final float HUD_H = 540f;

    // === Paths HUD (cambiá nombres si los tuyos son distintos) ===
    private static final String PATH_HEART_FULL  = "Hud/corazon_lleno.png";
    private static final String PATH_HEART_EMPTY = "Hud/corazon_vacio.png";
    private static final String PATH_SLOT        = "Hud/slot.png";

    // === Items (futuro) ===
    // Cuando diseñes sprites, ponelos en assets/Items/...
    // Convención: "Items/<tipo>.png" o "Items/item_<tipo>.png"
    // (ver método resolveItemIconPath)
    private static final String PATH_ITEM_UNKNOWN = "Hud/items/item_unknown.png"; // opcional fallback

    private final DisposicionMapa disposicion;
    private final Jugador jugador;

    private Habitacion salaActual;
    private final LayoutMinimapa layout;

    private final OrthographicCamera cam;
    private final Viewport viewport;

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;

    // === Texturas HUD ===
    private Texture texHeartFull, texHeartEmpty, texSlot, texItemUnknown;
    private TextureRegion heartFull, heartEmpty, slot, itemUnknown;

    // === Cache de iconos por tipo de item (carga lazy) ===
    private final EnumMap<ItemTipo, TextureRegion> iconosPorTipo = new EnumMap<>(ItemTipo.class);
    private final EnumMap<ItemTipo, Texture> texturasPorTipo = new EnumMap<>(ItemTipo.class);

    // Layout items
    private int maxSlots = 6;            // ajustable
    private float iconSize = 40f;        // tus sprites son 16x16
    private float iconGap = 6f;
    private float padding = 20f;

    public HudJuego(DisposicionMapa disposicion, Jugador jugador) {
        this.disposicion = disposicion;
        this.jugador = jugador;

        this.salaActual = disposicion.salaInicio();
        this.layout = LayoutMinimapa.construir(disposicion);

        cam = new OrthographicCamera();
        viewport = new FitViewport(HUD_W, HUD_H, cam);
        viewport.apply(true);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();

        cargarTexturasHud();
    }

    private void cargarTexturasHud() {
        texHeartFull = loadTextureSafe(PATH_HEART_FULL);
        texHeartEmpty = loadTextureSafe(PATH_HEART_EMPTY);
        texSlot = loadTextureSafe(PATH_SLOT);
        texItemUnknown = loadTextureSafe(PATH_ITEM_UNKNOWN);

        if (texHeartFull != null) {
            texHeartFull.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            heartFull = new TextureRegion(texHeartFull);
        }
        if (texHeartEmpty != null) {
            texHeartEmpty.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            heartEmpty = new TextureRegion(texHeartEmpty);
        }
        if (texSlot != null) {
            texSlot.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            slot = new TextureRegion(texSlot);
        }
        if (texItemUnknown != null) {
            texItemUnknown.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
            itemUnknown = new TextureRegion(texItemUnknown);
        }
    }

    private static Texture loadTextureSafe(String internalPath) {
        try {
            if (internalPath == null || internalPath.isBlank()) return null;
            if (!Gdx.files.internal(internalPath).exists()) return null;
            return new Texture(Gdx.files.internal(internalPath));
        } catch (GdxRuntimeException ex) {
            return null;
        }
    }

    public void actualizarSalaActual(Habitacion nuevaSala) {
        this.salaActual = nuevaSala;
    }

    @Override
    public void salaCambiada(Habitacion salaAnterior, Habitacion salaNueva) {
        actualizarSalaActual(salaNueva);
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render() {
        viewport.apply();
        cam.update();

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        dibujarVida();
        dibujarItemsSlots();
        batch.end();

        shapes.setProjectionMatrix(cam.combined);
        dibujarMinimapaExplorado();
    }

    // ============================================================
    // VIDA (corazones 16x16)
    // ============================================================

    private void dibujarVida() {
        int vidaActual = jugador.getVida();
        int vidaMax = jugador.getVidaMaxima();

        float x = padding;
        float yTop = HUD_H - padding;
        float y = yTop - iconSize;

        if (heartFull != null && heartEmpty != null) {
            for (int i = 0; i < vidaMax; i++) {
                TextureRegion tr = (i < vidaActual) ? heartFull : heartEmpty;
                batch.draw(tr, x + i * (iconSize + 4f), y, iconSize, iconSize);
            }
        } else {
            // Fallback texto (por si faltan assets)
            StringBuilder sb = new StringBuilder("Vida: ");
            for (int i = 0; i < vidaMax; i++) sb.append(i < vidaActual ? "♥" : "♡");
            font.draw(batch, sb.toString(), x, yTop);
        }
    }

    // ============================================================
    // ITEMS (slots + iconos por ItemTipo, carga lazy desde assets/Items)
    // ============================================================

    private void dibujarItemsSlots() {
        if (slot == null) {
            // Fallback texto (si falta el slot)
            float x = padding;
            float y = HUD_H - padding - iconSize - 20f; // debajo de la vida
            font.draw(batch, "Items:", x, y);
            y -= 18f;

            for (Item item : jugador.getObjetos()) {
                font.draw(batch, "- " + item.getNombre(), x, y);
                y -= 16f;
            }
            return;
        }

        int vidaMax = jugador.getVidaMaxima();

        // Arriba-izquierda
        float left = padding;
        float top = HUD_H - padding;

        // La vida ocupa 1 fila: vidaMax * (iconSize + 4)
        // Dejamos un margen y arrancamos debajo.
        float vidaH = iconSize;
        float offsetY = vidaH + 10f; // separación entre vida e items

        // Título items (debajo de la vida)
        float titleY = top - offsetY;
        font.draw(batch, "Items", left, titleY);

        // Fila de slots debajo del título
        float slotsY = titleY - 10f - iconSize;

        int total = jugador.getObjetos().size();
        int slots = Math.max(maxSlots, total);
        slots = Math.min(slots, maxSlots); // fijo; si querés que crezca, quitá esta línea

        for (int i = 0; i < slots; i++) {
            float x = left + i * (iconSize + iconGap);
            float y = slotsY;

            // Slot base
            batch.draw(slot, x, y, iconSize, iconSize);

            // Icono encima si hay item
            if (i < total) {
                Item item = jugador.getObjetos().get(i);
                TextureRegion icon = getIconoItem(item);

                if (icon != null) {
                    batch.draw(icon, x, y, iconSize, iconSize);
                } else {
                    String n = item.getNombre();
                    String letter = (n != null && !n.isBlank()) ? n.substring(0, 1).toUpperCase() : "?";
                    font.draw(batch, letter, x + 5f, y + 12f);
                }
            }
        }
    }


    private TextureRegion getIconoItem(Item item) {
        if (item == null) return itemUnknown;

        ItemTipo tipo = item.getTipo();
        if (tipo == null) return itemUnknown;

        // Cache hit
        TextureRegion cached = iconosPorTipo.get(tipo);
        if (cached != null) return cached;

        // Intentar carga lazy desde assets/Items
        String path = resolveItemIconPath(tipo);
        Texture tex = loadTextureSafe(path);

        if (tex == null) {
            // No existe todavía: devolvemos unknown (si existe)
            return itemUnknown;
        }

        tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

        TextureRegion region = new TextureRegion(tex);
        iconosPorTipo.put(tipo, region);
        texturasPorTipo.put(tipo, tex);
        return region;
    }

    /**
     * Convención de paths:
     * - Primero prueba: Items/<TIPO_EN_MINUSCULA>.png
     * - Si no existe:  Items/item_<TIPO_EN_MINUSCULA>.png
     *
     * Ej: ItemTipo.VELOCIDAD -> Items/velocidad.png  (o Items/item_velocidad.png)
     */
    private static String resolveItemIconPath(ItemTipo tipo) {
        String base = tipo.name().toLowerCase(); // ej: "velocidad"
        String p1 = "Items/" + base + ".png";
        if (Gdx.files.internal(p1).exists()) return p1;

        String p2 = "Items/item_" + base + ".png";
        if (Gdx.files.internal(p2).exists()) return p2;

        // Si no existe, devolvemos p1 como "default"
        return p1;
    }

    // ============================================================
    // MINIMAPA (igual que antes, pero en HUD virtual)
    // ============================================================

    private void dibujarMinimapaExplorado() {
        Set<Habitacion> descubiertas = disposicion.getDescubiertas();

        final float roomW = 18f;
        final float roomH = 14f;
        final float gap = 8f;

        int minX = layout.minX(), maxX = layout.maxX();
        int minY = layout.minY(), maxY = layout.maxY();

        int widthCells = (maxX - minX + 1);
        int heightCells = (maxY - minY + 1);

        float mapW = widthCells * roomW + (widthCells - 1) * gap;
        float mapH = heightCells * roomH + (heightCells - 1) * gap;

        float baseX = HUD_W - mapW - 20f;
        float baseY = HUD_H - mapH - 40f;

        // Fondo
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(baseX - 6, baseY - 6, mapW + 12, mapH + 12);
        shapes.end();

        // Pasillos
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.85f, 0.85f, 0.85f, 1f);

        for (Habitacion h : disposicion.getSalasActivas()) {
            if (!descubiertas.contains(h)) continue;

            for (var e : disposicion.getConexionesEnPiso(h).entrySet()) {
                Habitacion dest = e.getValue();
                if (dest == null) continue;
                if (!descubiertas.contains(dest)) continue;

                for (PosMini p : layout.pasilloEntre(h, dest)) {
                    float x = baseX + (p.x() - minX) * (roomW + gap);
                    float y = baseY + (p.y() - minY) * (roomH + gap);
                    shapes.rect(x + roomW * 0.25f, y + roomH * 0.25f, roomW * 0.5f, roomH * 0.5f);
                }
            }
        }
        shapes.end();

        // Salas
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Habitacion h : disposicion.getSalasActivas()) {
            if (!descubiertas.contains(h)) continue;

            PosMini p = layout.posiciones().get(h);
            if (p == null) continue;

            float x = baseX + (p.x() - minX) * (roomW + gap);
            float y = baseY + (p.y() - minY) * (roomH + gap);

            setColorSala(shapes, h);
            shapes.rect(x, y, roomW, roomH);
        }
        shapes.end();

        // Borde sala actual
        if (salaActual != null && descubiertas.contains(salaActual)) {
            PosMini p = layout.posiciones().get(salaActual);
            if (p != null) {
                float x = baseX + (p.x() - minX) * (roomW + gap);
                float y = baseY + (p.y() - minY) * (roomH + gap);

                shapes.begin(ShapeRenderer.ShapeType.Line);
                shapes.setColor(1f, 1f, 1f, 1f);
                shapes.rect(x - 1, y - 1, roomW + 2, roomH + 2);
                shapes.end();
            }
        }

        batch.begin();
        font.draw(batch, "Minimapa", baseX, baseY - 12f);
        batch.end();
    }

    private void setColorSala(ShapeRenderer sr, Habitacion h) {
        switch (h.tipo) {
            case INICIO -> sr.setColor(0.95f, 0.95f, 0.95f, 1f);
            case ACERTIJO -> sr.setColor(0.35f, 0.65f, 1f, 1f);
            case COMBATE -> sr.setColor(1f, 0.25f, 0.25f, 1f);
            case BOTIN -> sr.setColor(1f, 1f, 0.35f, 1f);
            case JEFE -> sr.setColor(0.85f, 0.35f, 1f, 1f);
            default -> sr.setColor(0.6f, 0.6f, 0.6f, 1f);
        }
    }

    // ============================================================
    // Config opcional
    // ============================================================

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = Math.max(0, maxSlots);
    }

    public void setIconSize(float iconSize) {
        this.iconSize = iconSize;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }

    // ============================================================
    // Dispose
    // ============================================================

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();

        if (texHeartFull != null) texHeartFull.dispose();
        if (texHeartEmpty != null) texHeartEmpty.dispose();
        if (texSlot != null) texSlot.dispose();
        if (texItemUnknown != null) texItemUnknown.dispose();

        for (Map.Entry<ItemTipo, Texture> e : texturasPorTipo.entrySet()) {
            Texture t = e.getValue();
            if (t != null) t.dispose();
        }
        texturasPorTipo.clear();
        iconosPorTipo.clear();
    }
}
