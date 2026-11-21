package com.shelter.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.text.Normalizer;

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
    /** Prefijo de los códigos de damnificado (SANMI, REFCE...). Se calcula una sola vez. */
    private final String prefijo;
    private final List<Damnificado> damnificados = new ArrayList<>();
    private final List<Recurso> recursos = new ArrayList<>();
    private int siguienteNumeroDamnificado = 1;
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
        this.prefijo = calcularPrefijo(this.nombre);
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

    /**
     * Aloja a un damnificado y le asigna un código propio de este refugio.
     *
     * CORRECCIÓN: si la persona ya trae un código de este refugio (caso de la carga desde JSON) se
     * conserva, pero el contador avanza por encima de ese número. Antes el contador volvía a 1 al
     * reiniciar el programa y podía reutilizar el código de alguien que ya salió, mezclando su
     * historial de entregas con el de la persona nueva. Un código repetido se regenera.
     */
    public boolean alojar(Damnificado damnificado) {
        if (damnificado == null) {
            throw new IllegalArgumentException("El damnificado no puede ser nulo.");
        }
        if (!tieneCupo()
                || buscarDamnificado(damnificado.getDni()).isPresent()
                || damnificados.stream().anyMatch(d -> d.getNombreCompleto()
                .equalsIgnoreCase(damnificado.getNombreCompleto()))) {
            return false;
        }
        boolean conservaCodigo = damnificado.tieneIdAsignado()
                && damnificado.getId().startsWith(prefijo + "-")
                && buscarDamnificadoPorId(damnificado.getId()).isEmpty();
        if (conservaCodigo) {
            reservarNumero(damnificado.getId());
        } else {
            damnificado.asignarId(generarIdDamnificado());
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

    public Optional<Damnificado> buscarDamnificadoPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return damnificados.stream().filter(d -> d.getId().equalsIgnoreCase(id.trim())).findFirst();
    }

    /**
     * Genera un prefijo legible a partir del nombre: San Miguel -> SANMI,
     * Refugio Central -> REFCE. El sufijo ordenado evita colisiones dentro del refugio.
     */
    static String calcularPrefijo(String nombre) {
        String base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        String[] palabras = base.split("\\s+");
        StringBuilder prefijo = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty() && prefijo.length() < 5) {
                prefijo.append(palabra, 0, Math.min(3, palabra.length()));
            }
        }
        if (prefijo.length() < 5 && palabras.length == 1) {
            prefijo.setLength(0);
            prefijo.append(palabras[0], 0, Math.min(5, palabras[0].length()));
        }
        while (prefijo.length() < 3) {
            prefijo.append('X');
        }
        return prefijo.substring(0, Math.min(5, prefijo.length()));
    }

    /** Prefijo de los códigos de este refugio. AlbergueService exige que sea único en la red. */
    public String getPrefijo() {
        return prefijo;
    }

    private String generarIdDamnificado() {
        String candidato;
        do {
            candidato = prefijo + "-" + String.format("%05d", siguienteNumeroDamnificado++);
        } while (buscarDamnificadoPorId(candidato).isPresent());
        return candidato;
    }

    /** Hace que el contador quede por encima del número de un código ya existente. */
    private void reservarNumero(String codigo) {
        try {
            int numero = Integer.parseInt(codigo.substring(codigo.lastIndexOf('-') + 1));
            siguienteNumeroDamnificado = Math.max(siguienteNumeroDamnificado, numero + 1);
        } catch (NumberFormatException ignorado) {
            // El formato ya lo valida Damnificado.asignarId; aquí no puede ocurrir.
        }
    }

    /** Siguiente número que se usará para un código nuevo (se guarda en el JSON). */
    public int getSiguienteNumeroDamnificado() {
        return siguienteNumeroDamnificado;
    }

    /** Restaura el contador guardado. Nunca lo hace retroceder (evita reutilizar códigos). */
    public void restaurarSiguienteNumero(int siguiente) {
        siguienteNumeroDamnificado = Math.max(siguienteNumeroDamnificado, siguiente);
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
