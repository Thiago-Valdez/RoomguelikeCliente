package mapa.puertas;

import mapa.model.Direccion;
import mapa.model.Habitacion;

/**
 * Información necesaria sobre una puerta para poder cambiar de sala
 * cuando el jugador la toca.
 *
 * origen   -> habitación desde la que se sale
 * destino  -> habitación a la que se entra
 * direccion -> dirección de la puerta vista desde la sala origen
 *             (NORTE, SUR, ESTE, OESTE)
 */
public record DatosPuerta(
    Habitacion origen,
    Habitacion destino,
    Direccion direccion,
    PuertaVisual visual
) {
}
