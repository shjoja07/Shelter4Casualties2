package com.shelter.model;

public class DamnificadoMenor extends Damnificado {
    private String dniTutor;
    private String nombreTutor;
    private String parentesco;
    
    public DamnificadoMenor(String dni, String nombreCompleto, int edad, String dniTutor, String nombreTutor, String parentesco ) {
        super(dni, nombreCompleto, edad);
        this.dniTutor = dniTutor;
        this.nombreTutor = nombreTutor;
        this.parentesco = parentesco;
    }
    
    //Getters & Setters
    public String getDniTutor(){
        return dniTutor;
    }
    public void setDniTutor(String dniTutor){
        this.dniTutor = dniTutor;
    }
    
    public String getNombreTutor(){
        return nombreTutor;
    }
    public void setNombreTutor(String nombreTutor){
        this.nombreTutor = nombreTutor;
    }
    
    public String getParentesco(){
        return parentesco;
    }
    public void setParentesco(String parentesco){
        this.parentesco = parentesco;
    }
    
    
    @Override
    public String determinarNivelPrioridad() {
        return "BAJA - Sin necesidades especiales. Asignación a pabellón Familiar.";
    }
    
}
