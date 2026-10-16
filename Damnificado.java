
public abstract class Damnificado {

    protected String dni;
    protected String nombreCompleto;
    protected int edad;
    protected CategoriaEdad categoria; 

    public enum CategoriaEdad {
        NINO, JOVEN, ADULTO, ADULTO_MAYOR
    }

    // Constructor
    public Damnificado(String dni, String nombreCompleto, int edad) {
        this.dni = dni;
        this.nombreCompleto = nombreCompleto;
        this.edad = edad;
        this.categoria = asignarCategoriaPorEdad(edad);
    }

    // Método privado para automatizar la categoría sin que el usuario tenga que escribirla
    private CategoriaEdad asignarCategoriaPorEdad(int edad) {
        if (edad <= 12) {
            return CategoriaEdad.NINO;
        } else if (edad <= 29) {
            return CategoriaEdad.JOVEN;
        } else if (edad <= 59) {
            return CategoriaEdad.ADULTO;
        } else {
            return CategoriaEdad.ADULTO_MAYOR;
        }
    }

    // Un método sin llaves. Obliga a que cualquier clase hija tenga que programar su propia forma de calcular la prioridad basándose en los Estándares Esfera.
    public abstract String determinarNivelPrioridad();

    // --- Getters y Setters tradicionales ---
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public int getEdad() { return edad; }
    public void setEdad(int edad) { 
        this.edad = edad; 
        this.categoria = asignarCategoriaPorEdad(edad); // Actualiza la categoría si cambia la edad
    }

    public CategoriaEdad getCategoria() { return categoria; }
}