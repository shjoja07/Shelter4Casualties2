package com.shelter.model;

/**
 * Nivel de prioridad de un damnificado para recibir recursos y atención.
 *
 * PASO 2 (prioridad unificada): antes existían dos sistemas que se contradecían
 * (un número 1/2 en AlbergueService y un texto "CRÍTICA/ALTA/MEDIA/BAJA" en cada subclase).
 * Ahora hay UNA sola fuente de verdad: cada subclase de Damnificado decide su nivel
 * (polimorfismo) y todo lo demás (servicio, reparto, interfaz) solo lo consulta.
 *
 * El "orden" va de 1 (más urgente) a 4 (menos urgente); sirve para ordenar listas.
 */
public enum NivelPrioridad {
    CRITICA(1, "CRÍTICA"),
    ALTA(2, "ALTA"),
    MEDIA(3, "MEDIA"),
    BAJA(4, "BAJA");

    private final int orden;
    private final String etiqueta;

    NivelPrioridad(int orden, String etiqueta) {
        this.orden = orden;
        this.etiqueta = etiqueta;
    }

    /** 1 = más urgente ... 4 = menos urgente. */
    public int getOrden() {
        return orden;
    }

    /** Texto para mostrar (con tilde). */
    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esMasUrgenteQue(NivelPrioridad otro) {
        return this.orden < otro.orden;
    }

    /** Devuelve el más urgente de dos niveles (se usa para combinar reglas). */
    public static NivelPrioridad masUrgente(NivelPrioridad a, NivelPrioridad b) {
        return a.orden <= b.orden ? a : b;
    }
}
