package com.shelter.model;

/**
 * Alerta visible sobre un albergue (RF-05 y RF-06).
 *
 * En lugar de devolver strings sueltos, el servicio devuelve objetos con gravedad y código,
 * así la interfaz Swing puede pintar cada alerta del color correcto y filtrarlas.
 *
 * @param gravedad AMARILLO (advertencia) o ROJO (incumple un mínimo / situación crítica)
 * @param codigo   identificador corto: OCUPACION, AGUA, ESPACIO, CAPACIDAD, RECURSO_FALTANTE, RECURSO_VENCIDO
 * @param mensaje  texto legible para el encargado
 */
public record Alerta(NivelSemaforo gravedad, String codigo, String mensaje) {

    @Override
    public String toString() {
        return "[" + gravedad.getEtiqueta() + "] " + mensaje;
    }
}
