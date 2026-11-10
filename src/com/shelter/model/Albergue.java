package com.shelter.model;

import java.util.ArrayList;
import java.util.List;

public class Albergue {
    private String id;
    private String nombre;
    private String distrito;
    private int capacidadMaxima;
    private int ocupacionActual;
    private String estadoSemaforo;

    // Listas para la gestión interna del albergue
    private List<Damnificado> listaDamnificados;
    private List<Recurso> listaRecursos;

    // Constructor completo
    public Albergue(String id, String nombre, String distrito, int capacidadMaxima) {
        this.id = id;
        this.nombre = nombre;
        this.distrito = distrito;
        this.capacidadMaxima = capacidadMaxima;
        this.ocupacionActual = 0;
        this.estadoSemaforo = "VERDE";
        this.listaDamnificados = new ArrayList<>();
        this.listaRecursos = new ArrayList<>();
    }

    // Constructor vacío por defecto
    public Albergue() {
        this.listaDamnificados = new ArrayList<>();
        this.listaRecursos = new ArrayList<>();
        this.estadoSemaforo = "VERDE";
    }
    // =======================================================
    // MÉTODOS DE GESTIÓN DE RECURSOS
    // =======================================================

    /**
     * Registra e inserta un nuevo recurso en la lista del albergue.
     */
    public boolean registrarRecurso(Recurso recurso) {
        if (recurso == null) {
            return false;
        }
        if (this.listaRecursos == null) {
            this.listaRecursos = new ArrayList<>();
        }
        this.listaRecursos.add(recurso);
        return true;
    }

    public List<Recurso> getRecursos() {
        return this.listaRecursos;
    }

    // =======================================================
    // MÉTODOS DE GESTIÓN DE DAMNIFICADOS
    // =======================================================

    /**
     * Aloja a un nuevo damnificado en el albergue si hay capacidad disponible.
     * Actualiza automáticamente el contador de ocupación actual.
     * 
     * @param damnificado El objeto Damnificado a ingresar.
     * @return true si se alojó con éxito; false si el albergue está lleno o el objeto es nulo.
     */
    public boolean alojarDamnificado(Damnificado damnificado) {
        if (damnificado == null) {
            return false;
        }
        if (tieneCupo()) {
            this.listaDamnificados.add(damnificado);
            this.ocupacionActual = this.listaDamnificados.size();
            return true;
        }
        return false;
    }

    public List<Damnificado> getDamnificados() {
        return this.listaDamnificados;
    }

    /**
     * Retira a un damnificado del albergue utilizando su DNI o ID.
     * Actualiza el contador de ocupación si se remueve correctamente.
     * 
     * @param dni El número de DNI o ID del damnificado.
     * @return true si se encontró y retiró exitosamente; false en caso contrario.
     */
    public boolean retirarDamnificado(String dni) {
        Damnificado encontrado = buscarDamnificado(dni);
        if (encontrado != null) {
            boolean removido = this.listaDamnificados.remove(encontrado);
            if (removido) {
                this.ocupacionActual = this.listaDamnificados.size();
                return true;
            }
        }
        return false;
    }

    /**
     * Busca a un damnificado dentro del albergue mediante su DNI o ID.
     * 
     * @param dni El número de DNI o ID a consultar.
     * @return El objeto Damnificado hallado, o null si no se encuentra alojado.
     */
    public Damnificado buscarDamnificado(String dni) {
        if (dni == null || dni.trim().isEmpty()) {
            return null;
        }
        for (Damnificado d : this.listaDamnificados) {
            if (d.getDni() != null && d.getDni().equalsIgnoreCase(dni)) {
                return d;
            }
        }
        return null;
    }

    // =======================================================
    // MÉTODOS AUXILIARES DE CONSULTA
    // =======================================================

    public boolean tieneCupo() {
        return this.ocupacionActual < this.capacidadMaxima;
    }

    public int getCuposDisponibles() {
        return Math.max(0, this.capacidadMaxima - this.ocupacionActual);
    }

    // =======================================================
    // GETTERS Y SETTERS
    // =======================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public int getOcupacionActual() {
        return ocupacionActual;
    }

    public void setOcupacionActual(int ocupacionActual) {
        this.ocupacionActual = ocupacionActual;
    }

    public String getEstadoSemaforo() {
        return estadoSemaforo;
    }

    public void setEstadoSemaforo(String estadoSemaforo) {
        this.estadoSemaforo = estadoSemaforo;
    }

    public List<Damnificado> getListaDamnificados() {
        return listaDamnificados;
    }

    public void setListaDamnificados(List<Damnificado> listaDamnificados) {
        this.listaDamnificados = listaDamnificados;
        this.ocupacionActual = (listaDamnificados != null) ? listaDamnificados.size() : 0;
    }

    public List<Recurso> getListaRecursos() {
        return listaRecursos;
    }

    public void setListaRecursos(List<Recurso> listaRecursos) {
        this.listaRecursos = listaRecursos;
    }
}