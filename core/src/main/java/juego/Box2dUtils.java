package juego;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;

/**
 * Helpers chicos para Box2D, para evitar repetir código y allocations.
 */
public final class Box2dUtils {

    private Box2dUtils() {}

    public record Aabb(float minX, float minY, float maxX, float maxY) {
        public float width() { return maxX - minX; }
        public float height() { return maxY - minY; }
    }

    /**
     * Devuelve el AABB (axis-aligned) en coordenadas mundo para un fixture.
     * Solo soporta PolygonShape (suficiente para tus puertas/bloqueos).
     */
    public static Aabb aabb(Fixture fixture) {
        if (fixture == null) return new Aabb(0,0,0,0);

        Shape shape = fixture.getShape();
        if (!(shape instanceof PolygonShape poly)) {
            throw new IllegalArgumentException("aabb() solo soporta PolygonShape. Shape=" + shape);
        }

        Body body = fixture.getBody();
        Vector2 v = new Vector2();

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < poly.getVertexCount(); i++) {
            poly.getVertex(i, v);
            Vector2 w = body.getWorldPoint(v);
            if (w.x < minX) minX = w.x;
            if (w.y < minY) minY = w.y;
            if (w.x > maxX) maxX = w.x;
            if (w.y > maxY) maxY = w.y;
        }

        return new Aabb(minX, minY, maxX, maxY);
    }
}
