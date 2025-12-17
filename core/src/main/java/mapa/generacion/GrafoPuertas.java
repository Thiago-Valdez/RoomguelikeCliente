package mapa.generacion;

import mapa.model.Direccion;
import mapa.model.Habitacion;

import java.util.*;

/**
 * Grafo de “compatibilidad de puertas” entre habitaciones.
 *
 * Dos habitaciones A y B están conectadas si:
 *  - A tiene una puerta en una Direccion d
 *  - B tiene una puerta en la direccion opuesta d.opuesta()
 *
 * En esta versión NO obligamos a que sean vecinas en la grilla 5x5.
 * Eso permite, por ejemplo, conectar el NORTE de Inicio con
 * cualquier sala que tenga puerta SUR.
 */
public class GrafoPuertas {

    private final List<Habitacion> habitaciones;
    private final Random rng;

    /**
     * Para cada habitación origen, guarda a qué habitación lleva cada
     * dirección concreta (NORTE, SUR, ESTE, OESTE).
     *
     * conexiones.get(origen).get(dir)  -> destino (o null si no hay)
     */
    private final Map<Habitacion, EnumMap<Direccion, Habitacion>> conexiones =
        new EnumMap<>(Habitacion.class);

    public GrafoPuertas(List<Habitacion> habitaciones, Random rng) {
        this.habitaciones = new ArrayList<>(habitaciones);
        this.rng = rng;
        construirConexiones();
    }

    /**
     * Construye las conexiones lógicas:
     * para cada puerta (origen, dir), elige una sala destino
     * que tenga la puerta opuesta.
     */
    private void construirConexiones() {

        for (Habitacion h : habitaciones)
            conexiones.put(h, new EnumMap<>(Direccion.class));

        record Door(Habitacion h, Direccion d) {}

        List<Door> puertasLibres = new ArrayList<>();

        // solo las puertas declaradas en el enum
        for (Habitacion h : habitaciones)
            for (Direccion d : h.puertas.keySet())
                puertasLibres.add(new Door(h, d));

        Collections.shuffle(puertasLibres, rng);

        for (Door door : puertasLibres) {

            Habitacion origen = door.h;
            Direccion dir = door.d;
            Direccion opuesta = dir.opuesta();

            // ya emparejada
            if (conexiones.get(origen).containsKey(dir))
                continue;

            // buscar candidatos que:
            // - no sean el origen
            // - tengan la puerta opuesta
            // - no tengan esa puerta ya emparejada
            // - NO estén ya conectados al origen (evita duplicados)
            List<Habitacion> candidatos = new ArrayList<>();

            for (Habitacion dest : habitaciones) {
                if (dest == origen) continue;
                if (!dest.puertas.containsKey(opuesta)) continue;
                if (conexiones.get(dest).containsKey(opuesta)) continue;

                // evitar que origen y destino tengan ya otra conexión
                // NO queremos:
                // Inicio.NORTE -> A
                // Inicio.ESTE  -> A
                if (conexiones.get(origen).containsValue(dest)) continue;

                candidatos.add(dest);
            }

            if (candidatos.isEmpty())
                continue;

            Habitacion destino = candidatos.get(rng.nextInt(candidatos.size()));

            conexiones.get(origen).put(dir, destino);
            conexiones.get(destino).put(opuesta, origen);
        }

        // log
        System.out.println("== GRAFO DE PUERTAS ==");
        for (var e : conexiones.entrySet()) {
            System.out.print(" " + e.getKey().nombreVisible + " ->");
            for (var d : e.getValue().entrySet())
                System.out.print(" [" + d.getKey() + "→" + d.getValue().nombreVisible + "]");
            System.out.println();
        }
    }





    /**
     * Devuelve la habitación destino a la que lleva la puerta `dir`
     * desde la habitación `origen`, o null si no hay conexión.
     */
    public Habitacion destinoDe(Habitacion origen, Direccion dir) {
        EnumMap<Direccion, Habitacion> mapaDirs = conexiones.get(origen);
        if (mapaDirs == null) return null;
        return mapaDirs.get(dir);
    }

    /**
     * Vecinas lógicas de una habitación (unión de todos los destinos
     * de sus direcciones). Útil si necesitás “salas adyacentes” en
     * el sentido lógico del grafo.
     */
    public List<Habitacion> vecinas(Habitacion h) {
        EnumMap<Direccion, Habitacion> mapaDirs = conexiones.get(h);
        if (mapaDirs == null || mapaDirs.isEmpty()) {
            return Collections.emptyList();
        }

        // Evitar duplicados
        LinkedHashSet<Habitacion> set = new LinkedHashSet<>(mapaDirs.values());
        return new ArrayList<>(set);
    }

    /**
     * Devuelve una vecina aleatoria que cumpla un filtro, o null si no hay.
     */
    public Habitacion vecinaAleatoria(Habitacion h,
                                      java.util.function.Predicate<Habitacion> filtro) {
        List<Habitacion> candidatas = new ArrayList<>();
        for (Habitacion x : vecinas(h)) {
            if (filtro == null || filtro.test(x)) {
                candidatas.add(x);
            }
        }
        if (candidatas.isEmpty()) return null;
        return candidatas.get(rng.nextInt(candidatas.size()));
    }
}
