package com.shelter.model;

public class Albergue {
    private String id;
    private String nombre;
    private String distrito;
    private int capacidadMaxima;
    private int ocupacionActual;
    private String estadoSemaforo; // "VERDE", "AMARILLO", "ROJO"

    public Albergue(String id, String nombre, String distrito, int capacidadMaxima) {
        this.id = id;
        this.nombre = nombre;
        this.distrito = distrito;
        this.capacidadMaxima = capacidadMaxima;
        this.ocupacionActual = 0;
        this.estadoSemaforo = "VERDE";
    }

    // Métodos auxiliares de negocio
    public boolean tieneCupo() {
        return ocupacionActual < capacidadMaxima;
    }

    public int getCuposDisponibles() {
        return capacidadMaxima - ocupacionActual;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDistrito() { return distrito; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public int getOcupacionActual() { return ocupacionActual; }
    public void setOcupacionActual(int ocupacionActual) { this.ocupacionActual = ocupacionActual; }
    public String getEstadoSemaforo() { return estadoSemaforo; }
    public void setEstadoSemaforo(String estadoSemaforo) { this.estadoSemaforo = estadoSemaforo; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Cupos: %d/%d - Semáforo: %s",
                id, nombre, distrito, getCuposDisponibles(), capacidadMaxima, estadoSemaforo);
    }
}