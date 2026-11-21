package com.shelter.model;

/**
 * Alerta visible sobre un albergue (RF-05 y RF-06).
 *
 * En lugar de devolver strings sueltos, el servicio devuelve objetos con gravedad y código,
 * así la interfaz Swing puede pintar cada alerta del color correcto, filtrarlas y ofrecer la
 * acción que la resuelve (por ejemplo, registrar el recurso que falta).
 *
 * @param gravedad AMARILLO (advertencia) o ROJO (incumple un mínimo / situación crítica)
 * @param codigo   identificador corto: OCUPACION, AGUA, ESPACIO, CAPACIDAD, RECURSO_FALTANTE, RECURSO_VENCIDO
 * @param mensaje  texto legible para el encargado
 * @param recurso  tipo de recurso involucrado, o null si la alerta no trata de un recurso
 */
public record Alerta(NivelSemaforo gravedad, String codigo, String mensaje, TipoRecurso recurso) {

    /** Alerta que no se refiere a un recurso concreto (ocupación, espacio, capacidad). */
    public Alerta(NivelSemaforo gravedad, String codigo, String mensaje) {
        this(gravedad, codigo, mensaje, null);
    }

    @Override
    public String toString() {
        return "[" + gravedad.getEtiqueta() + "] " + mensaje;
    }
}
