package entidades.datos;

/**
 * Estilo visual del personaje.
 * Solo se usa para elegir el sprite (ropa/peinado/cuerpo).
 */
public enum Estilo {
    CLASICO,
    DEPORTIVO,
    URBANO,
    ELEGANTE,
    GOTICO,
    CYBERPUNK;

    /**
     * Sufijo para armar el nombre del sprite.
     * Ej: CLASICO -> "clasico"
     */
    public String getSufijoSprite() {
        return name().toLowerCase();
    }
}
