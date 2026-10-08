
package com.shelter.model;

public class Damnificado {
    private String dni;
    private String nombre;
    private int edad;
    private boolean requiereAtencionMedica;
    private int nivelPrioridad; // Calculado automáticamente (1 = Alta, 3 = Baja)

    public Damnificado(String dni, String nombre, int edad, boolean requiereAtencionMedica) {
        this.dni = dni;
        this.nombre = nombre;
        this.edad = edad;
        this.requiereAtencionMedica = requiereAtencionMedica;
    }

    // Getters y Setters
    public String getDni() { return dni; }
    public String getNombre() { return nombre; }
    public int getEdad() { return edad; }
    public boolean isRequiereAtencionMedica() { return requiereAtencionMedica; }
    public int getNivelPrioridad() { return nivelPrioridad; }
    public void setNivelPrioridad(int nivelPrioridad) { this.nivelPrioridad = nivelPrioridad; }
}