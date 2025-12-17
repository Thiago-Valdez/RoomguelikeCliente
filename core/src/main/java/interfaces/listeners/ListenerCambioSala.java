// paquete: eventos
package interfaces.listeners;

import mapa.model.Habitacion;

public interface ListenerCambioSala {
    void salaCambiada(Habitacion salaAnterior, Habitacion salaNueva);
}
