package mapa.minimapa;

import mapa.model.Direccion;
import mapa.model.Habitacion;
import mapa.generacion.DisposicionMapa;

import java.util.*;

/**
 * Construye un layout del minimapa a partir de las CONEXIONES DEL PISO (conexionesPiso).
 *
 * - Parte de INICIO en (0,0)
 * - Para cada conexión (origen, dir -> destino), intenta colocar destino en origen + dir
 * - Si la celda está ocupada, se corre más en esa dirección (mantiene dirección)
 *
 * Esto hace que el minimapa represente EXACTAMENTE lo establecido por el generador (piso),
 * sin perder tu GrafoPuertas random (que sigue existiendo, solo no se usa para el HUD).
 */
public final class LayoutMinimapa {

    /** Posición asignada para cada sala activa. */
    private final Map<Habitacion, PosMini> posPorSala = new HashMap<>();

    /** Ocupación inversa: qué sala ocupa una posición. */
    private final Map<PosMini, Habitacion> salaPorPos = new HashMap<>();

    /** Pasillos: celdas intermedias entre dos salas. */
    private final Map<EdgeKey, List<PosMini>> pasillos = new HashMap<>();

    /** Bounds del layout (para centrar/normalizar en HUD). */
    private int minX, maxX, minY, maxY;

    private LayoutMinimapa() {}

    public static LayoutMinimapa construir(DisposicionMapa disposicion) {
        LayoutMinimapa out = new LayoutMinimapa();

        Set<Habitacion> activas = new HashSet<>(disposicion.getSalasActivas());
        Habitacion inicio = disposicion.salaInicio();

        ArrayDeque<Habitacion> q = new ArrayDeque<>();
        out.colocar(inicio, new PosMini(0, 0));
        q.add(inicio);

        while (!q.isEmpty()) {
            Habitacion h = q.poll();
            PosMini ph = out.posPorSala.get(h);
            if (ph == null) continue;

            // 🔥 Fuente de verdad: conexiones del piso
            EnumMap<Direccion, Habitacion> conex = disposicion.getConexionesEnPiso(h);

            for (var e : conex.entrySet()) {
                Direccion dir = e.getKey();
                Habitacion dest = e.getValue();

                if (dest == null) continue;
                if (!activas.contains(dest)) continue;

                // Posición deseada (adyacente según dir)
                PosMini pd = new PosMini(ph.x() + dx(dir), ph.y() + dy(dir));

                if (!out.posPorSala.containsKey(dest)) {
                    PosMini libre = out.buscarLibreEnDireccion(pd, dir);
                    out.colocar(dest, libre);

                    // Pasillo entre ph y libre (por si se tuvo que correr)
                    out.pasillos.put(new EdgeKey(h, dest), out.celdasIntermedias(ph, libre));

                    q.add(dest);
                } else {
                    // Ya colocado: asegurar pasillo si falta
                    PosMini ya = out.posPorSala.get(dest);
                    EdgeKey k = new EdgeKey(h, dest);
                    out.pasillos.putIfAbsent(k, out.celdasIntermedias(ph, ya));
                }
            }
        }

        out.calcularBounds();
        return out;
    }

    public Map<Habitacion, PosMini> posiciones() { return posPorSala; }

    /** Celdas del pasillo entre a y b (sin incluir endpoints). */
    public List<PosMini> pasilloEntre(Habitacion a, Habitacion b) {
        return pasillos.getOrDefault(new EdgeKey(a, b), List.of());
    }

    public int minX() { return minX; }
    public int maxX() { return maxX; }
    public int minY() { return minY; }
    public int maxY() { return maxY; }

    // ----------------- internos -----------------

    private void colocar(Habitacion h, PosMini p) {
        posPorSala.put(h, p);
        salaPorPos.put(p, h);
    }

    private PosMini buscarLibreEnDireccion(PosMini start, Direccion dir) {
        PosMini p = start;
        while (salaPorPos.containsKey(p)) {
            p = new PosMini(p.x() + dx(dir), p.y() + dy(dir));
        }
        return p;
    }

    private List<PosMini> celdasIntermedias(PosMini a, PosMini b) {
        // Nos movemos en “L”: primero X, luego Y.
        // En la práctica casi siempre será recto, pero esto evita huecos raros.
        List<PosMini> out = new ArrayList<>();

        int x = a.x();
        int y = a.y();

        while (x != b.x()) {
            x += Integer.compare(b.x(), x);
            if (x == b.x() && y == b.y()) break;
            out.add(new PosMini(x, y));
        }

        while (y != b.y()) {
            y += Integer.compare(b.y(), y);
            if (x == b.x() && y == b.y()) break;
            out.add(new PosMini(x, y));
        }

        // Seguridad: no incluir endpoint
        if (!out.isEmpty() && out.get(out.size() - 1).equals(b)) out.remove(out.size() - 1);
        return out;
    }

    private void calcularBounds() {
        minX = Integer.MAX_VALUE; maxX = Integer.MIN_VALUE;
        minY = Integer.MAX_VALUE; maxY = Integer.MIN_VALUE;

        for (PosMini p : posPorSala.values()) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }

        if (posPorSala.isEmpty()) {
            minX = maxX = minY = maxY = 0;
        }
    }

    private static int dx(Direccion d) {
        return switch (d) {
            case ESTE -> 1;
            case OESTE -> -1;
            default -> 0;
        };
    }

    private static int dy(Direccion d) {
        return switch (d) {
            case NORTE -> 1;
            case SUR -> -1;
            default -> 0;
        };
    }

    /** Clave no dirigida (A-B == B-A) para no duplicar pasillos. */
    private static final class EdgeKey {
        private final int a;
        private final int b;

        EdgeKey(Habitacion h1, Habitacion h2) {
            int i1 = h1.id;
            int i2 = h2.id;
            this.a = Math.min(i1, i2);
            this.b = Math.max(i1, i2);
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EdgeKey k)) return false;
            return a == k.a && b == k.b;
        }

        @Override public int hashCode() {
            return 31 * a + b;
        }
    }
}
