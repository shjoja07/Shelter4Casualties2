package com.shelter;

import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;
import com.shelter.model.TipoRecurso;
import com.shelter.service.AlbergueService;
import com.shelter.service.AsignacionRecurso;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main{
    private static final Scanner scanner = new Scanner(System.in);
    private static final AlbergueService albergueService = new AlbergueService();
    private static final AsignacionRecurso asignacionService = new AsignacionRecurso(albergueService);

    public static void main(String[] args) {

        // Albergues de prueba iniciales
        albergueService.registrarAlbergue(new Albergue("A1", "Refugio Central", "Lima Centro", 2));
        albergueService.registrarAlbergue(new Albergue("A2", "Refugio Norte", "Los Olivos", 5));
        
        boolean salir = false;
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
            System.out.println("0. Volver al Menu Principal");
            
            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> {       
                        System.out.println("\n--- REGISTRAR NUEVO ALBERGUE ---");
                        String id = leerTexto("Ingrese ID del Albergue: ");
                        
                        String nombreAlbergue = leerTexto("Ingrese Nombre del Albergue: "); 
                        
                        String distrito = leerTexto("Ingrese Distrito: ");
                        
                        System.out.print("Ingrese Capacidad Maxima: ");
                        int capacidad = leerEntero("Ingrese Capacidad Maxima: ");
                        scanner.nextLine(); // Limpiar búfer
                        
                        albergueService.registrarAlbergue(new Albergue(id, nombreAlbergue, distrito, capacidad));
                    }
                case 2 -> albergueService.mostrarPanelSemaforo();
                case 3 -> { /* Lógica filtrar */ }
                case 4 -> { 
                    System.out.println("\n--- ALBERGUES CON CUPO DISPONIBLE ---");
                    List<Albergue> disponibles = albergueService.consultarDisponibilidad();
                    if (disponibles.isEmpty()) {
                    System.out.println("No hay albergues con cupo disponible en este momento.");
                    } else {
                        disponibles.forEach(System.out::println);
                    }
                }
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    // =========================================================
    // SUBMENÚ 2: DAMNIFICADOS
    // =========================================================
    private static void menuDamnificados() {
        boolean volver = false;
        while (!volver) {
            System.out.println("\n--- 👥 MODULO DE DAMNIFICADOS ---");
            System.out.println("1. Registrar ingreso de damnificado (RF-02)");
            System.out.println("2. Derivar damnificado a otro albergue (RF-08)");
            System.out.println("3. Registrar salida voluntaria (RF-08)");
            System.out.println("0. Volver al Menu Principal");
            
            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> { 
                        System.out.println("\n--- REGISTRAR DAMNIFICADO ---");
                        String dni = leerTexto("Ingrese DNI/ID: ");
                        
                        String nombre = leerTexto("Ingrese Nombre Completo: "); 
                        
                        int edad = leerEntero("Ingrese Edad: "); 
                        
                        System.out.print("¿Requiere atención medica inmediata? (true/false): ");
                        boolean requiereAtencion = scanner.nextBoolean();
                        scanner.nextLine(); // Limpiar búfer
                    
                        String idAlbergue = leerTexto("Ingrese el ID del albergue destino (ej: A1, A2): "); 
                        
                        // Crear el tipo de damnificado según los datos ingresados
                        Damnificado nuevoDamnificado;
                        if (requiereAtencion) {
                            nuevoDamnificado = new DamnificadoVulnerable(dni, nombre, edad, true, false, false);
                        } else {
                            nuevoDamnificado = new DamnificadoGeneral(dni, nombre, edad);
                        }
                        
                        // Intentar ingreso
                        boolean exito = albergueService.registrarIngreso(idAlbergue, nuevoDamnificado);
                        
                        // Si no hay cupo, ofrecer derivación
                        if (!exito) {
                            String respuesta = leerTexto("¿Desea intentar derivarlo a otro albergue disponible? (s/n): ");
                            if (respuesta.equalsIgnoreCase("s")) {
                                String idDestino = leerTexto("Ingrese ID del albergue de destino para derivacion: ");
                                albergueService.derivarDamnificado(idAlbergue, idDestino, nuevoDamnificado);
                            }
                        }
                    }
                case 2 -> { /* Lógica derivar */ }
                case 3 -> { /* Lógica salida */ }
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
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
            System.out.println("0. Volver al Menu Principal");
            
            int op = leerEntero("Seleccione una opcion: ");
            switch (op) {
                case 1 -> {
                    System.out.println("\n--- REGISTRAR RECURSO EN INVENTARIO ---");
                    String idAlb = leerTexto("Ingrese ID del Albergue (ej. A1, A2): ");
                    
                    System.out.println("Tipos de recurso disponibles:");
                    TipoRecurso[] tipos = TipoRecurso.values();
                    for (int i = 0; i < tipos.length; i++) {
                        System.out.println((i + 1) + ". " + tipos[i]);
                    }
                    int tipoIndex = leerEntero("Seleccione el tipo (1-" + tipos.length + "): ") - 1;
                    if (tipoIndex < 0 || tipoIndex >= tipos.length) {
                        System.out.println("Tipo invalido.");
                        break;
                    }
                    TipoRecurso tipoSeleccionado = tipos[tipoIndex];
                    
                    String nombre = leerTexto("Nombre del recurso (ej. Agua embotellada, Arroz): ");
                    String unidad = leerTexto("Unidad de medida (ej. litros, kg, cajas): ");
                    double cantidad = leerDecimal("Cantidad disponible: ");
                    
                    String fechaStr = leerTexto("Fecha de vencimiento (AAAA-MM-DD) o presione Enter si no vence: ");
                    LocalDate fechaVencimiento = null;
                    if (!fechaStr.isBlank()) {
                        try {
                            fechaVencimiento = LocalDate.parse(fechaStr);
                        } catch (DateTimeParseException e) {
                            System.out.println("Formato de fecha invalido. Se registrara como 'no vence'.");
                        }
                    }
                    
                    try {
                        Recurso nuevoRecurso = new Recurso(tipoSeleccionado, nombre, unidad, cantidad, fechaVencimiento);
                        albergueService.registrarRecurso(idAlb, nuevoRecurso);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Error al crear recurso: " + e.getMessage());
                    }
                }
                
                case 2 -> {
                    System.out.println("\n--- ASIGNAR RECURSO POR PRIORIDAD ---");
                    String idAlb = leerTexto("Ingrese ID del Albergue: ");
                    Optional<Albergue> optAlb = albergueService.buscarPorId(idAlb);
                    
                    if (optAlb.isEmpty()) {
                        System.out.println("Albergue no encontrado.");
                        break;
                    }
                    Albergue albergue = optAlb.get();
                    
                    if (albergue.getDamnificados().isEmpty()) {
                        System.out.println("No hay damnificados alojados en este albergue para recibir recursos.");
                        break;
                    }
                    
                    List<Recurso> inventario = albergue.getRecursos();
                    if (inventario.isEmpty()) {
                        System.out.println("El albergue no tiene recursos en su inventario.");
                        break;
                    }
                    
                    System.out.println("Recursos disponibles en " + albergue.getNombre() + ":");
                    for (int i = 0; i < inventario.size(); i++) {
                        System.out.println((i + 1) + ". " + inventario.get(i));
                    }
                    
                    int recIndex = leerEntero("Seleccione el recurso a repartir (1-" + inventario.size() + "): ") - 1;
                    if (recIndex < 0 || recIndex >= inventario.size()) {
                        System.out.println("Seleccion invalida.");
                        break;
                    }
                    Recurso recursoElegido = inventario.get(recIndex);
                    
                    if (recursoElegido.estaVencido(LocalDate.now())) {
                        System.out.println("No se puede repartir este recurso porque esta VENCIDO.");
                        break;
                    }
                    
                    double cantRepartir = leerDecimal("Cantidad total máxima a distribuir (Stock actual: " + recursoElegido.getCantidadDisponible() + "): ");
                    
                    List<RegistroAsignacion> entregas = asignacionService.asignar(albergue, recursoElegido, cantRepartir);
                    
                    System.out.println("\n Se realizaron " + entregas.size() + " entregas por orden de prioridad:");
                    entregas.forEach(e -> System.out.println("  -> " + e));
                }
                case 3 -> { System.out.println("Validacion Sphere pendiente de implementacion."); }
                case 4 -> {
                    System.out.println("\n--- HISTORIAL GENERAL DE ENTREGAS ---");
                    List<RegistroAsignacion> hist = asignacionService.getHistorial();
                    if (hist.isEmpty()) {
                        System.out.println("Aun no se han registrado entregas.");
                    } else {
                        hist.forEach(System.out::println);
                    }
                }
                case 0 -> volver = true;
                default -> System.out.println("Opcion no valida.");
            }
        }
    }

    // =========================================================
    // SUBMENÚ 4: PERSISTENCIA (SEMANA 12)
    // =========================================================
    private static void menuPersistencia() {
        System.out.println("\n--- PERSISTENCIA DE DATOS (JSON) ---");
        System.out.println("1. Guardar estado actual en JSON");
        System.out.println("2. Cargar datos desde JSON");
        System.out.println("0. Volver");
        // ...
    }

    // =========================================================
    // MÉTODOS AUXILIARES DE LECTURA (Cumplen con el UML)
    // =========================================================
    private static int leerEntero(String mensaje) {
        System.out.print(mensaje);
        while (!scanner.hasNextInt()) {
            System.out.print("Ingrese un numero valido: ");
            scanner.next();
        }
        int num = scanner.nextInt();
        scanner.nextLine(); // Limpiar buffer
        return num;
    }
    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    private static double leerDecimal(String mensaje) {
        System.out.print(mensaje);
        while (!scanner.hasNextDouble()) {
            System.out.print("Ingrese un numero decimal valido: ");
            scanner.next();
        }
        double num = scanner.nextDouble();
        scanner.nextLine(); // Limpiar buffer
        return num;
    }

}