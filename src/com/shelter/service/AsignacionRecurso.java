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
                .thenComparing(d -> ultimasEntregas.getOrDefault(d.getDni(), LocalDateTime.MIN)));
        return priorizados;
    }

    // Fecha de la última entrega de ese recurso en ese albergue, por DNI (según el historial).
    private Map<String, LocalDateTime> ultimasEntregas(String idAlbergue, String nombreRecurso) {
        Map<String, LocalDateTime> mapa = new HashMap<>();
        for (RegistroAsignacion r : historial) {
            if (r.getIdAlbergue().equalsIgnoreCase(idAlbergue) && r.getRecurso().equalsIgnoreCase(nombreRecurso)) {
                mapa.merge(r.getDni(), r.getFechaHora(), (a, b) -> a.isAfter(b) ? a : b);
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
        validarCantidad(cantidadTotalADistribuir, "La cantidad a distribuir");
        validarCantidad(cantidadPorPersona, "La cantidad por persona");
        if (!albergue.getRecursos().contains(recurso)) {
            throw new IllegalArgumentException("El recurso no pertenece al inventario de este albergue.");
        }
        if (recurso.estaVencido(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede repartir " + recurso.getNombre() + ": está VENCIDO.");
        }

        List<RegistroAsignacion> registrosNuevos = new ArrayList<>();

        // 1. Lista ya ordenada por prioridad (y por tiempo desde la última entrega)
        List<Damnificado> priorizados = calcularOrdenPrioridad(albergue, recurso);

        double cantidadRestante = cantidadTotalADistribuir;

        // 2. Recorremos la lista ordenada para repartir
        for (Damnificado d : priorizados) {
            // Si ya no queda recurso para repartir o el inventario está en cero, terminamos el bucle
            if (cantidadRestante <= EPSILON || recurso.getCantidadDisponible() <= EPSILON) {
                break;
            }
            // La medicina solo es para quien la necesita
            if (recurso.getTipo() == TipoRecurso.MEDICINA && !d.necesitaMedicina()) {
                continue;
            }

            // CORRECCIÓN: la entrega nunca supera lo que queda por repartir NI el stock real.
            double cantidadEntregada = Math.min(cantidadPorPersona,
                    Math.min(cantidadRestante, recurso.getCantidadDisponible()));

            recurso.descontar(cantidadEntregada); // ya no puede lanzar "stock insuficiente"
            cantidadRestante -= cantidadEntregada;

            // Creamos la constancia de entrega (RNF-06: marca de tiempo)
            RegistroAsignacion registro = new RegistroAsignacion(
                albergue.getId(),
                d.getDni(),
                d.getNombreCompleto(),
                recurso.getNombre(),
                recurso.getUnidad(),
                cantidadEntregada
            );

            // 3. Agregamos el registro a nuestra lista del historial (.add)
            this.historial.add(registro);
            registrosNuevos.add(registro);
        }

        // El inventario cambió: el semáforo del albergue puede cambiar (ej. se acabó el agua)
        albergueService.actualizarSemaforo(albergue);
        return registrosNuevos;
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
    }
}
