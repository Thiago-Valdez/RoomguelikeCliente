package mapa.generacion;

import java.util.*;

import mapa.generacion.DisposicionMapa;
import mapa.model.Direccion;
import mapa.model.Habitacion;

import com.badlogic.gdx.Gdx;
import mapa.model.TipoSala;

/**
 * Genera un camino de habitaciones usando el GrafoPuertas y restricciones por nivel.
 * IMPORTANTE: además construye conexionesPiso bidireccionales para gameplay/HUD/puertas físicas.
 */
public class GeneradorMapa {

    public static class Configuracion {
        public int nivel = 1;
        public long semilla = System.currentTimeMillis();
    }

    private static class NivelCfg {
        final int minSalas;
        final int maxSalas;
        final int minAcertijos;
        final int minCombates;
        final boolean requiereBotin;
        final boolean terminaEnJefe;

        NivelCfg(int minSalas, int maxSalas,
                 int minAcertijos, int minCombates,
                 boolean requiereBotin,
                 boolean terminaEnJefe) {
            this.minSalas = minSalas;
            this.maxSalas = maxSalas;
            this.minAcertijos = minAcertijos;
            this.minCombates = minCombates;
            this.requiereBotin = requiereBotin;
            this.terminaEnJefe = terminaEnJefe;
        }
    }

    private final Configuracion cfg;
    private final GrafoPuertas grafo;
    private final Random rng;

    public List<Habitacion> salasDelPiso = new ArrayList<>();

    public GeneradorMapa(Configuracion cfg, GrafoPuertas grafo) {
        this.cfg = cfg;
        this.grafo = grafo;
        this.rng = new Random(cfg.semilla);
    }

    public DisposicionMapa generar() {
        NivelCfg nivelCfg = elegirCfgNivel(cfg.nivel);

        Habitacion inicio = Habitacion.INICIO_1;

        List<List<Habitacion>> candidatos = new ArrayList<>();
        List<Habitacion> path = new ArrayList<>();
        Set<Habitacion> visitados = new HashSet<>();

        path.add(inicio);
        visitados.add(inicio);

        dfsTodos(inicio, nivelCfg, path, visitados, candidatos);

        List<Habitacion> mejor;

        if (candidatos.isEmpty()) {
            Habitacion jefeFallback = elegirJefeAleatorio();
            mejor = new ArrayList<>();
            mejor.add(inicio);
            mejor.add(jefeFallback);

            Gdx.app.log("GeneradorMapa",
                "No se pudo generar un camino completo, usando fallback simple.");
            imprimirCamino("CAMINO FALLBACK", mejor);
        } else {
            int maxLen = 0;
            for (List<Habitacion> c : candidatos) maxLen = Math.max(maxLen, c.size());

            List<List<Habitacion>> masLargos = new ArrayList<>();
            for (List<Habitacion> c : candidatos) if (c.size() == maxLen) masLargos.add(c);

            mejor = masLargos.get(rng.nextInt(masLargos.size()));
            imprimirCamino("CAMINO GENERADO", mejor);

            salasDelPiso.clear();
            salasDelPiso.addAll(mejor);
        }

        // Construimos la DisposicionMapa REAL
        DisposicionMapa disposicion = new DisposicionMapa();
        for (Habitacion h : mejor) disposicion.agregarAlCamino(h);

        // 🔥 IMPORTANTE: construir conexionesPiso BIDIRECCIONALES según el camino elegido
        construirConexionesDelPiso(disposicion, mejor);

        // Debug opcional
        disposicion.imprimirConexionesPiso();

        return disposicion;
    }

    private void construirConexionesDelPiso(DisposicionMapa disposicion, List<Habitacion> mejor) {
        if (mejor.size() < 2) return;

        for (int i = 0; i < mejor.size() - 1; i++) {
            Habitacion a = mejor.get(i);
            Habitacion b = mejor.get(i + 1);

            Direccion dir = direccionEntre(a, b);
            if (dir == null) {
                // Si pasa esto, tu grafo te dio vecinas() pero no podemos recuperar la dirección.
                // Es 100% un bug de GrafoPuertas (vecinas sin mantener dirección).
                Gdx.app.log("GeneradorMapa",
                    "ADVERTENCIA: No se encontró dirección entre " + a.nombreVisible + " y " + b.nombreVisible);
                continue;
            }

            // Guardar A->B y B->A siempre
            disposicion.vincularEnPiso(a, dir, b);
            disposicion.vincularEnPiso(b, dir.opuesta(), a);
        }
    }

    /**
     * Encuentra qué dirección en el grafo lleva de 'a' hacia 'b'.
     * Esto permite mantener el grafo random sin cambiarlo.
     */
    private Direccion direccionEntre(Habitacion a, Habitacion b) {
        // Intentamos con los destinos por dirección si tu grafo tiene getDestino().
        for (Direccion d : Direccion.values()) {
            Habitacion dest = grafo.destinoDe(a, d); // <- si tu método se llama distinto, ajustalo acá
            if (dest == b) return d;
        }
        return null;
    }

    private void dfsTodos(Habitacion actual,
                          NivelCfg nivelCfg,
                          List<Habitacion> path,
                          Set<Habitacion> visitados,
                          List<List<Habitacion>> candidatos) {

        int n = path.size();
        if (n > nivelCfg.maxSalas) return;

        if (actual.tipo == TipoSala.JEFE) {
            if (nivelCfg.terminaEnJefe && cumpleRestricciones(path, nivelCfg)) {
                candidatos.add(new ArrayList<>(path));
            }
            return;
        }

        List<Habitacion> vecinos = new ArrayList<>(grafo.vecinas(actual));
        Collections.shuffle(vecinos, rng);

        for (Habitacion sig : vecinos) {
            if (visitados.contains(sig)) continue;

            path.add(sig);
            visitados.add(sig);

            dfsTodos(sig, nivelCfg, path, visitados, candidatos);

            path.remove(path.size() - 1);
            visitados.remove(sig);
        }
    }

    private boolean cumpleRestricciones(List<Habitacion> path, NivelCfg nivelCfg) {
        int n = path.size();
        if (n < nivelCfg.minSalas || n > nivelCfg.maxSalas) return false;

        Habitacion ultima = path.get(n - 1);
        if (nivelCfg.terminaEnJefe && ultima.tipo != TipoSala.JEFE) return false;

        int cAcertijo = 0;
        int cCombate = 0;
        int cBotin = 0;

        for (Habitacion h : path) {
            switch (h.tipo) {
                case ACERTIJO -> cAcertijo++;
                case COMBATE -> cCombate++;
                case BOTIN -> cBotin++;
                default -> {}
            }
        }

        if (cAcertijo < nivelCfg.minAcertijos) return false;
        if (cCombate < nivelCfg.minCombates) return false;
        if (nivelCfg.requiereBotin && cBotin == 0) return false;

        return true;
    }

    private NivelCfg elegirCfgNivel(int nivel) {
        return switch (nivel) {
            case 1 -> new NivelCfg(5, 10, 2, 2, true, true);
            case 2 -> new NivelCfg(7, 11, 3, 3, true, true);
            case 3 -> new NivelCfg(9, 13, 4, 4, true, true);
            default -> new NivelCfg(5, 10, 2, 2, true, true);
        };
    }

    private Habitacion elegirJefeAleatorio() {
        List<Habitacion> jefes = new ArrayList<>();
        for (Habitacion h : Habitacion.values()) {
            if (h.tipo == TipoSala.JEFE) jefes.add(h);
        }
        if (jefes.isEmpty()) throw new IllegalStateException("No hay habitaciones de JEFE definidas.");
        return jefes.get(rng.nextInt(jefes.size()));
    }

    private void imprimirCamino(String titulo, List<Habitacion> camino) {
        System.out.println("== " + titulo + " ==");
        for (Habitacion h : camino) {
            System.out.println(" - " + h.nombreVisible +
                " (" + h.gridX + "," + h.gridY + ")");
        }
    }
}
