package com.shelter.service;

import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlbergueService {
    private final List<Albergue> albergues = new ArrayList<>();

    // RF-01: Registrar nuevo albergue
    public void registrarAlbergue(Albergue albergue) {
        albergues.add(albergue);
        System.out.println("✅ Albergue registrado: " + albergue.getNombre());
    }

    // <include> Calcular prioridad de asignación
    public int calcularPrioridadAsignacion(Damnificado damnificado) {
        if (damnificado.isRequiereAtencionMedica() || damnificado.getEdad() >= 60 || damnificado.getEdad() <= 5) {
            return 1; // Alta prioridad
        }
        return 2; // Prioridad normal
    }

    // RF-02: Registrar ingreso de damnificado (incluye calcular prioridad)
    public boolean registrarIngreso(String idAlbergue, Damnificado damnificado) {
        Optional<Albergue> optAlbergue = buscarPorId(idAlbergue);

        if (optAlbergue.isPresent()) {
            Albergue albergue = optAlbergue.get();

            // <include> Calcular prioridad
            int prioridad = calcularPrioridadAsignacion(damnificado);
            damnificado.setNivelPrioridad(prioridad);

            if (albergue.tieneCupo()) {
                albergue.setOcupacionActual(albergue.getOcupacionActual() + 1);
                System.out.println("✅ " + damnificado.getNombre() + " (Prioridad " + prioridad + 
                                   ") ingresó a " + albergue.getNombre());
                return true;
            } else {
                System.out.println("⚠️ Albergue lleno. Invocando opción de derivación o filtrado...");
                return false;
            }
        }
        System.out.println("❌ Albergue no encontrado.");
        return false;
    }

    // RF-09: Consultar disponibilidad de otros albergues
    public List<Albergue> consultarDisponibilidad() {
        List<Albergue> disponibles = new ArrayList<>();
        for (Albergue alb : albergues) {
            if (alb.tieneCupo()) {
                disponibles.add(alb);
            }
        }
        return disponibles;
    }

    // RF-08: Derivar / trasladar damnificado a otro albergue (incluye RF-09)
    public boolean derivarDamnificado(String idAlbergueOrigen, String idAlbergueDestino, Damnificado damnificado) {
        // <include> Consultar disponibilidad de otros albergues
        List<Albergue> disponibles = consultarDisponibilidad();
        boolean destinoDisponible = disponibles.stream().anyMatch(a -> a.getId().equals(idAlbergueDestino));

        if (destinoDisponible) {
            Optional<Albergue> origen = buscarPorId(idAlbergueOrigen);
            Optional<Albergue> destino = buscarPorId(idAlbergueDestino);

            if (origen.isPresent() && destino.isPresent()) {
                origen.get().setOcupacionActual(origen.get().getOcupacionActual() - 1);
                destino.get().setOcupacionActual(destino.get().getOcupacionActual() + 1);
                System.out.println("🔄 " + damnificado.getNombre() + " derivado con éxito a " + destino.get().getNombre());
                return true;
            }
        }
        System.out.println("❌ No fue posible realizar la derivación.");
        return false;
    }

    // RF-05: Consultar panel semáforo (toda la red)
    public void mostrarPanelSemaforo() {
        System.out.println("\n=== PANEL SEMÁFORO DE RED DE ALBERGUES ===");
        for (Albergue alb : albergues) {
            System.out.println(alb);
        }
    }

    public Optional<Albergue> buscarPorId(String id) {
        return albergues.stream().filter(a -> a.getId().equalsIgnoreCase(id)).findFirst();
    }
}