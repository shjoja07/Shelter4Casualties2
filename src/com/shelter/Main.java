package com.shelter;

import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoMenor;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;
import com.shelter.persistence.PersistenciaJson;
import com.shelter.service.AlbergueService;
import com.shelter.service.AlbergueService.ResultadoDerivacion;
import com.shelter.service.AlbergueService.ResultadoIngreso;
import com.shelter.service.AsignacionRecurso;
import com.shelter.service.FiltroAlbergue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interfaz de consola del sistema. SOLO presentación, ya que toda la lógica vive en los servicios,
 * que devuelven resultados (sin println).
 *
 *  - Toda la lectura pasa por métodos auxiliares que validan y repiten la pregunta (antes
 *    nextInt()/nextBoolean() directos tumbaban el programa con una letra).
 *  - Opciones de filtrar (RF-07), derivar y salida (RF-08),
 *    validar Sphere (RF-06), actualizar inventario (RF-03) y persistencia JSON.
 *  - Al iniciar se carga el archivo JSON si existe; al salir, se guarda.
 */
public class Main{
    /** Archivo donde se guarda y de donde se carga el estado del sistema. */
    private static final Path RUTA_DATOS = Path.of("datos_shelter.json");

    private static final Scanner scanner = new Scanner(System.in);
    private static final AlbergueService albergueService = new AlbergueService();
    private static final AsignacionRecurso asignacionService = new AsignacionRecurso(albergueService);

    public static void main(String[] args) {
        
        if (args.length == 0) {
        com.shelter.ui.VentanaPrincipal.main(args);
        return;
}
        iniciarDatos();

        boolean salir = false;
        try {
            while (!salir) {
                System.out.println("\n===========================================");
                System.out.println("     SISTEMA SHELTER 4 CASUALTIES          ");
                System.out.println("===========================================");
                System.out.println("1. Modulo de Albergues y Red");
                System.out.println("2. Modulo de Damnificados");
                System.out.println("3. Modulo de Recursos y Normas Sphere");
                System.out.println("4. Guardar / Cargar Datos (JSON)");
                System.out.println("0. Salir del Sistema");

                int opcion = leerEntero("Seleccione un modulo: ");

                switch (opcion) {
                    case 1 -> menuAlbergues();
                    case 2 -> menuDamnificados();
                    case 3 -> menuRecursos();
                    case 4 -> menuPersistencia();
                    case 0 -> {
                        salir = true;
                        System.out.println("Guardando y cerrando el sistema...");
                    }
                    default -> System.out.println("Opcion no valida.");
                }
            }
        } catch (NoSuchElementException e) {
            // La entrada se terminó (ej. se cerró la consola): se sale ordenadamente y se guarda.
            System.out.println("\nEntrada finalizada. Guardando y cerrando el sistema...");
        }
        guardarEn(RUTA_DATOS);
    }

    // =========================================================
    // INICIO: cargar datos guardados o crear albergues de prueba
    // =========================================================
    private static void iniciarDatos() {
        if (Files.exists(RUTA_DATOS)) {
            if (cargarDesde(RUTA_DATOS)) {
                return;
            }
            System.out.println("No se pudo usar el archivo guardado; se inicia con datos de prueba.");
        }
        // Albergues de prueba iniciales (ahora con tipo de infraestructura y área en m²)
        albergueService.registrarAlbergue(new Albergue("A1", "Refugio Central", "Lima Centro", "Coliseo", 2, 14));
        albergueService.registrarAlbergue(new Albergue("A2", "Refugio Norte", "Los Olivos", "Colegio", 5, 40));
    }

    // =========================================================
    // SUBMENÚ 1: ALBERGUES
    // =========================================================
    private static void menuAlbergues() {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n--- MODULO DE ALBERGUES ---");
            System.out.println("1. Registrar nuevo albergue (RF-01)");
            System.out.println("2. Ver Panel Semaforo de la red (RF-05)");
            System.out.println("3. Filtrar albergues (RF-07)");
            System.out.println("4. Consultar disponibilidad (RF-09)");
            System.out.println("5. Consultar un albergue en detalle (RF-09)");
            System.out.println("0. Volver al Menu Principal");

            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> registrarAlbergue();
                case 2 -> {
                    System.out.println("\n=== PANEL SEMAFORO DE RED DE ALBERGUES ===");
                    List<Albergue> red = albergueService.obtenerPanelSemaforo();
                    if (red.isEmpty()) {
                        System.out.println("No hay albergues registrados.");
                    }
                    for (Albergue a : red) {
                        imprimirAlbergueConAlertas(a);
                    }
                }
                case 3 -> filtrarAlbergues();
                case 4 -> {
                    System.out.println("\n--- ALBERGUES CON CUPO DISPONIBLE ---");
                    List<Albergue> disponibles = albergueService.consultarDisponibilidad();
                    if (disponibles.isEmpty()) {
                        System.out.println("No hay albergues con cupo disponible en este momento.");
                    } else {
                        disponibles.forEach(System.out::println);
                    }
                }
                case 5 -> consultarAlbergueDetalle();
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    private static void registrarAlbergue() {
        System.out.println("\n--- REGISTRAR NUEVO ALBERGUE ---");
        String id = leerTextoNoVacio("Ingrese ID del Albergue: ");
        if (albergueService.buscarPorId(id).isPresent()) {
            System.out.println("Ya existe un albergue con ese ID. No se registro.");
            return;
        }
        String nombreAlbergue = leerTextoNoVacio("Ingrese Nombre del Albergue: ");
        String distrito = leerTextoNoVacio("Ingrese Distrito: ");
        String tipo = leerTexto("Tipo de infraestructura (ej. Coliseo, Colegio, Carpa) [Enter = no especificada]: ");
        int capacidad = leerEnteroEnRango("Ingrese Capacidad Maxima: ", 1, 100000);
        double area = leerDecimalNoNegativo("Area total en m2 (0 si no la conoce): ");

        try {
            Albergue nuevo = new Albergue(id, nombreAlbergue, distrito, tipo, capacidad, area);
            albergueService.registrarAlbergue(nuevo);
            System.out.println("Albergue registrado: " + nuevo.getNombre());
            // Si la capacidad declarada no cabe en el área según Sphere, se avisa de inmediato
            for (Alerta alerta : albergueService.validarMinimosSphere(nuevo)) {
                System.out.println("   ! " + alerta);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo registrar el albergue: " + e.getMessage());
        }
    }

    private static void filtrarAlbergues() {
        System.out.println("\n--- FILTRAR ALBERGUES (Enter = no filtrar por ese criterio) ---");
        Integer cupos = null;
        String textoCupos = leerTexto("Cupos libres minimos: ");
        if (!textoCupos.isEmpty()) {
            try {
                cupos = Integer.parseInt(textoCupos);
            } catch (NumberFormatException e) {
                System.out.println("Valor no valido: se ignora este criterio.");
            }
        }
        String distrito = leerTexto("Distrito (o parte del nombre): ");

        NivelSemaforo nivel = null;
        String textoNivel = leerTexto("Nivel de alerta (VERDE / AMARILLO / ROJO): ").toUpperCase();
        if (!textoNivel.isEmpty()) {
            try {
                nivel = NivelSemaforo.valueOf(textoNivel);
            } catch (IllegalArgumentException e) {
                System.out.println("Nivel no valido: se ignora este criterio.");
            }
        }

        TipoRecurso faltante = null;
        String textoRecurso = leerTexto("Recurso critico faltante (AGUA / ALIMENTO / MEDICINA ...): ").toUpperCase();
        if (!textoRecurso.isEmpty()) {
            try {
                faltante = TipoRecurso.valueOf(textoRecurso);
            } catch (IllegalArgumentException e) {
                System.out.println("Tipo de recurso no valido: se ignora este criterio.");
            }
        }

        List<Albergue> resultado = albergueService.filtrarAlbergues(new FiltroAlbergue(cupos, distrito, nivel, faltante));
        System.out.println("\n" + resultado.size() + " albergue(s) cumplen los criterios:");
        resultado.forEach(Main::imprimirAlbergueConAlertas);
    }

    private static void consultarAlbergueDetalle() {
        String id = leerTextoNoVacio("Ingrese ID del Albergue a consultar: ");
        Optional<Albergue> opt = albergueService.consultarAlbergue(id);
        if (opt.isEmpty()) {
            System.out.println("Albergue no encontrado.");
            return;
        }
        Albergue a = opt.get();
        System.out.println("\n--- DETALLE DEL ALBERGUE ---");
        imprimirAlbergueConAlertas(a);
        System.out.println("   Infraestructura: " + a.getTipoInfraestructura()
                + " | Area: " + (a.getAreaM2() > 0 ? a.getAreaM2() + " m2" : "no registrada"));
        System.out.println("   Damnificados alojados (" + a.getDamnificados().size() + "):");
        for (Damnificado d : a.getDamnificados()) {
            System.out.println("     - " + d + " | " + d.determinarNivelPrioridad());
        }
        System.out.println("   Inventario (" + a.getRecursos().size() + "):");
        for (Recurso r : a.getRecursos()) {
            System.out.println("     - " + r);
        }
    }

    // =========================================================
    // SUBMENÚ 2: DAMNIFICADOS
    // =========================================================
    private static void menuDamnificados() {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n--- MODULO DE DAMNIFICADOS ---");
            System.out.println("1. Registrar ingreso de damnificado (RF-02)");
            System.out.println("2. Derivar damnificado a otro albergue (RF-08)");
            System.out.println("3. Registrar salida voluntaria (RF-08)");
            System.out.println("0. Volver al Menu Principal");

            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> registrarIngreso();
                case 2 -> derivarDamnificado();
                case 3 -> registrarSalida();
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    private static void registrarIngreso() {
        System.out.println("\n--- REGISTRO DE NUEVO DAMNIFICADO ---");
        // Se pide primero el albergue: si no existe, no se pierde el trabajo de escribir todos los datos
        String idAlbergue = leerTextoNoVacio("Ingrese el ID del Albergue asignado (ej. A1): ");
        if (albergueService.buscarPorId(idAlbergue).isEmpty()) {
            System.out.println("Albergue no encontrado.");
            return;
        }

        String dni = leerDni("Ingrese DNI (8 digitos): ");
        Optional<Albergue> yaAlojado = albergueService.ubicarDamnificado(dni);
        if (yaAlojado.isPresent()) {
            System.out.println("Ese DNI ya esta registrado en el albergue " + yaAlojado.get().getNombre() + ".");
            return;
        }
        String nombre = leerTextoNoVacio("Ingrese Nombre Completo: ");
        int edad = leerEnteroEnRango("Ingrese Edad: ", 0, Damnificado.EDAD_MAXIMA);
        boolean atencionMedica = leerSiNo("¿Requiere atencion medica inmediata? (s/n): ");

        Damnificado nuevoDamnificado;
        if (edad < Damnificado.EDAD_MAYORIA) {
            // Todo menor de 18 se registra como Menor (tutor opcional: sin tutor = no acompañado)
            System.out.println("Por ser menor de " + Damnificado.EDAD_MAYORIA + " años se registrara como Menor de edad.");
            String dniTutor = null;
            String nombreTutor = null;
            String parentesco = null;
            if (leerSiNo("¿Se encuentra con un tutor o apoderado? (s/n): ")) {
                dniTutor = leerDni("DNI del tutor (8 digitos): ");
                nombreTutor = leerTextoNoVacio("Nombre del tutor: ");
                parentesco = leerTextoNoVacio("Parentesco (ej. madre, abuelo): ");
            }
            nuevoDamnificado = new DamnificadoMenor(dni, nombre, edad, atencionMedica, dniTutor, nombreTutor, parentesco);
        } else {
            System.out.println("\nSeleccione el tipo de perfil:");
            System.out.println("1. General");
            System.out.println("2. Vulnerable (Gestante, Discapacidad, Enfermedad cronica, etc.)");
            int tipoPerfil = leerEnteroEnRango("Opcion: ", 1, 2);

            if (tipoPerfil == 2) {
                System.out.println("\n--- DETALLES DE VULNERABILIDAD ---");
                String condicion = leerTexto("Describa la condicion especial (ej. Discapacitado) [Enter = ninguna]: ");
                boolean esGestante = leerSiNo("¿Se encuentra en estado de gestacion? (s/n): ");
                boolean esCronica = leerSiNo("¿Padece alguna enfermedad cronica o critica? (s/n): ");
                nuevoDamnificado = new DamnificadoVulnerable(dni, nombre, edad, atencionMedica, condicion, esGestante, esCronica);
            } else {
                nuevoDamnificado = new DamnificadoGeneral(dni, nombre, edad, atencionMedica);
            }
        }

        // Invoca el registro en la capa de servicio y reacciona según el resultado
        ResultadoIngreso resultado = albergueService.registrarIngreso(idAlbergue, nuevoDamnificado);
        switch (resultado) {
            case INGRESADO -> imprimirIngreso(nuevoDamnificado, idAlbergue);
            case DNI_DUPLICADO -> System.out.println("Ese DNI ya esta registrado en la red.");
            case ALBERGUE_NO_ENCONTRADO -> System.out.println("Albergue no encontrado.");
            case SIN_CUPO -> ofrecerDerivacionPorFaltaDeCupo(nuevoDamnificado, idAlbergue);
        }
    }

    // Avance 2: si no se logró el registro, se ofrece intentar en otro albergue con cupo
    private static void ofrecerDerivacionPorFaltaDeCupo(Damnificado damnificado, String idLleno) {
        System.out.println("Albergue lleno. Buscando alternativas en la red...");
        while (true) {
            List<Albergue> alternativas = albergueService.consultarDisponibilidad(idLleno);
            if (alternativas.isEmpty()) {
                System.out.println("No hay otros albergues con cupo disponible. El damnificado no pudo ser registrado.");
                return;
            }
            alternativas.forEach(a -> System.out.println("  " + a));
            if (!leerSiNo("¿Desea registrarlo en otro albergue? (s/n): ")) {
                System.out.println("Registro cancelado.");
                return;
            }
            String idDestino = leerTextoNoVacio("ID del albergue destino: ");
            ResultadoIngreso r = albergueService.registrarIngreso(idDestino, damnificado);
            if (r == ResultadoIngreso.INGRESADO) {
                imprimirIngreso(damnificado, idDestino);
                return;
            }
            System.out.println(r == ResultadoIngreso.SIN_CUPO
                    ? "Ese albergue tampoco tiene cupo."
                    : "No se pudo registrar: " + r);
        }
    }

    private static void imprimirIngreso(Damnificado d, String idAlbergue) {
        String nombreAlbergue = albergueService.buscarPorId(idAlbergue).map(Albergue::getNombre).orElse(idAlbergue);
        System.out.println("- " + d.getNombreCompleto() + " (Prioridad " + d.getNivelPrioridad().getOrden()
                + ") ingreso a " + nombreAlbergue);
        System.out.println("  " + d.determinarNivelPrioridad());
    }

    private static void derivarDamnificado() {
        System.out.println("\n--- DERIVAR DAMNIFICADO A OTRO ALBERGUE ---");
        String origen = leerTextoNoVacio("ID del albergue de origen: ");
        String dni = leerDni("DNI del damnificado (8 digitos): ");
        List<Albergue> alternativas = albergueService.consultarDisponibilidad(origen);
        if (alternativas.isEmpty()) {
            System.out.println("No hay otros albergues con cupo disponible.");
            return;
        }
        System.out.println("Albergues con cupo disponible:");
        alternativas.forEach(a -> System.out.println("  " + a));
        String destino = leerTextoNoVacio("ID del albergue destino: ");

        ResultadoDerivacion r = albergueService.derivarDamnificado(origen, destino, dni);
        switch (r) {
            case DERIVADO -> System.out.println("Damnificado derivado con exito a "
                    + albergueService.buscarPorId(destino).map(Albergue::getNombre).orElse(destino) + ".");
            case ORIGEN_NO_ENCONTRADO -> System.out.println("El albergue de origen no existe.");
            case DESTINO_NO_ENCONTRADO -> System.out.println("El albergue de destino no existe.");
            case MISMO_ALBERGUE -> System.out.println("El origen y el destino son el mismo albergue.");
            case DAMNIFICADO_NO_ENCONTRADO -> System.out.println("Ese DNI no esta alojado en el albergue de origen.");
            case DESTINO_SIN_CUPO -> System.out.println("El albergue de destino no tiene cupo.");
        }
    }

    private static void registrarSalida() {
        System.out.println("\n--- REGISTRAR SALIDA VOLUNTARIA ---");
        String idAlbergue = leerTextoNoVacio("ID del albergue: ");
        String dni = leerDni("DNI del damnificado (8 digitos): ");
        if (albergueService.buscarPorId(idAlbergue).isEmpty()) {
            System.out.println("Albergue no encontrado.");
        } else if (albergueService.registrarSalida(idAlbergue, dni)) {
            System.out.println("Salida registrada. El cupo fue liberado.");
        } else {
            System.out.println("Ese DNI no esta alojado en el albergue indicado.");
        }
    }

    // =========================================================
    // SUBMENÚ 3: RECURSOS
    // =========================================================
    private static void menuRecursos() {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n--- MODULO DE RECURSOS Y SPHERE ---");
            System.out.println("1. Registrar recurso en inventario (RF-03)");
            System.out.println("2. Asignar recurso por prioridad (RF-04)");
            System.out.println("3. Validar minimos Sphere (RF-06)");
            System.out.println("4. Ver historial de asignaciones");
            System.out.println("5. Actualizar inventario existente (RF-03)");
            System.out.println("0. Volver al Menu Principal");

            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> registrarRecurso();
                case 2 -> asignarRecurso();
                case 3 -> validarSphere();
                case 4 -> {
                    System.out.println("\n--- HISTORIAL GENERAL DE ENTREGAS ---");
                    List<RegistroAsignacion> hist = asignacionService.getHistorial();
                    if (hist.isEmpty()) {
                        System.out.println("Aun no se han registrado entregas.");
                    } else {
                        hist.forEach(System.out::println);
                    }
                }
                case 5 -> actualizarInventario();
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    private static void registrarRecurso() {
        System.out.println("\n--- REGISTRAR RECURSO EN INVENTARIO ---");
        String idAlb = leerTextoNoVacio("Ingrese ID del Albergue (ej. A1, A2): ");
        if (albergueService.buscarPorId(idAlb).isEmpty()) {
            System.out.println("Albergue no encontrado.");
            return;
        }

        System.out.println("Tipos de recurso disponibles:");
        TipoRecurso[] tipos = TipoRecurso.values();
        for (int i = 0; i < tipos.length; i++) {
            System.out.println((i + 1) + ". " + tipos[i] + (tipos[i].esCritico() ? " (critico)" : ""));
        }
        TipoRecurso tipoSeleccionado = tipos[leerEnteroEnRango("Seleccione el tipo (1-" + tipos.length + "): ", 1, tipos.length) - 1];

        String nombre = leerTextoNoVacio("Nombre del recurso (ej. Agua embotellada, Arroz): ");
        String unidad;
        if (tipoSeleccionado == TipoRecurso.AGUA) {
            // El agua siempre en litros: es la unidad del minimo Sphere (L/persona/dia)
            unidad = AlbergueService.UNIDAD_AGUA;
            System.out.println("Unidad de medida: litros (el agua siempre se registra en litros).");
        } else {
            unidad = leerTextoNoVacio("Unidad de medida (ej. kg, cajas): ");
        }
        double cantidad = leerDecimalNoNegativo("Cantidad disponible: ");
        // Fecha mal escrita => se vuelve a preguntar (antes se guardaba como "no vence", peligroso en medicinas)
        LocalDate fechaVencimiento = leerFechaOpcional("Fecha de vencimiento (AAAA-MM-DD) o Enter si no vence: ");

        try {
            Recurso nuevoRecurso = new Recurso(tipoSeleccionado, nombre, unidad, cantidad, fechaVencimiento);
            if (albergueService.registrarRecurso(idAlb, nuevoRecurso)) {
                System.out.println("Recurso '" + nombre + "' (" + cantidad + " " + unidad + ") registrado en el albergue " + idAlb
                        + ". Si ya existia un lote igual, se sumo la cantidad.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error al registrar recurso: " + e.getMessage());
        }
    }

    private static void asignarRecurso() {
        System.out.println("\n--- ASIGNAR RECURSO POR PRIORIDAD ---");
        Albergue albergue = pedirAlbergueExistente("Ingrese ID del Albergue: ");
        if (albergue == null) {
            return;
        }
        if (albergue.getDamnificados().isEmpty()) {
            System.out.println("No hay damnificados alojados en este albergue para recibir recursos.");
            return;
        }
        Recurso recursoElegido = elegirRecurso(albergue, "Seleccione el recurso a repartir");
        if (recursoElegido == null) {
            return;
        }
        if (recursoElegido.estaVencido(LocalDate.now())) {
            System.out.println("No se puede repartir este recurso porque esta VENCIDO.");
            return;
        }

        double cantRepartir = leerDecimalPositivo("Cantidad total maxima a distribuir (Stock actual: "
                + recursoElegido.getCantidadDisponible() + " " + recursoElegido.getUnidad() + "): ");
        double sugerida = asignacionService.cantidadSugeridaPorPersona(recursoElegido);
        double porPersona = leerDecimalPositivoConDefecto("Cantidad por persona [Enter = " + sugerida + "]: ", sugerida);

        try {
            List<RegistroAsignacion> entregas = asignacionService.asignar(albergue, recursoElegido, cantRepartir, porPersona);
            System.out.println("\n Se realizaron " + entregas.size() + " entregas por orden de prioridad:");
            entregas.forEach(e -> System.out.println("  -> " + e));
            if (recursoElegido.getTipo() == TipoRecurso.MEDICINA && entregas.isEmpty()) {
                System.out.println("(La medicina solo se entrega a quien requiere atencion medica, es gestante o tiene enfermedad cronica.)");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("No se pudo asignar: " + e.getMessage());
        }
    }

    private static void validarSphere() {
        System.out.println("\n--- VALIDAR MINIMOS SPHERE (RF-06) ---");
        String id = leerTexto("ID del albergue [Enter = toda la red]: ");
        List<Albergue> aRevisar;
        if (id.isEmpty()) {
            aRevisar = albergueService.getAlbergues();
        } else {
            Optional<Albergue> opt = albergueService.buscarPorId(id);
            if (opt.isEmpty()) {
                System.out.println("Albergue no encontrado.");
                return;
            }
            aRevisar = List.of(opt.get());
        }
        for (Albergue a : aRevisar) {
            List<Alerta> alertas = albergueService.validarMinimosSphere(a);
            System.out.println("\n" + a.getNombre() + " [" + a.getId() + "]:");
            if (alertas.isEmpty()) {
                System.out.println("   Cumple los minimos Sphere evaluados (agua y espacio vital).");
            } else {
                alertas.forEach(al -> System.out.println("   ! " + al));
            }
        }
    }

    private static void actualizarInventario() {
        System.out.println("\n--- ACTUALIZAR INVENTARIO ---");
        Albergue albergue = pedirAlbergueExistente("Ingrese ID del Albergue: ");
        if (albergue == null) {
            return;
        }
        Recurso recurso = elegirRecurso(albergue, "Seleccione el recurso a actualizar");
        if (recurso == null) {
            return;
        }
        double nuevaCantidad = leerDecimalNoNegativo("Nueva cantidad disponible: ");
        System.out.println("Fecha de vencimiento actual: " + (recurso.getFechaVencimiento() == null ? "no vence" : recurso.getFechaVencimiento()));
        String texto = leerTexto("Nueva fecha (AAAA-MM-DD), 'n' si no vence, Enter para conservar: ");
        LocalDate nuevaFecha = recurso.getFechaVencimiento();
        if (texto.equalsIgnoreCase("n")) {
            nuevaFecha = null;
        } else if (!texto.isEmpty()) {
            try {
                nuevaFecha = LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                System.out.println("Formato de fecha invalido. Se conserva la fecha actual.");
            }
        }
        if (albergueService.actualizarRecurso(albergue.getId(), recurso, nuevaCantidad, nuevaFecha)) {
            System.out.println("Inventario actualizado: " + recurso);
        }
    }

    // =========================================================
    // SUBMENÚ 4: PERSISTENCIA
    // =========================================================
    private static void menuPersistencia() {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n--- PERSISTENCIA DE DATOS (JSON) ---");
            System.out.println("1. Guardar estado actual en JSON");
            System.out.println("2. Cargar datos desde JSON");
            System.out.println("0. Volver");
            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> guardarEn(pedirRuta());
                case 2 -> {
                    Path ruta = pedirRuta();
                    if (!Files.exists(ruta)) {
                        System.out.println("No existe el archivo: " + ruta);
                    } else if (leerSiNo("Esto REEMPLAZA los datos actuales del sistema. ¿Continuar? (s/n): ")) {
                        cargarDesde(ruta);
                    }
                }
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    private static Path pedirRuta() {
        String texto = leerTexto("Ruta del archivo [Enter = " + RUTA_DATOS + "]: ");
        return texto.isEmpty() ? RUTA_DATOS : Path.of(texto);
    }

    private static void guardarEn(Path ruta) {
        try {
            PersistenciaJson.guardar(ruta, albergueService.getAlbergues(), asignacionService.getHistorial());
            System.out.println("Datos guardados en " + ruta.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("No se pudo guardar: " + e.getMessage());
        }
    }

    /** Carga un JSON. Si el archivo es invalido NO se modifica nada del sistema. Devuelve true si cargo. */
    private static boolean cargarDesde(Path ruta) {
        try {
            PersistenciaJson.Datos datos = PersistenciaJson.cargar(ruta);
            albergueService.reemplazarAlbergues(datos.albergues());
            asignacionService.reemplazarHistorial(datos.historial());
            System.out.println("Datos cargados desde " + ruta.toAbsolutePath() + ": "
                    + datos.albergues().size() + " albergue(s), " + datos.historial().size() + " entrega(s) en el historial.");
            return true;
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("No se pudo cargar: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // IMPRESIÓN (solo presentación)
    // =========================================================
    private static void imprimirAlbergueConAlertas(Albergue a) {
        System.out.println("[" + a.getEstadoSemaforo().getEtiqueta() + "] " + a.getId() + " - " + a.getNombre()
                + " (" + a.getDistrito() + ")");
        System.out.println("   Ocupacion " + a.getOcupacionActual() + "/" + a.getCapacidadMaxima()
                + " " + barra(a.getPorcentajeOcupacion()) + String.format(" %.0f%%", a.getPorcentajeOcupacion()));
        for (Alerta alerta : albergueService.obtenerAlertas(a)) {
            System.out.println("   ! " + alerta);
        }
    }

    // Barra de progreso en texto, ej. [#####-----]
    private static String barra(double porcentaje) {
        int llenos = (int) Math.round(Math.min(100, Math.max(0, porcentaje)) / 10.0);
        return "[" + "#".repeat(llenos) + "-".repeat(10 - llenos) + "]";
    }

    private static Albergue pedirAlbergueExistente(String mensaje) {
        String id = leerTextoNoVacio(mensaje);
        Optional<Albergue> opt = albergueService.buscarPorId(id);
        if (opt.isEmpty()) {
            System.out.println("Albergue no encontrado.");
            return null;
        }
        return opt.get();
    }

    private static Recurso elegirRecurso(Albergue albergue, String mensaje) {
        List<Recurso> inventario = albergue.getRecursos();
        if (inventario.isEmpty()) {
            System.out.println("El albergue no tiene recursos en su inventario.");
            return null;
        }
        System.out.println("Recursos disponibles en " + albergue.getNombre() + ":");
        for (int i = 0; i < inventario.size(); i++) {
            System.out.println((i + 1) + ". " + inventario.get(i));
        }
        int recIndex = leerEnteroEnRango(mensaje + " (1-" + inventario.size() + "): ", 1, inventario.size()) - 1;
        return inventario.get(recIndex);
    }

    // =========================================================
    // MÉTODOS AUXILIARES DE LECTURA (Cumplen con el UML)
    // Todos leen una línea completa y repiten la pregunta si el dato no es válido.
    // Los números decimales aceptan coma o punto, sin depender del idioma del sistema.
    // =========================================================
    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim(); // si no hay más entrada lanza NoSuchElementException (se atrapa en main)
    }

    private static String leerTextoNoVacio(String mensaje) {
        String texto = leerTexto(mensaje);
        while (texto.isEmpty()) {
            texto = leerTexto("Este dato es obligatorio. " + mensaje);
        }
        return texto;
    }

    private static int leerEntero(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.print("Ingrese un numero valido. ");
            }
        }
    }

    private static int leerEnteroEnRango(String mensaje, int min, int max) {
        while (true) {
            int n = leerEntero(mensaje);
            if (n >= min && n <= max) {
                return n;
            }
            System.out.println("El valor debe estar entre " + min + " y " + max + ".");
        }
    }

    private static double leerDecimal(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje).replace(',', '.');
            try {
                double d = Double.parseDouble(texto);
                if (!Double.isNaN(d) && !Double.isInfinite(d)) { // Double.parseDouble acepta "NaN" e "Infinity"
                    return d;
                }
            } catch (NumberFormatException e) {
                // cae al mensaje de abajo
            }
            System.out.print("Ingrese un numero decimal valido. ");
        }
    }

    private static double leerDecimalNoNegativo(String mensaje) {
        while (true) {
            double d = leerDecimal(mensaje);
            if (d >= 0) {
                return d;
            }
            System.out.println("El valor no puede ser negativo.");
        }
    }

    private static double leerDecimalPositivo(String mensaje) {
        while (true) {
            double d = leerDecimal(mensaje);
            if (d > 0) {
                return d;
            }
            System.out.println("El valor debe ser mayor que cero.");
        }
    }

    private static double leerDecimalPositivoConDefecto(String mensaje, double defecto) {
        while (true) {
            String texto = leerTexto(mensaje);
            if (texto.isEmpty()) {
                return defecto;
            }
            try {
                double d = Double.parseDouble(texto.replace(',', '.'));
                if (d > 0 && !Double.isInfinite(d)) {
                    return d;
                }
            } catch (NumberFormatException e) {
                // cae al mensaje de abajo
            }
            System.out.println("Ingrese un numero mayor que cero o presione Enter.");
        }
    }

    private static boolean leerSiNo(String mensaje) {
        while (true) {
            String t = leerTexto(mensaje).toLowerCase();
            if (t.equals("s") || t.equals("si") || t.equals("sí") || t.equals("true")) {
                return true;
            }
            if (t.equals("n") || t.equals("no") || t.equals("false")) {
                return false;
            }
            System.out.print("Responda con s o n. ");
        }
    }

    private static String leerDni(String mensaje) {
        while (true) {
            String dni = leerTexto(mensaje);
            if (Damnificado.esDniValido(dni)) {
                return dni;
            }
            System.out.print("El DNI debe tener exactamente " + Damnificado.LONGITUD_DNI + " digitos. ");
        }
    }

    private static LocalDate leerFechaOpcional(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(texto);
            } catch (DateTimeParseException e) {
                System.out.print("Formato invalido (use AAAA-MM-DD). ");
            }
        }
    }
}
