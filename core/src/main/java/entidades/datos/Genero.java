package entidades.datos;

/**
 * Género del personaje. Es puramente estético.
 * Podés usarlo para elegir sprites, voz, etc.
 */
public enum Genero {
    MASCULINO("masculino", "m"),
    FEMENINO("femenino", "f");

    private final String nombreVisible;
    private final String sufijoSprite;

    Genero(String nombreVisible, String sufijoSprite) {
        this.nombreVisible = nombreVisible;
        this.sufijoSprite = sufijoSprite;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Podemos usar esto para armar nombres de archivos de sprite.
     * Ej: "player_" + genero.getSufijoSprite() + "_" + estilo.getSufijoSprite()
     */
    public String getSufijoSprite() {
        return sufijoSprite;
    }
}
