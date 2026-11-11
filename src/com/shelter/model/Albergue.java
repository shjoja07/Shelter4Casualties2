package com.shelter.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Albergue temporal de la red.
 *
 *  - La ocupación ya no es un contador aparte, getOcupacionActual() es el tamaño de la lista de
 *    damnificados. Ahora es imposible que ocupación y personas alojadas se desincronicen, y se eliminó setOcupacionActual().
 *  - getDamnificados() / getRecursos() devuelven listas de SOLO LECTURA. Para cambiar el albergue
 *    se usan los métodos alojar(), retirar() y registrarRecurso().
 *  - Se agregan tipoInfraestructura y areaM2.
 *  - El estado semáforo es un enum calculado por AlbergueService (ya no es solo un String).
 *  - Se validan los datos del constructor (id/nombre no vacíos, capacidad > 0, área >= 0).
 */
public class Albergue {

    public static final String INFRAESTRUCTURA_NO_ESPECIFICADA = "No especificada";

    private final String id;
    private final String nombre;
    private final String distrito;
    private final String tipoInfraestructura;
    private final int capacidadMaxima;
    private final double areaM2;
    private final List<Damnificado> damnificados = new ArrayList<>();
    private final List<Recurso> recursos = new ArrayList<>();
    private NivelSemaforo estadoSemaforo = NivelSemaforo.VERDE;

    // Constructor original (4 parámetros): se conserva. Sin área ni tipo de infraestructura.
    public Albergue(String id, String nombre, String distrito, int capacidadMaxima) {
        this(id, nombre, distrito, INFRAESTRUCTURA_NO_ESPECIFICADA, capacidadMaxima, 0.0);
    }

    // Constructor completo. areaM2 = 0 significa "área no registrada" (no se valida espacio vital).
    public Albergue(String id, String nombre, String distrito, String tipoInfraestructura,
                    int capacidadMaxima, double areaM2) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del albergue no puede estar vacío.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del albergue no puede estar vacío.");
        }
        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor que cero.");
        }
        if (Double.isNaN(areaM2) || Double.isInfinite(areaM2) || areaM2 < 0) {
            throw new IllegalArgumentException("El área no puede ser negativa.");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
        this.distrito = (distrito == null) ? "" : distrito.trim();
        this.tipoInfraestructura = (tipoInfraestructura == null || tipoInfraestructura.isBlank())
                ? INFRAESTRUCTURA_NO_ESPECIFICADA : tipoInfraestructura.trim();
        this.capacidadMaxima = capacidadMaxima;
        this.areaM2 = areaM2;
    }

    // ------------------------------------------------------------------
    // Ocupación (todo se deriva de la lista de damnificados)
    // ------------------------------------------------------------------

    public int getOcupacionActual() {
        return damnificados.size();
    }

    public boolean tieneCupo() {
        return getOcupacionActual() < capacidadMaxima;
    }

    public int getCuposDisponibles() {
        return capacidadMaxima - getOcupacionActual();
    }

    /** Porcentaje de ocupación (0 a 100). Alimenta las barras de progreso. */
    public double getPorcentajeOcupacion() {
        return getOcupacionActual() * 100.0 / capacidadMaxima;
    }

    // ------------------------------------------------------------------
    // Damnificados
    // ------------------------------------------------------------------

    /** Aloja a un damnificado. Devuelve false si no hay cupo o si su DNI ya está en este albergue. */
    public boolean alojar(Damnificado damnificado) {
        if (damnificado == null) {
            throw new IllegalArgumentException("El damnificado no puede ser nulo.");
        }
        if (!tieneCupo() || buscarDamnificado(damnificado.getDni()).isPresent()) {
            return false;
        }
        damnificados.add(damnificado);
        return true;
    }

    /** Retira a un damnificado del albergue. Devuelve false si no estaba alojado aquí. */
    public boolean retirar(Damnificado damnificado) {
        return damnificado != null && damnificados.remove(damnificado);
    }

    public Optional<Damnificado> buscarDamnificado(String dni) {
        if (dni == null) {
            return Optional.empty();
        }
        String buscado = dni.trim();
        return damnificados.stream().filter(d -> d.getDni().equals(buscado)).findFirst();
    }

    /** Lista de solo lectura: para modificar el albergue usar alojar() / retirar(). */
    public List<Damnificado> getDamnificados() {
        return Collections.unmodifiableList(damnificados);
    }

    // ------------------------------------------------------------------
    // Recursos (RF-03)
    // ------------------------------------------------------------------

    /**
     * Registra un recurso en el inventario. Si ya existe un lote idéntico (mismo tipo, nombre,
     * unidad y fecha de vencimiento) se SUMA la cantidad en vez de crear un duplicado.
     * Devuelve el recurso que quedó en el inventario.
     */
    public Recurso registrarRecurso(Recurso nuevo) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El recurso no puede ser nulo.");
        }
        for (Recurso existente : recursos) {
            if (existente.getTipo() == nuevo.getTipo()
                    && existente.getNombre().equalsIgnoreCase(nuevo.getNombre())
                    && existente.getUnidad().equalsIgnoreCase(nuevo.getUnidad())
                    && Objects.equals(existente.getFechaVencimiento(), nuevo.getFechaVencimiento())) {
                existente.actualizar(existente.getCantidadDisponible() + nuevo.getCantidadDisponible(),
                        existente.getFechaVencimiento());
                return existente;
            }
        }
        recursos.add(nuevo);
        return nuevo;
    }

    /** Lista de solo lectura del inventario. */
    public List<Recurso> getRecursos() {
        return Collections.unmodifiableList(recursos);
    }

    /** Cantidad total VIGENTE (no vencida) de un tipo de recurso, a la fecha indicada. */
    public double getCantidadTotal(TipoRecurso tipo, LocalDate fechaReferencia) {
        double total = 0;
        for (Recurso r : recursos) {
            if (r.getTipo() == tipo && !r.estaVencido(fechaReferencia)) {
                total += r.getCantidadDisponible();
            }
        }
        return total;
    }

    public double getCantidadTotal(TipoRecurso tipo) {
        return getCantidadTotal(tipo, LocalDate.now());
    }

    public boolean tieneRecursoDisponible(TipoRecurso tipo) {
        return getCantidadTotal(tipo) > 0;
    }

    // ------------------------------------------------------------------
    // Estado semáforo (lo calcula AlbergueService)
    // ------------------------------------------------------------------

    public NivelSemaforo getEstadoSemaforo() { return estadoSemaforo; }

    public void setEstadoSemaforo(NivelSemaforo estadoSemaforo) {
        this.estadoSemaforo = Objects.requireNonNull(estadoSemaforo);
    }

    // Datos de identificación (no cambian)
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDistrito() { return distrito; }
    public String getTipoInfraestructura() { return tipoInfraestructura; }
    public int getCapacidadMaxima() { return capacidadMaxima; }
    public double getAreaM2() { return areaM2; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) - Ocupación: %d/%d (%.0f%%) - Semáforo: %s",
                id, nombre, distrito, getOcupacionActual(), capacidadMaxima,
                getPorcentajeOcupacion(), estadoSemaforo.getEtiqueta());
    }
}
