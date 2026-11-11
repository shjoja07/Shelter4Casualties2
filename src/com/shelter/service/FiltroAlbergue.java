package com.shelter.service;

import com.shelter.model.Albergue;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.TipoRecurso;

/**
 * Criterios para filtrar la tabla de albergues (RF-07). Cada criterio es opcional:
 * un valor null significa "no filtrar por este criterio". Los criterios se combinan con AND.
 *
 * @param cuposMinimos     cupos libres mínimos (ej. 1 = "solo albergues con cupo")
 * @param distrito         texto contenido en el distrito (sin distinguir mayúsculas)
 * @param nivelAlerta      nivel de semáforo exacto (VERDE, AMARILLO o ROJO)
 * @param recursoFaltante  tipo de recurso del que el albergue NO tiene stock vigente
 */
public record FiltroAlbergue(Integer cuposMinimos, String distrito,
                             NivelSemaforo nivelAlerta, TipoRecurso recursoFaltante) {

    /** Filtro vacío: deja pasar todos los albergues. */
    public static FiltroAlbergue sinFiltros() {
        return new FiltroAlbergue(null, null, null, null);
    }

    public boolean cumple(Albergue a) {
        if (cuposMinimos != null && a.getCuposDisponibles() < cuposMinimos) {
            return false;
        }
        if (distrito != null && !distrito.isBlank()
                && !a.getDistrito().toLowerCase().contains(distrito.trim().toLowerCase())) {
            return false;
        }
        if (nivelAlerta != null && a.getEstadoSemaforo() != nivelAlerta) {
            return false;
        }
        return recursoFaltante == null || !a.tieneRecursoDisponible(recursoFaltante);
    }
}
