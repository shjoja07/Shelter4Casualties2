package com.shelter.controller;

import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.persistence.PersistenciaJson;
import com.shelter.service.AlbergueService;
import com.shelter.service.AlbergueService.ResultadoDerivacion;
import com.shelter.service.AlbergueService.ResultadoIngreso;
import com.shelter.service.AlbergueService.ResumenRed;
import com.shelter.service.AsignacionRecurso;
import com.shelter.service.FiltroAlbergue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controlador (la "C" de MVC) entre la interfaz y los servicios de negocio.
 *
 * Es el ÚNICO punto por el que la interfaz modifica datos. Cada operación que cambia el estado
 * avisa a los oyentes registrados, así todas las vistas (tablero, damnificados, recursos,
 * historial) se redibujan a partir del mismo estado y nunca quedan desincronizadas.
 *
 * No depende de Swing: la vista decide en qué hilo se redibuja. Los servicios no son seguros
 * para varios hilos, por lo que la interfaz llama siempre desde el hilo de eventos de Swing (EDT).
 */
public class ShelterController {

    /** Archivo donde se guarda y de donde se carga el estado del sistema. */
    public static final Path RUTA_DATOS = Path.of("datos_shelter.json");

    private final AlbergueService albergueService = new AlbergueService();
    private final AsignacionRecurso asignacion = new AsignacionRecurso(albergueService);
    private final List<Runnable> oyentes = new CopyOnWriteArrayList<>();

    // =========================================================
    // OYENTES (patrón observador)
    // =========================================================

    /** Registra una acción que se ejecuta cada vez que cambian los datos. */
    public void agregarOyente(Runnable oyente) {
        oyentes.add(oyente);
    }

    private void notificar() {
        for (Runnable oyente : oyentes) {
            oyente.run();
        }
    }

    // =========================================================
    // ARRANQUE Y PERSISTENCIA
    // =========================================================

    /**
     * Carga el archivo de datos si existe; si no existe o está dañado, crea los albergues de prueba.
     *
     * @return null si todo salió bien, o un aviso para mostrar al usuario
     */
    public String iniciar() {
        String aviso = null;
        if (Files.exists(RUTA_DATOS)) {
            try {
                cargar(RUTA_DATOS);
                return null;
            } catch (IOException | IllegalArgumentException e) {
                aviso = "No se pudo cargar " + RUTA_DATOS + " (" + e.getMessage() + ").\n"
                        + "Se iniciará con datos de prueba.";
            }
        }
        crearDatosDePrueba(albergueService);
        notificar();
        return aviso;
    }

    /** Albergues de prueba iniciales (los usan la consola y la interfaz gráfica). */
    public static void crearDatosDePrueba(AlbergueService servicio) {
        servicio.registrarAlbergue(new Albergue("A1", "Refugio Central", "Lima Centro", "Coliseo", 2, 14));
        servicio.registrarAlbergue(new Albergue("A2", "Refugio Norte", "Los Olivos", "Colegio", 5, 40));
    }

    public void guardar(Path ruta) throws IOException {
        PersistenciaJson.guardar(ruta, albergueService.getAlbergues(), asignacion.getHistorial());
    }

    /** Carga un archivo. Si es inválido lanza la excepción y NO modifica el estado actual. */
    public PersistenciaJson.Datos cargar(Path ruta) throws IOException {
        PersistenciaJson.Datos datos = PersistenciaJson.cargar(ruta);
        albergueService.reemplazarAlbergues(datos.albergues());
        asignacion.reemplazarHistorial(datos.historial());
        notificar();
        return datos;
    }

    // =========================================================
    // OPERACIONES QUE MODIFICAN EL ESTADO (todas notifican)
    // =========================================================

    public boolean registrarAlbergue(Albergue albergue) {
        boolean ok = albergueService.registrarAlbergue(albergue);
        if (ok) {
            notificar();
        }
        return ok;
    }

    public ResultadoIngreso registrarIngreso(String idAlbergue, Damnificado damnificado) {
        ResultadoIngreso r = albergueService.registrarIngreso(idAlbergue, damnificado);
        if (r == ResultadoIngreso.INGRESADO) {
            notificar();
        }
        return r;
    }

    public ResultadoDerivacion derivar(String idOrigen, String idDestino, String idDamnificado) {
        ResultadoDerivacion r = albergueService.derivarDamnificado(idOrigen, idDestino, idDamnificado);
        if (r == ResultadoDerivacion.DERIVADO) {
            notificar();
        }
        return r;
    }

    public boolean registrarSalida(String idAlbergue, String idDamnificado) {
        boolean ok = albergueService.registrarSalida(idAlbergue, idDamnificado);
        if (ok) {
            notificar();
        }
        return ok;
    }

    public void registrarVerificacion(DamnificadoVulnerable persona, boolean atendido, boolean estable) {
        persona.setAtendido(atendido);
        persona.setEstable(estable);
        notificar();
    }

    public boolean registrarRecurso(String idAlbergue, Recurso recurso) {
        boolean ok = albergueService.registrarRecurso(idAlbergue, recurso);
        if (ok) {
            notificar();
        }
        return ok;
    }

    public boolean actualizarRecurso(String idAlbergue, Recurso recurso, double cantidad, LocalDate vencimiento) {
        boolean ok = albergueService.actualizarRecurso(idAlbergue, recurso, cantidad, vencimiento);
        if (ok) {
            notificar();
        }
        return ok;
    }

    public List<RegistroAsignacion> repartir(Albergue albergue, Recurso recurso, double total, double porPersona) {
        List<RegistroAsignacion> entregas = asignacion.asignar(albergue, recurso, total, porPersona);
        notificar();
        return entregas;
    }

    // =========================================================
    // CONSULTAS (no modifican nada)
    // =========================================================

    public List<Albergue> getAlbergues() {
        return albergueService.obtenerPanelSemaforo();
    }

    public List<Albergue> filtrar(FiltroAlbergue filtro) {
        return albergueService.filtrarAlbergues(filtro);
    }

    public Optional<Albergue> buscarAlbergue(String id) {
        return albergueService.consultarAlbergue(id);
    }

    public List<Albergue> alberguesConCupo(String idExcluido) {
        return albergueService.consultarDisponibilidad(idExcluido);
    }

    public Optional<Albergue> ubicarPorDni(String dni) {
        return albergueService.ubicarDamnificadoPorDni(dni);
    }

    public List<Alerta> alertas(Albergue albergue) {
        return albergueService.obtenerAlertas(albergue);
    }

    public List<Alerta> minimosSphere(Albergue albergue) {
        return albergueService.validarMinimosSphere(albergue);
    }

    public ResumenRed resumenRed() {
        return albergueService.resumenRed();
    }

    public List<Damnificado> ordenPrioridad(Albergue albergue, Recurso recurso) {
        return asignacion.calcularOrdenPrioridad(albergue, recurso);
    }

    public double cantidadSugerida(Recurso recurso) {
        return asignacion.cantidadSugeridaPorPersona(recurso);
    }

    public List<RegistroAsignacion> historial() {
        return asignacion.getHistorial();
    }
}
