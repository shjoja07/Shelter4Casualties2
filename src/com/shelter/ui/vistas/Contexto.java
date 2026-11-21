package com.shelter.ui.vistas;

import java.util.function.Consumer;

/**
 * Estado de la interfaz compartido por las cuatro secciones.
 *
 * Antes cada sección tenía su propio albergue elegido: si el encargado miraba el Refugio Norte
 * en el Tablero y pasaba a Recursos, el formulario seguía en el Refugio Central y podía registrar
 * un lote en el albergue equivocado. Ahora hay un único "albergue en foco" que todas respetan.
 *
 * Es estado de la vista (qué está mirando la persona), no del negocio: por eso vive en la UI y
 * no en el controlador.
 */
public final class Contexto {

    /** Secciones de la ventana, en el orden de la barra lateral. */
    public enum Seccion { TABLERO, DAMNIFICADOS, RECURSOS, HISTORIAL }

    private String idAlbergue;
    private Consumer<Seccion> navegador = s -> { };

    public String getIdAlbergue() {
        return idAlbergue;
    }

    public void setIdAlbergue(String idAlbergue) {
        this.idAlbergue = idAlbergue;
    }

    /** La ventana registra aquí cómo cambiar de sección. */
    public void setNavegador(Consumer<Seccion> navegador) {
        this.navegador = navegador;
    }

    /** Cambia de sección; la sección destino se alinea con el albergue en foco al mostrarse. */
    public void ir(Seccion seccion) {
        navegador.accept(seccion);
    }

    // ----- Acciones que resuelven una alerta (las registra la ventana) -----

    private Consumer<com.shelter.model.TipoRecurso> registrarLote = t -> { };
    private Runnable derivar = () -> { };

    public void setAcciones(Consumer<com.shelter.model.TipoRecurso> registrarLote, Runnable derivar) {
        this.registrarLote = registrarLote;
        this.derivar = derivar;
    }

    /** Abre Recursos con el albergue en foco y el tipo de recurso ya elegidos. */
    public void registrarLote(com.shelter.model.TipoRecurso tipo) {
        registrarLote.accept(tipo);
    }

    /** Abre la derivación con el albergue en foco como origen. */
    public void derivar() {
        derivar.run();
    }
}
