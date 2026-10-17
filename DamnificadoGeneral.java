package com.shelter.model;

public class DamnificadoGeneral extends Damnificado{
    
    public DamnificadoGeneral(String dni, String nombreCompleto, int edad) {
        super(dni, nombreCompleto, edad);
    }
    
    @Override
    public String determinarNivelPrioridad() {
        return "BAJA - Sin necesidades especiales. Asignación a pabellón general.";
    }

}
