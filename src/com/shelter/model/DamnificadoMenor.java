package com.shelter.model;

/**
 * Damnificado menor de 18 años.
 *
 * CAMBIOS (PASOS 1 y 2):
 *  - Existe un constructor completo con todos los datos del tutor.
 *  - Se valida que sea menor de edad (< 18).
 *  - Prioridad: se mantiene BAJA como punto de partida (el menor ACOMPAÑADO está
 *    protegido por su tutor), pero un menor NO ACOMPAÑADO (sin tutor) sube a ALTA, y las reglas
 *    comunes siguen aplicando (atención médica => CRÍTICA; niño de 5 años o menos => ALTA).
 */
public class DamnificadoMenor extends Damnificado {
    private String dniTutor;
    private String nombreTutor;
    private String parentesco;

    // Constructor original (compatible): solo se conoce el DNI del tutor (null/vacío = sin tutor).
    public DamnificadoMenor(String dni, String nombreCompleto, int edad, boolean atencionMedica, String dniTutor) {
        this(dni, nombreCompleto, edad, atencionMedica, dniTutor, null, null);
    }

    // Constructor completo con los datos del tutor.
    public DamnificadoMenor(String dni, String nombreCompleto, int edad, boolean atencionMedica,
                            String dniTutor, String nombreTutor, String parentesco) {
        super(dni, nombreCompleto, edad, atencionMedica);
        if (edad >= EDAD_MAYORIA) {
            throw new IllegalArgumentException("Un menor debe tener menos de " + EDAD_MAYORIA + " años.");
        }
        this.dniTutor = dniTutor;
        this.nombreTutor = nombreTutor;
        this.parentesco = parentesco;
    }

    /** Presencia de tutor (requisito del avance 2). */
    public boolean tieneTutor() {
        return dniTutor != null && !dniTutor.isBlank();
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
    public NivelPrioridad getNivelPrioridad() {
        NivelPrioridad nivel = calcularNivelBase();
        if (!tieneTutor()) {
            // Menor no acompañado: nunca puede quedar en prioridad baja.
            nivel = NivelPrioridad.masUrgente(nivel, NivelPrioridad.ALTA);
        }
        return nivel;
    }

    @Override
    public String determinarNivelPrioridad() {
        NivelPrioridad nivel = getNivelPrioridad();
        if (nivel == NivelPrioridad.CRITICA) {
            return nivel.getEtiqueta() + " - Requiere asistencia paramédica inmediata";
        }
        if (!tieneTutor()) {
            return nivel.getEtiqueta() + " - Menor no acompañado. Asignar a zona de protección y ubicar a un familiar.";
        }
        if (nivel == NivelPrioridad.ALTA) {
            return nivel.getEtiqueta() + " - Niño pequeño. Asignación a pabellón Familiar con atención preferente.";
        }
        return nivel.getEtiqueta() + " - Sin necesidades especiales. Asignación a pabellón Familiar.";
    }
}
