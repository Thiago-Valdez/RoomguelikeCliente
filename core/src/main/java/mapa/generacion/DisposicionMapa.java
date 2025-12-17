package mapa.generacion;

import mapa.model.Direccion;
import mapa.model.Habitacion;

import java.util.*;

public class DisposicionMapa {

    /** El camino de habitaciones que el generador decidió para este nivel (EN ORDEN) */
    private final List<Habitacion> camino = new ArrayList<>();

    /** Habitaciones visitadas (útil para el minimapa, HUD) */
    private final Set<Habitacion> descubiertas = new HashSet<>();

    /** Conexiones REALES del piso (solo puertas válidas) */
    private final Map<Habitacion, EnumMap<Direccion, Habitacion>> conexionesPiso = new HashMap<>();

    /** Agrega una sala al camino (la run actual) */
    public void agregarAlCamino(Habitacion h) {
        if (h == null) return;
        if (!camino.contains(h)) camino.add(h);
    }

    /** Devuelve el camino completo (salas activas de esta run) */
    public List<Habitacion> getCamino() {
        return camino;
    }

    /** Alias semántico: salas activas = camino */
    public List<Habitacion> getSalasActivas() {
        return camino;
    }

    public boolean esSalaActiva(Habitacion h) {
        return camino.contains(h);
    }

    /** Marca una sala como descubierta */
    public void descubrir(Habitacion h) {
        if (h != null) descubiertas.add(h);
    }

    /** Devuelve true si la sala ya se visitó */
    public boolean estaDescubierta(Habitacion h) {
        return descubiertas.contains(h);
    }

    /** Devuelve un set de todas las salas descubiertas */
    public Set<Habitacion> getDescubiertas() {
        return descubiertas;
    }

    /** Sala de inicio: la primera del camino, si existe; si no, INICIO_1 */
    public Habitacion salaInicio() {
        if (!camino.isEmpty()) return camino.get(0);
        return Habitacion.INICIO_1;
    }

    // =========================
    // Conexiones del piso
    // =========================

    /** Vincula una puerta válida del piso */
    public void vincularEnPiso(Habitacion origen, Direccion dir, Habitacion destino) {
        if (origen == null || dir == null || destino == null) return;
        conexionesPiso
            .computeIfAbsent(origen, k -> new EnumMap<>(Direccion.class))
            .put(dir, destino);
    }

    /** Destino por una dirección, SOLO si la puerta es válida en este piso */
    public Habitacion getDestinoEnPiso(Habitacion origen, Direccion dir) {
        EnumMap<Direccion, Habitacion> m = conexionesPiso.get(origen);
        if (m == null) return null;
        return m.get(dir);
    }

    /** Conexiones válidas de la sala en este piso */
    public EnumMap<Direccion, Habitacion> getConexionesEnPiso(Habitacion origen) {
        EnumMap<Direccion, Habitacion> m = conexionesPiso.get(origen);
        if (m == null) return new EnumMap<>(Direccion.class);
        return m;
    }

    /** Útil para debug */
    public void imprimirConexionesPiso() {
        System.out.println("== CONEXIONES DEL PISO ==");
        for (Habitacion h : camino) {
            EnumMap<Direccion, Habitacion> m = conexionesPiso.get(h);
            StringBuilder sb = new StringBuilder();
            sb.append(" ").append(h.nombreVisible).append(" -> ");
            if (m != null) {
                for (var e : m.entrySet()) {
                    sb.append("[").append(e.getKey()).append("→").append(e.getValue().nombreVisible).append("] ");
                }
            }
            System.out.println(sb);
        }
    }
}
