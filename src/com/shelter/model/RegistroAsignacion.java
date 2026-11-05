package com.shelter.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Constancia de una entrega de recurso a un damnificado.
 */
public class RegistroAsignacion {
    private String idAlbergue;
    private String dni;
    private String nombreDamnificado;
    private String recurso;
    private String unidad;
    private double cantidad;
    private LocalDateTime fechaHora;

    public RegistroAsignacion(String idAlbergue, String dni, String nombreDamnificado,
                              String recurso, String unidad, double cantidad) {
        this.idAlbergue = idAlbergue;
        this.dni = dni;
        this.nombreDamnificado = nombreDamnificado;
        this.recurso = recurso;
        this.unidad = unidad;
        this.cantidad = cantidad;
        this.fechaHora = LocalDateTime.now();
    }

    // Getters
    public String getIdAlbergue() { return idAlbergue; }
    public String getDni() { return dni; }
    public String getNombreDamnificado() { return nombreDamnificado; }
    public String getRecurso() { return recurso; }
    public String getUnidad() { return unidad; }
    public double getCantidad() { return cantidad; }
    public LocalDateTime getFechaHora() { return fechaHora; }

    @Override
    public String toString() {
        return String.format("%s | Albergue %s | %s (DNI %s) recibió %.2f %s de %s",
                fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                idAlbergue, nombreDamnificado, dni, cantidad, unidad, recurso);
    }
}
