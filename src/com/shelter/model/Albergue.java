package com.shelter.model;

import java.util.ArrayList;
import java.util.List;

public class Albergue {
    private String id;
    private String nombre;
    private String distrito;
    private int capacidadMaxima;
    private int ocupacionActual;
    //Atributos que agregue para que funcione lo de mi parte.    Atte: Adrian
    private List<Damnificado> damnificados = new ArrayList<>();
    private List<Recurso> recursos;
    private String estadoSemaforo; // "VERDE", "AMARILLO", "ROJO"

    public Albergue(String id, String nombre, String distrito, int capacidadMaxima) {
        this.id = id;
        this.nombre = nombre;
        this.distrito = distrito;
        this.capacidadMaxima = capacidadMaxima;
        this.ocupacionActual = 0;
        this.estadoSemaforo = "VERDE";
        //Lo mismo
        this.damnificados = new ArrayList<>();
        this.recursos = new ArrayList<>();
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
    //Lo mismo
    public List<Damnificado> getDamnificados() { return damnificados; }
    public List<Recurso> getRecursos() { return recursos; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Cupos: %d/%d - Semáforo: %s",
                id, nombre, distrito, getCuposDisponibles(), capacidadMaxima, estadoSemaforo);
    }

    public void registrarRecurso(Recurso recurso) {
        this.recursos.add(recurso); 
    }
}