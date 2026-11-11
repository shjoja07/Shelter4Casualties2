package com.shelter.model;

/**
 * Estado semáforo de un albergue (RF-05).
 *
 * Usa enum, así el compilador impide valores inválidos y la interfaz Swing puede mapear cada nivel a un color.
 * El estado es calculado por AlbergueService a partir de la ocupación, los mínimos Sphere y los recursos críticos.
 */
public enum NivelSemaforo {
    VERDE("VERDE"),
    AMARILLO("AMARILLO"),
    ROJO("ROJO");

    private final String etiqueta;

    NivelSemaforo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Devuelve el más grave de dos niveles (ROJO > AMARILLO > VERDE). */
    public static NivelSemaforo peor(NivelSemaforo a, NivelSemaforo b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
