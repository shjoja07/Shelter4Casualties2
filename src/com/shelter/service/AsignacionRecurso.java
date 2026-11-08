package com.shelter.service;

import com.shelter.model.Albergue;
import com.shelter.model.Damnificado;
import com.shelter.model.Recurso;
import com.shelter.model.RegistroAsignacion;

import java.util.ArrayList;
import java.util.List;

public class AsignacionRecurso {
    
    private AlbergueService albergueService;
    
    //Uso de listas 
    private List<RegistroAsignacion> historial;

    public AsignacionRecurso(AlbergueService albergueService) {
        this.albergueService = albergueService;
        this.historial = new ArrayList<>();
    }

    // RF-04: Calcular orden de prioridad
    public List<Damnificado> calcularOrdenPrioridad(Albergue albergue) {
        // Obtenemos la lista de damnificados del albergue 
        List<Damnificado> listaDamnificados = albergue.getDamnificados();
        
        // Creamos una nueva lista temporal para no desordenar la lista original del albergue
        List<Damnificado> damnificadosPriorizados = new ArrayList<>(listaDamnificados);
        
        //Ordenamos usando el método de prioridad de AlbergueService (1 Alta, 2 Normal)
        damnificadosPriorizados.sort((d1, d2) -> {
            int prioridad1 = albergueService.calcularPrioridadAsignacion(d1);
            int prioridad2 = albergueService.calcularPrioridadAsignacion(d2);
            return Integer.compare(prioridad1, prioridad2);
        });
        
        return damnificadosPriorizados;
    }

    // RF-04: Asignar recursos a los damnificados
    public List<RegistroAsignacion> asignar(Albergue albergue, Recurso recurso, double cantidadTotalADistribuir) {
        List<RegistroAsignacion> registrosNuevos = new ArrayList<>();
        
        // 1. Obtener la lista ya ordenada por prioridad
        List<Damnificado> priorizados = calcularOrdenPrioridad(albergue);
        
        double cantidadRestante = cantidadTotalADistribuir;
        
        // 2. USO DE LISTAS: Recorremos la lista ordenada para repartir
        for (Damnificado d : priorizados) {
            // Si ya no queda recurso para repartir o el inventario está en cero, terminamos el bucle
            if (cantidadRestante <= 0 || recurso.getCantidadDisponible() <= 0) {
                break;
            }
            
            // Lógica de reparto (Ejemplo: entregamos 1.0 unidad a cada persona por turno)
            double cantidadEntregada = 1.0; 
            if (cantidadRestante < cantidadEntregada) {
                cantidadEntregada = cantidadRestante; // Si queda menos de 1, entregamos solo lo que sobra
            }
            
            // Descontamos del inventario físico (Este método ya existe en tu clase Recurso)
            recurso.descontar(cantidadEntregada);
            cantidadRestante -= cantidadEntregada;
            
            // Creamos la constancia de entrega
            RegistroAsignacion registro = new RegistroAsignacion(
                albergue.getId(),
                d.getDni(),
                d.getNombreCompleto(),
                recurso.getNombre(),
                recurso.getUnidad(),
                cantidadEntregada
            );
            
            // 3. USO DE LISTAS: Agregamos el registro a nuestra lista del historial (.add)
            this.historial.add(registro);
            registrosNuevos.add(registro);
        }
        
        return registrosNuevos;
    }

    // Getter
    public List<RegistroAsignacion> getHistorial() {
        return historial;
    }
}