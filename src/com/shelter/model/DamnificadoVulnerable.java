package com.shelter.model;

/**
 * Damnificado vulnerable (gestante, enfermedad crónica, discapacidad, etc.).
 *
 *  - atencionMedica pertenece a la clase padre (se hereda isAtencionMedica / setAtencionMedica).
 *  - Prioridades: CRÍTICA con atención médica, ALTA si está
 *    gestante, MEDIA en el resto; y si por edad le corresponde algo más urgente, gana la edad.
 *  - El parámetro "condicion" ya no se descarta: se guarda (texto libre: "Discapacitado", etc.).
 *  - confirmacionAtencion y estadoPostAtencion ya no imprimen por consola (el modelo no debe
 *    imprimir: la interfaz decide cómo mostrar). Ahora GUARDAN el estado y devuelven el texto.
 */
public class DamnificadoVulnerable extends Damnificado {
    private boolean enfermedadCronica;
    private boolean estadoGestacion;
    private String condicion;
    private boolean atendido;   // verificación de atención (avance 2: "verificación post-atención")
    private boolean estable;    // estado tras la atención

    public DamnificadoVulnerable(String dni, String nombreCompleto, int edad, boolean atencionMedica,
                                 String condicion, boolean estadoGestacion, boolean enfermedadCronica) {
        super(dni, nombreCompleto, edad, atencionMedica);
        this.condicion = (condicion == null) ? "" : condicion.trim();
        this.enfermedadCronica = enfermedadCronica;
        this.estadoGestacion = estadoGestacion;
    }

    /** Registra si ya fue atendido y devuelve un mensaje neutro (sin imprimir). */
    public String confirmacionAtencion(boolean atencion) {
        this.atendido = atencion;
        return atencion ? nombreCompleto + " ha sido atendido/a." : nombreCompleto + " todavía no ha sido atendido/a.";
    }

    /** Registra el estado posterior a la atención y devuelve el mensaje correspondiente. */
    public String estadoPostAtencion(boolean estado) {
        this.estable = estado;
        return estado ? "Se encuentra ahora mismo estable" : "Ahora mismo se está recuperando";
    }

    // Nivel que corresponde solo por su condición (sin mirar la edad).
    private NivelPrioridad nivelPorCondicion() {
        if (atencionMedica) {
            return NivelPrioridad.CRITICA;
        } else if (estadoGestacion) {
            return NivelPrioridad.ALTA;
        }
        return NivelPrioridad.MEDIA; // crónico o vulnerable básico: nunca BAJA
    }

    @Override
    public NivelPrioridad getNivelPrioridad() {
        return NivelPrioridad.masUrgente(nivelPorCondicion(), calcularNivelBase());
    }

    @Override
    public boolean necesitaMedicina() {
        return atencionMedica || enfermedadCronica || estadoGestacion;
    }

    @Override
    public String determinarNivelPrioridad() {
        NivelPrioridad nivel = getNivelPrioridad();
        String detalle;
        if (atencionMedica) {
            detalle = "Requiere asistencia paramédica inmediata";
        } else if (estadoGestacion) {
            detalle = "Requiere cama especial y evaluación";
        } else if (enfermedadCronica) {
            detalle = "Requiere registro de medicamentos";
        } else {
            detalle = "Asignar a zona segura";
        }
        String texto = nivel.getEtiqueta() + " - " + detalle;
        if (nivel.esMasUrgenteQue(nivelPorCondicion())) {
            texto += " (prioridad elevada por edad)";
        }
        return texto;
    }

    // Getters y Setters
    public boolean isEnfermedadCronica() {
        return enfermedadCronica;
    }

    public void setEnfermedadCronica(boolean enfermedadCronica) {
        this.enfermedadCronica = enfermedadCronica;
    }

    public boolean isEstadoGestacion() {
        return estadoGestacion;
    }

    public void setEstadoGestacion(boolean estadoGestacion) {
        this.estadoGestacion = estadoGestacion;
    }

    public String getCondicion() {
        return condicion;
    }

    public boolean isAtendido() {
        return atendido;
    }

    public void setAtendido(boolean atendido) {
        this.atendido = atendido;
    }

    public boolean isEstable() {
        return estable;
    }

    public void setEstable(boolean estable) {
        this.estable = estable;
    }
}
