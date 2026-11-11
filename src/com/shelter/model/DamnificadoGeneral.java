package com.shelter.model;

/**
 * Damnificado sin condición especial. Su prioridad es la regla base:
 * BAJA, salvo que requiera atención médica (CRÍTICA) o sea niño pequeño / adulto mayor (ALTA).
 *
 * CAMBIO: antes el parámetro atencionMedica se recibía y se descartaba (se perdía el dato);
 * ahora se guarda en la clase padre y sí influye en la prioridad.
 */
public class DamnificadoGeneral extends Damnificado {

    public DamnificadoGeneral(String dni, String nombreCompleto, int edad, boolean atencionMedica) {
        super(dni, nombreCompleto, edad, atencionMedica);
    }

    @Override
    public NivelPrioridad getNivelPrioridad() {
        return calcularNivelBase();
    }

    @Override
    public String determinarNivelPrioridad() {
        NivelPrioridad nivel = getNivelPrioridad();
        return switch (nivel) {
            case CRITICA -> nivel.getEtiqueta() + " - Requiere asistencia paramédica inmediata";
            case ALTA -> nivel.getEtiqueta()
                    + " - Niño pequeño o adulto mayor. Asignación a pabellón general con atención preferente.";
            default -> nivel.getEtiqueta() + " - Sin necesidades especiales. Asignación a pabellón general.";
        };
    }
}
