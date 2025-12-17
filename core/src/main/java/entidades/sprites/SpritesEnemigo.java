package entidades.sprites;

import entidades.enemigos.Enemigo;

public class SpritesEnemigo extends SpritesEntidad {

    public SpritesEnemigo(Enemigo enemigo, int frameW, int frameH) {
        super(enemigo, frameW, frameH);
        cargar();
        construirAnimaciones();
    }

    @Override
    protected String pathQuieto() {
        Enemigo e = (Enemigo) entidad;
        return "Enemigos/" + e.getNombre() + "_quieto.png";
    }

    @Override
    protected String pathMovimiento() {
        Enemigo e = (Enemigo) entidad;
        return "Enemigos/" + e.getNombre() + "_movimiento.png";
    }

    @Override
    protected String pathMuerte() {
        Enemigo e = (Enemigo) entidad;
        return "Enemigos/" + e.getNombre() + "_muerte.png";
    }
}
