package com.shelter.model;

/**
 * Clase padre de todo damnificado. Cada subclase define su propia prioridad.
 *
 *  - dni y nombreCompleto son FINAL: la identificación no se debe cambiar.
 *  - La atención médica vive aquí, no en los hijos: cualquier persona
 *    (general, menor o vulnerable) puede necesitarla.
 *  - Se agrega getNivelPrioridad() abstracto: la prioridad se decide por polimorfismo, de modo que
 *    añadir un nuevo subtipo NO obliga a modificar AlbergueService.
 *  - Los números de edad pasan a constantes con nombre.
 *  - Se validan los datos en el constructor (DNI/nombre no vacíos, edad entre 0 y 120).
 */
public abstract class Damnificado {

    // ----- Constantes de edad (antes estaban dispersas como números sueltos) -----
    public static final int EDAD_MAXIMA = 120;
    /** Menor de edad: tiene menos de 18 años. */
    public static final int EDAD_MAYORIA = 18;
    public static final int EDAD_MAX_NINO = 12;
    public static final int EDAD_MAX_JOVEN = 29;
    public static final int EDAD_MAX_ADULTO = 59;
    /** Desde esta edad la persona es adulto mayor y recibe prioridad alta. */
    public static final int EDAD_ADULTO_MAYOR = 60;
    /** Hasta esta edad (inclusive) un niño pequeño recibe prioridad alta. */
    public static final int EDAD_NINO_PRIORITARIO = 5;
    /** Longitud del DNI peruano. */
    public static final int LONGITUD_DNI = 8;

    protected final String dni;
    protected final String nombreCompleto;
    protected int edad;
    protected CategoriaEdad categoria;
    protected boolean atencionMedica;

    public enum CategoriaEdad {
        NINO, JOVEN, ADULTO, ADULTO_MAYOR
    }

    // Constructor original (3 parámetros): se mantiene por compatibilidad; sin atención médica.
    public Damnificado(String dni, String nombreCompleto, int edad) {
        this(dni, nombreCompleto, edad, false);
    }

    // Constructor completo.
    public Damnificado(String dni, String nombreCompleto, int edad, boolean atencionMedica) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío.");
        }
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        validarEdad(edad);
        this.dni = dni.trim();
        this.nombreCompleto = nombreCompleto.trim();
        this.edad = edad;
        this.categoria = asignarCategoriaPorEdad(edad);
        this.atencionMedica = atencionMedica;
    }

    private static void validarEdad(int edad) {
        if (edad < 0 || edad > EDAD_MAXIMA) {
            throw new IllegalArgumentException("La edad debe estar entre 0 y " + EDAD_MAXIMA + ".");
        }
    }

    /** Un DNI peruano válido son exactamente 8 dígitos. Lo usan la consola y (luego) Swing. */
    public static boolean esDniValido(String dni) {
        return dni != null && dni.matches("\\d{" + LONGITUD_DNI + "}");
    }

    // Método privado para automatizar la categoría sin que el usuario tenga que escribirla
    private CategoriaEdad asignarCategoriaPorEdad(int edad) {
        if (edad <= EDAD_MAX_NINO) {
            return CategoriaEdad.NINO;
        } else if (edad <= EDAD_MAX_JOVEN) {
            return CategoriaEdad.JOVEN;
        } else if (edad <= EDAD_MAX_ADULTO) {
            return CategoriaEdad.ADULTO;
        } else {
            return CategoriaEdad.ADULTO_MAYOR;
        }
    }

    /**
     * Regla común a todos: atención médica => CRÍTICA; adulto mayor o niño pequeño => ALTA;
     * en otro caso BAJA. Las subclases parten de este nivel base y lo ajustan.
     */
    protected NivelPrioridad calcularNivelBase() {
        if (atencionMedica) {
            return NivelPrioridad.CRITICA;
        }
        if (edad >= EDAD_ADULTO_MAYOR || edad <= EDAD_NINO_PRIORITARIO) {
            return NivelPrioridad.ALTA;
        }
        return NivelPrioridad.BAJA;
    }

    /** ÚNICA fuente de verdad de la prioridad. Cada subclase programa su propia versión. */
    public abstract NivelPrioridad getNivelPrioridad();

    /**
     * Texto descriptivo del nivel (se muestra al registrar el ingreso). Se conserva el método
     * original; ahora cada subclase construye el texto a partir de getNivelPrioridad(),
     * por lo que nunca puede contradecir al nivel real.
     */
    public abstract String determinarNivelPrioridad();

    /** ¿Debe recibir MEDICINA con prioridad? (la usa AsignacionRecurso para no repartirla a todos). */
    public boolean necesitaMedicina() {
        return atencionMedica;
    }

    // --- Getters y Setters ---
    public String getDni() { return dni; }

    public String getNombreCompleto() { return nombreCompleto; }

    public int getEdad() { return edad; }

    public void setEdad(int edad) {
        validarEdad(edad);
        this.edad = edad;
        this.categoria = asignarCategoriaPorEdad(edad); // Actualiza la categoría si cambia la edad
    }

    public CategoriaEdad getCategoria() { return categoria; }

    public boolean isAtencionMedica() { return atencionMedica; }

    public void setAtencionMedica(boolean atencionMedica) { this.atencionMedica = atencionMedica; }

    // Dos damnificados son la misma persona si tienen el mismo DNI (evita duplicados en listas).
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Damnificado otro && dni.equals(otro.dni);
    }

    @Override
    public int hashCode() {
        return dni.hashCode();
    }

    @Override
    public String toString() {
        return nombreCompleto + " (DNI " + dni + ", " + edad + " años)";
    }
}
