package com.shelter;

import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoGeneral;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.service.AlbergueService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            AlbergueService service = new AlbergueService();
            
            // Cargar albergues de prueba iniciales
            service.registrarAlbergue(new Albergue("A1", "Refugio Central", "Lima Centro", 2));
            service.registrarAlbergue(new Albergue("A2", "Refugio Norte", "Los Olivos", 5));
            
            boolean salir = false;
            
            while (!salir) {
                System.out.println("\n==========================================");
                System.out.println("   SISTEMA SHELTER 4 CASUALTIES - MENU   ");
                System.out.println("==========================================");
                System.out.println("1. Registrar nuevo ingreso de damnificado");
                System.out.println("2. Registrar nuevo albergue");
                System.out.println("3. Ver panel semaforo (Estado de albergues)");
                System.out.println("4. Salir");
                System.out.print("Seleccione una opcion: ");
                
                int opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar el búfer
                
                switch (opcion) {
                    case 1:
                        System.out.println("\n--- REGISTRAR DAMNIFICADO ---");
                        System.out.print("Ingrese DNI/ID: ");
                        String dni = scanner.nextLine();
                        
                        System.out.print("Ingrese Nombre Completo: ");
                        String nombre = scanner.nextLine();
                        
                        System.out.print("Ingrese Edad: ");
                        int edad = scanner.nextInt();
                        
                        System.out.print("¿Requiere atención medica inmediata? (true/false): ");
                        boolean requiereAtencion = scanner.nextBoolean();
                        scanner.nextLine(); // Limpiar búfer
                        
                        System.out.print("Ingrese el ID del albergue destino (ej: A1, A2): ");
                        String idAlbergue = scanner.nextLine();
                        
                        // Crear el tipo de damnificado según los datos ingresados
                        Damnificado nuevoDamnificado;
                        if (requiereAtencion) {
                            nuevoDamnificado = new DamnificadoVulnerable(dni, nombre, edad, true, false, false);
                        } else {
                            nuevoDamnificado = new DamnificadoGeneral(dni, nombre, edad);
                        }
                        
                        // Intentar ingreso
                        boolean exito = service.registrarIngreso(idAlbergue, nuevoDamnificado);
                        
                        // Si no hay cupo, ofrecer derivación
                        if (!exito) {
                            System.out.print("¿Desea intentar derivarlo a otro albergue disponible? (s/n): ");
                            String respuesta = scanner.nextLine();
                            if (respuesta.equalsIgnoreCase("s")) {
                                System.out.print("Ingrese ID del albergue de destino para derivacion: ");
                                String idDestino = scanner.nextLine();
                                service.derivarDamnificado(idAlbergue, idDestino, nuevoDamnificado);
                            }
                        }
                        break;
                        
                    case 2:
                        System.out.println("\n--- REGISTRAR NUEVO ALBERGUE ---");
                        System.out.print("Ingrese ID del Albergue: ");
                        String id = scanner.nextLine();
                        
                        System.out.print("Ingrese Nombre del Albergue: ");
                        String nombreAlbergue = scanner.nextLine();
                        
                        System.out.print("Ingrese Distrito: ");
                        String distrito = scanner.nextLine();
                        
                        System.out.print("Ingrese Capacidad Maxima: ");
                        int capacidad = scanner.nextInt();
                        scanner.nextLine(); // Limpiar búfer
                        
                        service.registrarAlbergue(new Albergue(id, nombreAlbergue, distrito, capacidad));
                        break;
                        
                    case 3:
                        service.mostrarPanelSemaforo();
                        break;
                        
                    case 4:
                        salir = true;
                        System.out.println("Saliendo del sistema...");
                        break;
                        
                    default:
                        System.out.println("Opcion no válida. Intente de nuevo.");
                }
            }
        }
    }
}