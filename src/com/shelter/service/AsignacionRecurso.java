package com.shelter.service;

import com.shelter.config.SphereConstantes;
import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reparto de recursos escasos entre los damnificados de un albergue (RF-04).
 *
 * Cada entrega es min(cantidad por persona, cantidad restante, stock), asi que no falla ni deja stock negativo.
 *  - El orden es (1) nivel de prioridad, (2) quien lleva más tiempo sin recibir ese recurso.
 *    Las fechas se obtienen del historial guardado en JSON.
 *  - La MEDICINA solo se reparte a quien la necesita.
 *  - Para el AGUA la cantidad sugerida por persona es el mínimo Sphere (15 L/persona/día).
 *  - Se valida que el recurso pertenezca al albergue, no esté vencido y que las cantidades sean válidas.
 *  - No imprime nada, solo se devuelven los registros de entrega.
 */
public class AsignacionRecurso {

    /** Cantidad por persona para recursos que no tienen una referencia Sphere. */
    public static final double CANTIDAD_POR_PERSONA_DEFECTO = 1.0;
    // Tolerancia para comparar decimales (los double acumulan errores diminutos).
    private static final double EPSILON = 1e-9;

    private final AlbergueService albergueService;

    //Uso de listas
    private final List<RegistroAsignacion> historial;

    public AsignacionRecurso(AlbergueService albergueService) {
        this.albergueService = albergueService;
        this.historial = new ArrayList<>();
    }

    /** Cantidad sugerida por persona: 15 L para el agua (mínimo Sphere), 1.0 para el resto. */
    public double cantidadSugeridaPorPersona(Recurso recurso) {
        return recurso.getTipo() == TipoRecurso.AGUA
                ? SphereConstantes.AGUA_LITROS_PERSONA_DIA_MIN
                : CANTIDAD_POR_PERSONA_DEFECTO;
    }

    // RF-04: Calcular orden de prioridad (solo por nivel; el desempate es el orden de llegada)
    public List<Damnificado> calcularOrdenPrioridad(Albergue albergue) {
        return calcularOrdenPrioridad(albergue, null);
    }

    /**
     * RF-04: orden de prioridad para repartir un recurso concreto. Nivel de prioridad primero;
     * a igual nivel, va antes quien lleva más tiempo sin recibir ese recurso (o nunca lo recibió).
     * Se ordena una COPIA para no alterar la lista original del albergue.
     */
    public List<Damnificado> calcularOrdenPrioridad(Albergue albergue, Recurso recurso) {
        List<Damnificado> priorizados = new ArrayList<>(albergue.getDamnificados());
        Map<String, LocalDateTime> ultimasEntregas = (recurso == null)
                ? Collections.emptyMap()
                : ultimasEntregas(albergue.getId(), recurso.getNombre());

        // List.sort es ESTABLE: a igualdad total se conserva el orden de llegada al albergue.
        priorizados.sort(Comparator
                .comparingInt((Damnificado d) -> albergueService.calcularPrioridadAsignacion(d))
                .thenComparing(d -> ultimasEntregas.getOrDefault(d.getId(), LocalDateTime.MIN)));
        return priorizados;
    }

    // Fecha de la última entrega por ID ordenado (el DNI queda solo como dato histórico).
    private Map<String, LocalDateTime> ultimasEntregas(String idAlbergue, String nombreRecurso) {
        Map<String, LocalDateTime> mapa = new HashMap<>();
        for (RegistroAsignacion r : historial) {
            if (r.getIdAlbergue().equalsIgnoreCase(idAlbergue) && r.getRecurso().equalsIgnoreCase(nombreRecurso)) {
                mapa.merge(r.getIdDamnificado(), r.getFechaHora(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return mapa;
    }

    // RF-04: Asignar recursos (cantidad por persona sugerida según el tipo de recurso)
    public List<RegistroAsignacion> asignar(Albergue albergue, Recurso recurso, double cantidadTotalADistribuir) {
        return asignar(albergue, recurso, cantidadTotalADistribuir, cantidadSugeridaPorPersona(recurso));
    }

    /**
     * RF-04: reparte hasta cantidadTotalADistribuir del recurso, entregando como máximo
     * cantidadPorPersona a cada damnificado, en orden de prioridad, y sin pasar del stock.
     *
     * @throws IllegalArgumentException si los datos no son válidos (recurso ajeno, vencido, cantidades <= 0)
     */
    public List<RegistroAsignacion> asignar(Albergue albergue, Recurso recurso,
                                            double cantidadTotalADistribuir, double cantidadPorPersona) {
        List<EntregaPlanificada> plan = previsualizar(albergue, recurso, cantidadTotalADistribuir, cantidadPorPersona);
        List<RegistroAsignacion> registrosNuevos = new ArrayList<>();
        for (EntregaPlanificada e : plan) {
            recurso.descontar(e.cantidad()); // el plan nunca supera el stock: no puede fallar
            // Constancia de entrega (RNF-06: marca de tiempo)
            RegistroAsignacion registro = new RegistroAsignacion(albergue.getId(), e.damnificado().getId(),
                    e.damnificado().getDni(), e.damnificado().getNombreCompleto(),
                    recurso.getNombre(), recurso.getUnidad(), e.cantidad());
            this.historial.add(registro);
            registrosNuevos.add(registro);
        }
        ultimoReparto = registrosNuevos.isEmpty() ? null : new RepartoDeshacible(albergue, recurso, registrosNuevos);
        // El inventario cambió: el semáforo del albergue puede cambiar (ej. se acabó el agua)
        albergueService.actualizarSemaforo(albergue);
        return registrosNuevos;
    }

    /** Una entrega que el reparto haría: a quién y cuánto. */
    public record EntregaPlanificada(Damnificado damnificado, double cantidad) {
    }

    /**
     * Calcula el reparto SIN modificar nada: sirve para mostrar un resumen y pedir confirmación
     * antes de descontar stock. asignar() usa exactamente este mismo plan, así la vista previa y
     * el reparto real nunca difieren.
     */
    public List<EntregaPlanificada> previsualizar(Albergue albergue, Recurso recurso,
                                                  double cantidadTotalADistribuir, double cantidadPorPersona) {
        if (albergue == null || recurso == null) {
            throw new IllegalArgumentException("Debe indicar el albergue y el recurso a repartir.");
        }
        validarCantidad(cantidadTotalADistribuir, "La cantidad a distribuir");
        validarCantidad(cantidadPorPersona, "La cantidad por persona");
        if (!albergue.getRecursos().contains(recurso)) {
            throw new IllegalArgumentException("El recurso no pertenece al inventario de este albergue.");
        }
        if (recurso.estaVencido(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede repartir " + recurso.getNombre() + ": está VENCIDO.");
        }
        List<EntregaPlanificada> plan = new ArrayList<>();
        double restante = cantidadTotalADistribuir;
        double stock = recurso.getCantidadDisponible();
        // Lista ya ordenada por prioridad (y por tiempo desde la última entrega de ese recurso)
        for (Damnificado d : calcularOrdenPrioridad(albergue, recurso)) {
            if (restante <= EPSILON || stock <= EPSILON) {
                break;
            }
            // La medicina solo es para quien la necesita
            if (recurso.getTipo() == TipoRecurso.MEDICINA && !d.necesitaMedicina()) {
                continue;
            }
            // La entrega nunca supera lo que queda por repartir NI el stock real
            double cantidad = Math.min(cantidadPorPersona, Math.min(restante, stock));
            plan.add(new EntregaPlanificada(d, cantidad));
            restante -= cantidad;
            stock -= cantidad;
        }
        return plan;
    }

    // ----- Deshacer el último reparto -----

    private record RepartoDeshacible(Albergue albergue, Recurso recurso, List<RegistroAsignacion> registros) {
    }

    private RepartoDeshacible ultimoReparto;

    /** ¿Hay un reparto de esta sesión que todavía se pueda deshacer? */
    public boolean puedeDeshacerUltimoReparto() {
        return ultimoReparto != null && ultimoReparto.albergue().getRecursos().contains(ultimoReparto.recurso())
                && historial.containsAll(ultimoReparto.registros());
    }

    /**
     * Deshace el último reparto: devuelve la cantidad al lote y retira sus constancias del historial.
     * Solo vale para el reparto más reciente de esta sesión y mientras el lote siga en el albergue.
     *
     * @return la cantidad devuelta al stock
     */
    public double deshacerUltimoReparto() {
        if (!puedeDeshacerUltimoReparto()) {
            throw new IllegalArgumentException("No hay un reparto reciente que se pueda deshacer.");
        }
        RepartoDeshacible r = ultimoReparto;
        double devuelto = 0;
        for (RegistroAsignacion reg : r.registros()) {
            devuelto += reg.getCantidad();
        }
        r.recurso().actualizar(r.recurso().getCantidadDisponible() + devuelto, r.recurso().getFechaVencimiento());
        historial.removeAll(r.registros());
        ultimoReparto = null;
        albergueService.actualizarSemaforo(r.albergue());
        return devuelto;
    }

    private static void validarCantidad(double cantidad, String nombre) {
        if (Double.isNaN(cantidad) || Double.isInfinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException(nombre + " debe ser un número mayor que cero.");
        }
    }

    // Getter (solo lectura: el historial solo crece mediante asignar())
    public List<RegistroAsignacion> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    // Persistencia: reemplaza el historial por el cargado desde JSON
    public void reemplazarHistorial(List<RegistroAsignacion> nuevo) {
        historial.clear();
        historial.addAll(nuevo);
        ultimoReparto = null; // tras cargar otro estado ya no hay nada que deshacer
    }
}
