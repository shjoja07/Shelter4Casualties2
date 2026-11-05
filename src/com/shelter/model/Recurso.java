package com.shelter.model;

import java.time.LocalDate;

public class Recurso implements Distribuible {
    private TipoRecurso tipo;
    private String nombre;
    private String unidad;
    private double cantidadDisponible;
    private LocalDate fechaVencimiento; // null si el recurso no vence

    public Recurso(TipoRecurso tipo, String nombre, String unidad,
                   double cantidadDisponible, LocalDate fechaVencimiento) {
        if (cantidadDisponible < 0) {
            throw new IllegalArgumentException("La cantidad disponible no puede ser negativa.");
        }
        this.tipo = tipo;
        this.nombre = nombre;
        this.unidad = unidad;
        this.cantidadDisponible = cantidadDisponible;
        this.fechaVencimiento = fechaVencimiento;
    }

    // Resta del stock la cantidad entregada
    @Override
    public void descontar(double cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor que cero.");
        }
        if (cantidad > cantidadDisponible) {
            throw new IllegalArgumentException("Stock insuficiente de " + nombre
                    + ": disponible " + cantidadDisponible + " " + unidad + ", solicitado " + cantidad + ".");
        }
        this.cantidadDisponible -= cantidad;
    }

    // RF-03: Actualizar inventario (nueva cantidad y nueva fecha de vencimiento)
    public void actualizar(double nuevaCantidad, LocalDate nuevaFechaVencimiento) {
        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        this.cantidadDisponible = nuevaCantidad;
        this.fechaVencimiento = nuevaFechaVencimiento;
    }

    // Un recurso está vencido si la fecha de referencia es posterior a su vencimiento
    @Override
    public boolean estaVencido(LocalDate fechaReferencia) {
        return fechaVencimiento != null && fechaReferencia.isAfter(fechaVencimiento);
    }

    // Getters
    public TipoRecurso getTipo() { return tipo; }
    @Override public String getNombre() { return nombre; }
    @Override public String getUnidad() { return unidad; }
    @Override public double getCantidadDisponible() { return cantidadDisponible; }
    @Override public LocalDate getFechaVencimiento() { return fechaVencimiento; }

    @Override
    public String toString() {
        String vence = (fechaVencimiento == null) ? "no vence" : "vence " + fechaVencimiento;
        return String.format("[%s] %s - %.2f %s (%s)", tipo, nombre, cantidadDisponible, unidad, vence);
    }
}
