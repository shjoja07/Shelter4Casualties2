package com.shelter.model;

import java.time.LocalDate;

/**
 * Lote de un recurso en el inventario de un albergue.
 *
 * CORRECCIÓN: se validan todos los datos. Antes una cantidad NaN pasaba el control "cantidad < 0"
 * (toda comparación con NaN es falsa) y un nombre o unidad null provocaba NullPointerException más
 * tarde, al comparar lotes. Tipo, nombre y unidad ahora son inmutables.
 */
public class Recurso implements Distribuible {
    private final TipoRecurso tipo;
    private final String nombre;
    private final String unidad;
    private double cantidadDisponible;
    private LocalDate fechaVencimiento; // null si el recurso no vence

    public Recurso(TipoRecurso tipo, String nombre, String unidad,
                   double cantidadDisponible, LocalDate fechaVencimiento) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de recurso es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del recurso no puede estar vacío.");
        }
        if (unidad == null || unidad.isBlank()) {
            throw new IllegalArgumentException("La unidad del recurso no puede estar vacía.");
        }
        validarCantidad(cantidadDisponible);
        this.tipo = tipo;
        this.nombre = nombre.trim();
        this.unidad = unidad.trim();
        this.cantidadDisponible = cantidadDisponible;
        this.fechaVencimiento = fechaVencimiento;
    }

    private static void validarCantidad(double cantidad) {
        if (!Double.isFinite(cantidad) || cantidad < 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número válido y no negativo.");
        }
    }

    // Resta del stock la cantidad entregada
    @Override
    public void descontar(double cantidad) {
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
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
        validarCantidad(nuevaCantidad);
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
