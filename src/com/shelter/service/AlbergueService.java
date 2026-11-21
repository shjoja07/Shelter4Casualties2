package com.shelter.service;

import com.shelter.config.SphereConstantes;
import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.Recurso;
import com.shelter.model.TipoRecurso;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Controlador de la lógica de negocio de la red de albergues.
 *
 * Esta clase no imprime nada; las operaciones devuelven resultados (boolean, enum, lista u Optional)
 * o lanza IllegalArgumentException si los datos son inválidos.
 *  Se reutiliza la lógica en la interfaz gráfica.
 *
 */
public class AlbergueService {

    // ----- Umbrales del semáforo de ocupación (porcentaje 0-100) -----
    /** A partir de este % de ocupación el albergue pasa a AMARILLO. */
    public static final double UMBRAL_AMARILLO = 70.0;
    /** A partir de este % de ocupación el albergue pasa a ROJO. */
    public static final double UMBRAL_ROJO = 90.0;
    /** Margen para avisar (AMARILLO) antes de incumplir un mínimo Sphere: 15 % por encima del mínimo. */
    public static final double MARGEN_ADVERTENCIA = 1.15;
    /** El agua se registra siempre en litros, para poder compararla con el mínimo Sphere (L/persona/día). */
    public static final String UNIDAD_AGUA = "litros";

    /** Resultados posibles de registrar el ingreso de un damnificado (RF-02). */
    public enum ResultadoIngreso {
        INGRESADO, ALBERGUE_NO_ENCONTRADO, SIN_CUPO, IDENTIDAD_DUPLICADA
    }

    /** Resultados posibles de derivar/reubicar a un damnificado (RF-08). */
    public enum ResultadoDerivacion {
        DERIVADO, ORIGEN_NO_ENCONTRADO, DESTINO_NO_ENCONTRADO, MISMO_ALBERGUE,
        DAMNIFICADO_NO_ENCONTRADO, DESTINO_SIN_CUPO
    }

    private final List<Albergue> albergues = new ArrayList<>();

    // =========================================================
    // ALBERGUES (RF-01)
    // =========================================================

    /** RF-01: registra un albergue. Devuelve false si ya existe uno con ese ID (sin distinguir mayúsculas). */
    public boolean registrarAlbergue(Albergue albergue) {
        if (albergue == null) {
            throw new IllegalArgumentException("El albergue no puede ser nulo.");
        }
        if (buscarPorId(albergue.getId()).isPresent()) {
            return false;
        }
        validarPrefijoLibre(albergue, albergues);
        albergues.add(albergue);
        actualizarSemaforo(albergue);
        return true;
    }

    /**
     * CORRECCIÓN: dos refugios con el mismo prefijo ("Refugio Central" y "Refugio Centro" generan
     * REFCE) repartían los mismos códigos (REFCE-00001 en ambos), y al derivar entre ellos la persona
     * conservaba su código aunque ya estuviera ocupado en el destino. Ahora el prefijo debe ser único.
     */
    private static void validarPrefijoLibre(Albergue nuevo, List<Albergue> existentes) {
        for (Albergue a : existentes) {
            if (a.getPrefijo().equals(nuevo.getPrefijo())) {
                throw new IllegalArgumentException("El nombre '" + nuevo.getNombre() + "' genera el prefijo de códigos "
                        + nuevo.getPrefijo() + ", que ya usa el albergue " + a.getId() + " (" + a.getNombre()
                        + "). Use un nombre más distintivo.");
            }
        }
    }

    public Optional<Albergue> buscarPorId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String buscado = id.trim();
        return albergues.stream().filter(a -> a.getId().equalsIgnoreCase(buscado)).findFirst();
    }

    // =========================================================
    // PRIORIDAD
    // =========================================================

    /**
     * Prioridad de asignación (1 = más urgente ... 4 = menos urgente).
     * No usa "instanceof": delega en el damnificado (polimorfismo), así que agregar un
     * subtipo nuevo no obliga a modificar este método (RNF-03).
     */
    public int calcularPrioridadAsignacion(Damnificado damnificado) {
        return damnificado.getNivelPrioridad().getOrden();
    }

    // =========================================================
    // DAMNIFICADOS (RF-02, RF-08)
    // =========================================================

    /** Busca en toda la red por código alfanumérico de refugio. */
    public Optional<Albergue> ubicarDamnificadoPorId(String id) {
        for (Albergue a : albergues) {
            if (a.buscarDamnificadoPorId(id).isPresent()) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public Optional<Damnificado> buscarDamnificadoPorId(String id) {
        return ubicarDamnificadoPorId(id).flatMap(a -> a.buscarDamnificadoPorId(id));
    }

    /** RF-02: registra el ingreso (la prioridad se calcula en el propio damnificado). */
    public ResultadoIngreso registrarIngreso(String idAlbergue, Damnificado damnificado) {
        if (damnificado == null) {
            throw new IllegalArgumentException("El damnificado no puede ser nulo.");
        }
        Optional<Albergue> opt = buscarPorId(idAlbergue);
        if (opt.isEmpty()) {
            return ResultadoIngreso.ALBERGUE_NO_ENCONTRADO;
        }
        if (ubicarPorDni(damnificado.getDni()).isPresent()
                || ubicarPorNombre(damnificado.getNombreCompleto()).isPresent()) {
            return ResultadoIngreso.IDENTIDAD_DUPLICADA;
        }
        Albergue albergue = opt.get();
        if (!albergue.alojar(damnificado)) {
            return ResultadoIngreso.SIN_CUPO;
        }
        actualizarSemaforo(albergue);
        return ResultadoIngreso.INGRESADO;
    }

    private Optional<Albergue> ubicarPorDni(String dni) {
        for (Albergue a : albergues) {
            if (a.buscarDamnificado(dni).isPresent()) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /** Consulta secundaria por DNI para verificación administrativa. */
    public Optional<Albergue> ubicarDamnificadoPorDni(String dni) {
        return ubicarPorDni(dni);
    }

    private Optional<Albergue> ubicarPorNombre(String nombre) {
        String buscado = nombre == null ? "" : nombre.trim();
        if (buscado.isEmpty()) {
            return Optional.empty();
        }
        for (Albergue a : albergues) {
            if (a.getDamnificados().stream()
                    .anyMatch(d -> d.getNombreCompleto().equalsIgnoreCase(buscado))) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    /** Derivación por ID, que es la operación recomendada por la aplicación. */
    public ResultadoDerivacion derivarDamnificado(String idOrigen, String idDestino, String idDamnificado) {
        Optional<Albergue> origen = buscarPorId(idOrigen);
        if (origen.isEmpty()) return ResultadoDerivacion.ORIGEN_NO_ENCONTRADO;
        Optional<Albergue> destino = buscarPorId(idDestino);
        if (destino.isEmpty()) return ResultadoDerivacion.DESTINO_NO_ENCONTRADO;
        if (origen.get() == destino.get()) return ResultadoDerivacion.MISMO_ALBERGUE;
        Optional<Damnificado> persona = origen.get().buscarDamnificadoPorId(idDamnificado);
        if (persona.isEmpty()) return ResultadoDerivacion.DAMNIFICADO_NO_ENCONTRADO;
        if (!destino.get().tieneCupo()) return ResultadoDerivacion.DESTINO_SIN_CUPO;
        origen.get().retirar(persona.get());
        if (!destino.get().alojar(persona.get())) {
            origen.get().alojar(persona.get());
            return ResultadoDerivacion.DESTINO_SIN_CUPO;
        }
        actualizarSemaforo(origen.get());
        actualizarSemaforo(destino.get());
        return ResultadoDerivacion.DERIVADO;
    }

    /** RF-08: registra la salida voluntaria usando el código del damnificado. */
    public boolean registrarSalida(String idAlbergue, String idDamnificado) {
        Optional<Albergue> albergue = buscarPorId(idAlbergue);
        if (albergue.isEmpty()) {
            return false;
        }
        Optional<Damnificado> persona = albergue.get().buscarDamnificadoPorId(idDamnificado);
        if (persona.isEmpty() || !albergue.get().retirar(persona.get())) {
            return false;
        }
        actualizarSemaforo(albergue.get());
        return true;
    }

    // =========================================================
    // CONSULTAS DE LA RED (RF-05, RF-07, RF-09)
    // =========================================================

    /** RF-09: albergues de la red que tienen cupo disponible. */
    public List<Albergue> consultarDisponibilidad() {
        List<Albergue> disponibles = new ArrayList<>();
        for (Albergue alb : albergues) {
            if (alb.tieneCupo()) {
                disponibles.add(alb);
            }
        }
        return disponibles;
    }

    /** RF-09: igual que el anterior pero excluyendo un albergue (útil para elegir un destino). */
    public List<Albergue> consultarDisponibilidad(String idExcluido) {
        List<Albergue> disponibles = consultarDisponibilidad();
        disponibles.removeIf(a -> a.getId().equalsIgnoreCase(idExcluido == null ? "" : idExcluido.trim()));
        return disponibles;
    }

    /** RF-09: consulta ocupación y recursos de CUALQUIER albergue de la red (con semáforo al día). */
    public Optional<Albergue> consultarAlbergue(String id) {
        Optional<Albergue> opt = buscarPorId(id);
        opt.ifPresent(this::actualizarSemaforo);
        return opt;
    }

    /** RF-05: devuelve toda la red con el semáforo recalculado (la tabla/panel lo dibuja). */
    public List<Albergue> obtenerPanelSemaforo() {
        actualizarTodosLosSemaforos();
        return getAlbergues();
    }

    /** RF-07: filtra los albergues según los criterios indicados. */
    public List<Albergue> filtrarAlbergues(FiltroAlbergue filtro) {
        actualizarTodosLosSemaforos();
        FiltroAlbergue f = (filtro == null) ? FiltroAlbergue.sinFiltros() : filtro;
        List<Albergue> resultado = new ArrayList<>();
        for (Albergue a : albergues) {
            if (f.cumple(a)) {
                resultado.add(a);
            }
        }
        return resultado;
    }

    /** Resumen de toda la red: alimenta el encabezado del centro de comando. */
    public record ResumenRed(int albergues, int ocupacion, int capacidad,
                             int verdes, int amarillos, int rojos, NivelSemaforo peorEstado) {
        public double porcentajeOcupacion() {
            return capacidad == 0 ? 0 : ocupacion * 100.0 / capacidad;
        }
    }

    public ResumenRed resumenRed() {
        actualizarTodosLosSemaforos();
        int ocupacion = 0, capacidad = 0, verdes = 0, amarillos = 0, rojos = 0;
        NivelSemaforo peor = NivelSemaforo.VERDE;
        for (Albergue a : albergues) {
            ocupacion += a.getOcupacionActual();
            capacidad += a.getCapacidadMaxima();
            switch (a.getEstadoSemaforo()) {
                case VERDE -> verdes++;
                case AMARILLO -> amarillos++;
                case ROJO -> rojos++;
            }
            peor = NivelSemaforo.peor(peor, a.getEstadoSemaforo());
        }
        return new ResumenRed(albergues.size(), ocupacion, capacidad, verdes, amarillos, rojos, peor);
    }

    // =========================================================
    // RECURSOS (RF-03)
    // =========================================================

    /**
     * RF-03: registra un recurso en el inventario de un albergue.
     * Devuelve false si el albergue no existe. Lanza IllegalArgumentException si el recurso ya
     * está vencido o si es AGUA en una unidad distinta de litros.
     */
    public boolean registrarRecurso(String idAlbergue, Recurso recurso) {
        if (recurso == null) {
            throw new IllegalArgumentException("El recurso no puede ser nulo.");
        }
        Optional<Albergue> opt = buscarPorId(idAlbergue);
        if (opt.isEmpty()) {
            return false;
        }
        if (recurso.estaVencido(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede registrar un recurso ya vencido.");
        }
        if (recurso.getTipo() == TipoRecurso.AGUA && !esUnidadLitros(recurso.getUnidad())) {
            throw new IllegalArgumentException("El agua debe registrarse en litros para validar el mínimo Sphere.");
        }
        opt.get().registrarRecurso(recurso);
        actualizarSemaforo(opt.get());
        return true;
    }

    /**
     * RF-03 (actualizar inventario): cambia cantidad y fecha de vencimiento de un recurso que
     * pertenece al albergue. Devuelve false si el albergue o el recurso no existen.
     */
    public boolean actualizarRecurso(String idAlbergue, Recurso recurso, double nuevaCantidad,
                                     LocalDate nuevaFechaVencimiento) {
        Optional<Albergue> opt = buscarPorId(idAlbergue);
        if (opt.isEmpty() || recurso == null || !opt.get().getRecursos().contains(recurso)) {
            return false;
        }
        recurso.actualizar(nuevaCantidad, nuevaFechaVencimiento); // valida que no sea negativa
        actualizarSemaforo(opt.get());
        return true;
    }

    private static boolean esUnidadLitros(String unidad) {
        if (unidad == null) {
            return false;
        }
        String u = unidad.trim().toLowerCase(Locale.ROOT);
        return u.equals("litros") || u.equals("litro") || u.equals("l") || u.equals("lt") || u.equals("lts");
    }

    // =========================================================
    // SEMÁFORO Y MÍNIMOS SPHERE (RF-05, RF-06)
    // =========================================================

    /** Capacidad máxima que permite el área del albergue según Sphere (3.5 m²/persona), o -1 si no hay área. */
    public int capacidadSegunEspacio(Albergue albergue) {
        if (albergue.getAreaM2() <= 0) {
            return -1;
        }
        return (int) Math.floor(albergue.getAreaM2() / SphereConstantes.ESPACIO_VITAL_M2_PERSONA_MIN);
    }

    /**
     * RF-06: valida los mínimos Sphere que el sistema puede medir con sus datos: agua (L/persona/día)
     * y espacio vital (m²/persona). Devuelve una lista de alertas (vacía si cumple todo).
     *
     * Interpretación del agua: se toma el agua VIGENTE almacenada y se divide entre los ocupantes;
     * equivale a "días de cobertura al mínimo Sphere" (15 L/persona/día = 1 día de cobertura).
     * Las demás constantes de SphereConstantes (grifos, inodoros, distancias...) requieren datos que
     * el sistema aún no captura, por eso no se evalúan aquí.
     */
    public List<Alerta> validarMinimosSphere(Albergue albergue) {
        List<Alerta> alertas = new ArrayList<>();
        int ocupantes = albergue.getOcupacionActual();

        // --- Agua ---
        if (ocupantes > 0) {
            double litrosPorPersona = albergue.getCantidadTotal(TipoRecurso.AGUA) / ocupantes;
            if (litrosPorPersona < SphereConstantes.AGUA_LITROS_PERSONA_DIA_EMERGENCIA) {
                alertas.add(new Alerta(NivelSemaforo.ROJO, "AGUA", String.format(Locale.US,
                        "Agua: %.1f L/persona, por debajo incluso del mínimo de emergencia (%.1f L/persona/día).",
                        litrosPorPersona, SphereConstantes.AGUA_LITROS_PERSONA_DIA_EMERGENCIA)));
            } else if (litrosPorPersona < SphereConstantes.AGUA_LITROS_PERSONA_DIA_MIN) {
                alertas.add(new Alerta(NivelSemaforo.AMARILLO, "AGUA", String.format(Locale.US,
                        "Agua: %.1f L/persona, bajo el mínimo Sphere (%.1f L/persona/día).",
                        litrosPorPersona, SphereConstantes.AGUA_LITROS_PERSONA_DIA_MIN)));
            }
        }

        // --- Espacio vital ---
        double area = albergue.getAreaM2();
        if (area > 0 && ocupantes > 0) {
            double m2PorPersona = area / ocupantes;
            double minimo = SphereConstantes.ESPACIO_VITAL_M2_PERSONA_MIN;
            if (m2PorPersona < minimo) {
                alertas.add(new Alerta(NivelSemaforo.ROJO, "ESPACIO", String.format(Locale.US,
                        "Hacinamiento: %.2f m²/persona, bajo el mínimo Sphere (%.1f m²/persona).",
                        m2PorPersona, minimo)));
            } else if (m2PorPersona < minimo * MARGEN_ADVERTENCIA) {
                alertas.add(new Alerta(NivelSemaforo.AMARILLO, "ESPACIO", String.format(Locale.US,
                        "Espacio cerca del mínimo: %.2f m²/persona (mínimo Sphere %.1f).", m2PorPersona, minimo)));
            }
        }

        // --- Capacidad declarada vs. área disponible ---
        int maxPorEspacio = capacidadSegunEspacio(albergue);
        if (maxPorEspacio >= 0 && albergue.getCapacidadMaxima() > maxPorEspacio) {
            alertas.add(new Alerta(NivelSemaforo.AMARILLO, "CAPACIDAD", String.format(Locale.US,
                    "La capacidad declarada (%d) supera lo que permite el área según Sphere (%d personas).",
                    albergue.getCapacidadMaxima(), maxPorEspacio)));
        }
        return alertas;
    }

    /** Alertas de inventario: recursos críticos sin stock vigente y lotes vencidos con stock. */
    public List<Alerta> evaluarRecursos(Albergue albergue) {
        List<Alerta> alertas = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        if (albergue.getOcupacionActual() > 0) {
            for (TipoRecurso tipo : TipoRecurso.values()) {
                // Un recurso crítico ausente también es una carencia; no se confunde
                // "sin registro" con "sin necesidad". El AGUA se omite aquí porque ya la evalúa
                // validarMinimosSphere en litros por persona (sin agua => alerta ROJA de Sphere).
                // Antes se recalculaban todas las alertas Sphere dentro del bucle para llegar al
                // mismo resultado.
                if (tipo.esCritico() && tipo != TipoRecurso.AGUA && !albergue.tieneRecursoDisponible(tipo)) {
                    alertas.add(new Alerta(NivelSemaforo.ROJO, "RECURSO_FALTANTE",
                            "Sin stock vigente del recurso crítico " + tipo + "."));
                }
            }
        }
        for (Recurso r : albergue.getRecursos()) {
            if (r.estaVencido(hoy) && r.getCantidadDisponible() > 0) {
                alertas.add(new Alerta(NivelSemaforo.AMARILLO, "RECURSO_VENCIDO",
                        "Lote vencido: " + r.getNombre() + " (" + r.getCantidadDisponible() + " " + r.getUnidad() + ")."));
            }
        }
        return alertas;
    }

    /** Todas las alertas de un albergue: ocupación + Sphere + inventario (en ese orden). */
    public List<Alerta> obtenerAlertas(Albergue albergue) {
        List<Alerta> alertas = new ArrayList<>();
        double pct = albergue.getPorcentajeOcupacion();
        if (pct >= UMBRAL_ROJO) {
            alertas.add(new Alerta(NivelSemaforo.ROJO, "OCUPACION", String.format(Locale.US,
                    "Ocupación al %.0f%% (umbral crítico: %.0f%%).", pct, UMBRAL_ROJO)));
        } else if (pct >= UMBRAL_AMARILLO) {
            alertas.add(new Alerta(NivelSemaforo.AMARILLO, "OCUPACION", String.format(Locale.US,
                    "Ocupación al %.0f%% (advertencia desde: %.0f%%).", pct, UMBRAL_AMARILLO)));
        }
        alertas.addAll(validarMinimosSphere(albergue));
        alertas.addAll(evaluarRecursos(albergue));
        return alertas;
    }

    /** El semáforo es la gravedad MÁS ALTA entre todas las alertas (sin alertas = VERDE). */
    public NivelSemaforo calcularSemaforo(Albergue albergue) {
        NivelSemaforo nivel = NivelSemaforo.VERDE;
        for (Alerta alerta : obtenerAlertas(albergue)) {
            nivel = NivelSemaforo.peor(nivel, alerta.gravedad());
        }
        return nivel;
    }

    public void actualizarSemaforo(Albergue albergue) {
        albergue.setEstadoSemaforo(calcularSemaforo(albergue));
    }

    public void actualizarTodosLosSemaforos() {
        for (Albergue a : albergues) {
            actualizarSemaforo(a);
        }
    }

    // =========================================================
    // PERSISTENCIA (usado por PersistenciaJson desde Main / Swing)
    // =========================================================

    /** Lista de albergues de solo lectura (sirve para la tabla de Swing y para guardar en JSON). */
    public List<Albergue> getAlbergues() {
        return Collections.unmodifiableList(albergues);
    }

    /**
     * Reemplaza todos los albergues por los cargados desde JSON. ANTES de reemplazar valida que no
     * haya IDs de albergue repetidos ni el mismo DNI en dos lugares; si algo falla lanza
     * IllegalArgumentException y NO modifica el estado actual.
     */
    public void reemplazarAlbergues(List<Albergue> nuevos) {
        if (nuevos == null) {
            throw new IllegalArgumentException("La lista de albergues no puede ser nula.");
        }
        List<Albergue> revisados = new ArrayList<>();
        for (Albergue a : nuevos) {
            validarPrefijoLibre(a, revisados);
            revisados.add(a);
        }
        Set<String> ids = new HashSet<>();
        Set<String> dnis = new HashSet<>();
        Set<String> nombres = new HashSet<>();
        Set<String> idsDamnificados = new HashSet<>();
        for (Albergue a : nuevos) {
            if (!ids.add(a.getId().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("ID de albergue repetido: " + a.getId());
            }
            for (Damnificado d : a.getDamnificados()) {
                if (!idsDamnificados.add(d.getId().toUpperCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("ID de damnificado repetido: " + d.getId());
                }
                if (!dnis.add(d.getDni())) {
                    throw new IllegalArgumentException("DNI repetido en la red: " + d.getDni());
                }
                if (!nombres.add(d.getNombreCompleto().toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Nombre de damnificado repetido: " + d.getNombreCompleto());
                }
            }
        }
        albergues.clear();
        albergues.addAll(nuevos);
        actualizarTodosLosSemaforos();
    }
}
