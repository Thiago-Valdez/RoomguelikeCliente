package fisica;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.*;
import com.badlogic.gdx.maps.objects.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.tiles.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ShortArray;

public final class ColisionesDesdeTiled {

    private static final String NOMBRE_CAPA = "colision";

    private ColisionesDesdeTiled() {}

    public static void crearColisiones(TiledMap map, World world) {
        if (map == null || world == null) return;

        int[] creadas = {0};

        recorrerLayers(map.getLayers(), layer -> {
            if (layer.getName() == null) return;
            if (!layer.getName().equalsIgnoreCase(NOMBRE_CAPA)) return;

            creadas[0] += crearDesdeObjectLayer(layer, world);
            if (layer instanceof TiledMapTileLayer tileLayer) {
                creadas[0] += crearDesdeTileCollisions(tileLayer, world);
            }
        });

        Gdx.app.log("ColisionesDesdeTiled", "Colisiones creadas: " + creadas[0]);
    }

    private static void recorrerLayers(MapLayers layers, java.util.function.Consumer<MapLayer> fn) {
        for (MapLayer layer : layers) {
            fn.accept(layer);
            if (layer instanceof MapGroupLayer group) {
                recorrerLayers(group.getLayers(), fn);
            }
        }
    }


    private static int crearDesdeObjectLayer(MapLayer layer, World world) {
        int count = 0;

        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject r) {
                crearRect(world, r.getRectangle(), obj.getName());
                count++;
            } else if (obj instanceof PolygonMapObject p) {
                crearPolygon(world, p.getPolygon(), obj.getName());
                count++;
            } else if (obj instanceof PolylineMapObject pl) {
                // Polylines: si las usás como paredes, las convertimos en segmentos (chain)
                crearPolyline(world, pl.getPolyline(), obj.getName());
                count++;
            } else if (obj instanceof EllipseMapObject e) {
                // Elipse -> aproximación simple como círculo si es casi círculo
                crearEllipse(world, e.getEllipse(), obj.getName());
                count++;
            } else if (obj instanceof TiledMapTileMapObject tmo) {
                // Objetos de tipo Tile en un object layer
                count += crearDesdeTileObject(world, tmo);
            } else {
                // Si acá ves que te faltan, loguealo:
                // Gdx.app.log("ColisionesDesdeTiled", "Objeto ignorado: " + obj.getClass());
            }
        }

        return count;
    }

    /**
     * Colisiones definidas en los tiles (Tile Collision Editor).
     * Recorre cada celda y crea shapes usando tile.getObjects().
     */
    private static int crearDesdeTileCollisions(TiledMapTileLayer tileLayer, World world) {
        int count = 0;

        float tileW = tileLayer.getTileWidth();
        float tileH = tileLayer.getTileHeight();

        for (int y = 0; y < tileLayer.getHeight(); y++) {
            for (int x = 0; x < tileLayer.getWidth(); x++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell == null) continue;

                TiledMapTile tile = cell.getTile();
                if (tile == null) continue;

                MapObjects collisionObjs = tile.getObjects();
                if (collisionObjs == null || collisionObjs.getCount() == 0) continue;

                float cellX = x * tileW;
                float cellY = y * tileH;

                for (MapObject obj : collisionObjs) {
                    if (obj instanceof RectangleMapObject r) {
                        Rectangle rr = new Rectangle(r.getRectangle());
                        rr.x += cellX;
                        rr.y += cellY;
                        crearRect(world, rr, "tileRect");
                        count++;
                    } else if (obj instanceof PolygonMapObject p) {
                        Polygon poly = new Polygon(p.getPolygon().getVertices());
                        poly.setPosition(p.getPolygon().getX() + cellX, p.getPolygon().getY() + cellY);
                        crearPolygon(world, poly, "tilePoly");
                        count++;
                    } else if (obj instanceof PolylineMapObject pl) {
                        Polyline line = new Polyline(pl.getPolyline().getVertices());
                        line.setPosition(pl.getPolyline().getX() + cellX, pl.getPolyline().getY() + cellY);
                        crearPolyline(world, line, "tileLine");
                        count++;
                    } else if (obj instanceof EllipseMapObject e) {
                        Ellipse el = new Ellipse(e.getEllipse());
                        el.x += cellX;
                        el.y += cellY;
                        crearEllipse(world, el, "tileEllipse");
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private static int crearDesdeTileObject(World world, TiledMapTileMapObject tmo) {
        // En general esto lo podés tratar como rect o como el polígono del tile si existiera.
        // Si lo usás, decime cómo los definiste en Tiled y lo afinamos.
        // Por ahora lo ignoramos.
        return 0;
    }

    // ------------------ helpers Box2D ------------------

    private static void crearRect(World world, Rectangle rect, String name) {
        // rect.x, rect.y (abajo-izquierda), width/height en pixeles
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;

        float cx = rect.x + rect.width / 2f;
        float cy = rect.y + rect.height / 2f;
        bd.position.set(cx, cy);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(rect.width / 2f, rect.height / 2f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0f;
        fd.restitution = 0f;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(name != null ? name : "colision");

        shape.dispose();
    }

    private static void crearPolygon(World world, Polygon poly, String name) {
        float[] verts = poly.getTransformedVertices();
        int count = verts.length / 2;
        if (count < 3) return;

        // Triangulamos (sirve para concavos y cualquier forma)
        EarClippingTriangulator tri = new EarClippingTriangulator();
        ShortArray indices = tri.computeTriangles(verts); // índices de triángulos

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(0, 0);
        Body body = world.createBody(bd);

        FixtureDef fd = new FixtureDef();
        fd.friction = 0f;
        fd.restitution = 0f;

        // Creamos 1 fixture por triángulo
        for (int i = 0; i < indices.size; i += 3) {
            int i1 = indices.get(i) * 2;
            int i2 = indices.get(i + 1) * 2;
            int i3 = indices.get(i + 2) * 2;

            Vector2 v1 = new Vector2(verts[i1], verts[i1 + 1]);
            Vector2 v2 = new Vector2(verts[i2], verts[i2 + 1]);
            Vector2 v3 = new Vector2(verts[i3], verts[i3 + 1]);

            PolygonShape shape = new PolygonShape();
            shape.set(new Vector2[]{v1, v2, v3});

            fd.shape = shape;
            Fixture fx = body.createFixture(fd);
            fx.setUserData(name != null ? name : "colision");

            shape.dispose();
        }
    }

    private static void crearPolyline(World world, Polyline line, String name) {
        float[] verts = line.getTransformedVertices();
        int count = verts.length / 2;
        if (count < 2) return;

        Vector2[] v = new Vector2[count];
        for (int i = 0; i < count; i++) {
            v[i] = new Vector2(verts[i * 2], verts[i * 2 + 1]);
        }

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(0, 0);

        Body body = world.createBody(bd);

        ChainShape shape = new ChainShape();
        shape.createChain(v);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(name != null ? name : "colision");

        shape.dispose();
    }

    private static void crearEllipse(World world, Ellipse e, String name) {
        // Si es círculo aprox, lo creamos como CircleShape.
        float rx = e.width / 2f;
        float ry = e.height / 2f;

        if (Math.abs(rx - ry) > 0.5f) {
            // Elipses reales: podríamos aproximarlas, pero normalmente en colisión no hace falta.
            Gdx.app.log("ColisionesDesdeTiled", "Ellipse no circular ignorada (aprox pendiente).");
            return;
        }

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;

        float cx = e.x + rx;
        float cy = e.y + ry;
        bd.position.set(cx, cy);

        Body body = world.createBody(bd);

        CircleShape shape = new CircleShape();
        shape.setRadius(rx);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(name != null ? name : "colision");

        shape.dispose();
    }
}
