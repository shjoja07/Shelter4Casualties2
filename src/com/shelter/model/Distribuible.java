package com.shelter.model;

import java.time.LocalDate;

/**
 * Contrato de todo lo que se puede distribuir entre los damnificados de un albergue.
 */
public interface Distribuible {
    String getNombre();

    String getUnidad();

    double getCantidadDisponible();

    LocalDate getFechaVencimiento();

    boolean estaVencido(LocalDate fechaReferencia);

    void descontar(double cantidad);
}
