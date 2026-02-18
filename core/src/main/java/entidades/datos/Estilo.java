package entidades.datos;

/**
 * Estilo visual del personaje.
 * Solo se usa para elegir el sprite (ropa/peinado/cuerpo).
 *
 * ⚠️ Importante:
 * - En el proyecto solo usamos 3 estilos (según tu definición):
 *   CLASICO, FUTURISTA, OSCURO
 * - El orden (ordinal) se usa para elegir sprites numerados:
 *   jugador_masc1_*, jugador_masc2_*, jugador_masc3_*
 */
public enum Estilo {
    CLASICO,
    FUTURISTA,
    OSCURO;

    /**
     * Sufijo para armar el nombre del sprite.
     * Ej: CLASICO -> "clasico"
     */
    public String getSufijoSprite() {
        return name().toLowerCase();
    }
}
