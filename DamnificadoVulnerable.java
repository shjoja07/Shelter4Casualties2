public class DamnificadoVulnerable extends Damnificado{
    private boolean enfermedadCronica; 
    private boolean atencionMedica; 
    private boolean estadoGestacion; 

    public DamnificadoVulnerable(String dni, String nombreCompleto, int edad, boolean atencionMedica, boolean estadoGestacion, boolean enfermedadCronica){
        super(dni, nombreCompleto, edad); 
        this.atencionMedica = atencionMedica; 
        this.enfermedadCronica = enfermedadCronica;  
        this.estadoGestion = estadoGestacion; 
    }

    public void confirmacionAtencion(boolean atencion){
        if(atencion){
            System.out.println(nombreCompleto + " ha sido atendida.");
        }else{
            System.out.println(nombreCompleto + " todavia no ha sido atendida");
        }
    }

    public String estadoPostAtencion(boolean estado){
        if(estado){
            return "Se encuentra ahora mismo estable";
        }else{
            return "Ahora mismo se esta recuperando";
        }
    }

    // Getters y Setters
    public boolean isEnfermedadCronica() {
        return enfermedadCronica;
    }

    public void setEnfermedadCronica(boolean enfermedadCronica) {
        this.enfermedadCronica = enfermedadCronica;
    }

    public boolean isAtencionMedica() {
        return atencionMedica;
    }

    public void setAtencionMedica(boolean atencionMedica) {
        this.atencionMedica = atencionMedica;
    }

    public boolean isEstadoGestacion() {
        return estadoGestacion;
    }

    public void setEstadoGestacion(boolean estadoGestacion) {
        this.estadoGestacion = estadoGestacion;
    }

    @Override
    public String determinarNivelPrioridad() {
        if (atencionMedica) {
            return "CRÍTICA - Requiere asistencia paramédica inmediata";
        } else if (estadoGestacion) {
            return "ALTA - Requiere cama especial y evaluación";
        } else if (enfermedadCronica) {
            return "MEDIA - Requiere registro de medicamentos";
        } else {
            return "VULNERABLE BÁSICO - Asignar a zona segura";
        }
    }

}
