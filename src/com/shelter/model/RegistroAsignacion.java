package com.shelter.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Constancia de una entrega de recurso a un damnificado. Es inmutable: una constancia
 * no se modifica después de emitida.
 */
public class RegistroAsignacion {
    private final String idAlbergue;
    private final String idDamnificado;
    private final String dni;
    private final String nombreDamnificado;
    private final String recurso;
    private final String unidad;
    private final double cantidad;
    private final LocalDateTime fechaHora;

    // Entrega nueva: la fecha y hora son las del momento
    public RegistroAsignacion(String idAlbergue, String idDamnificado, String dni, String nombreDamnificado,
                              String recurso, String unidad, double cantidad) {
        this(idAlbergue, idDamnificado, dni, nombreDamnificado, recurso, unidad, cantidad, LocalDateTime.now());
    }

    public RegistroAsignacion(String idAlbergue, String idDamnificado, String dni, String nombreDamnificado,
                              String recurso, String unidad, double cantidad, LocalDateTime fechaHora) {
        this.idAlbergue = idAlbergue;
        this.idDamnificado = idDamnificado;
        this.dni = dni;
        this.nombreDamnificado = nombreDamnificado;
        this.recurso = recurso;
        this.unidad = unidad;
        this.cantidad = cantidad;
        this.fechaHora = fechaHora;
    }

    // Getters
    public String getIdAlbergue() { return idAlbergue; }
    public String getDni() { return dni; }
    public String getIdDamnificado() { return idDamnificado; }
    public String getNombreDamnificado() { return nombreDamnificado; }
    public String getRecurso() { return recurso; }
    public String getUnidad() { return unidad; }
    public double getCantidad() { return cantidad; }
    public LocalDateTime getFechaHora() { return fechaHora; }

    @Override
    public String toString() {
        return String.format("%s | Albergue %s | %s %s (DNI %s) recibió %.2f %s de %s",
                fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                idAlbergue, idDamnificado, nombreDamnificado, dni, cantidad, unidad, recurso);
    }
}
