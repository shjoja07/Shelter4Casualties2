package com.shelter.config;

/**
 * Valores mínimos del Manual Esfera (Sphere Handbook) usados por el sistema,
 * en especial por el panel semáforo de la red de albergues.
 *
 * Fuente: The Sphere Handbook: Humanitarian Charter and Minimum Standards in
 * Humanitarian Response, 4ª edición, Sphere Association, 2018.
 * Los valores se extrajeron de MinimosSphere.md; si cambia uno, se cambia aquí
 * y en ese documento.
 *
 * Uso: SphereConstantes.AGUA_LITROS_PERSONA_DIA_MIN
 */
public final class SphereConstantes {

    private SphereConstantes() {
        // Clase de constantes: no se instancia.
    }

    // ------------------------------------------------------------------
    // Abastecimiento de agua
    // ------------------------------------------------------------------

    /** Litros por persona por día. Umbral mínimo. */
    public static final double AGUA_LITROS_PERSONA_DIA_MIN = 15.0;

    /** Litros por persona por día. Solo fase aguda de sequía o emergencia muy corta, de forma temporal. */
    public static final double AGUA_LITROS_PERSONA_DIA_EMERGENCIA = 7.5;

    /** Metros. Distancia máxima de una vivienda al punto de agua más cercano. */
    public static final int AGUA_DISTANCIA_MAX_METROS = 500;

    /** Minutos. Tiempo de cola máximo aceptable en un punto de agua. */
    public static final int AGUA_TIEMPO_COLA_MAX_MINUTOS = 30;

    /** Personas por grifo. Flujo de referencia: 7.5 L/min por grifo. */
    public static final int AGUA_PERSONAS_POR_GRIFO_MAX = 250;

    /** Personas por bomba manual. Flujo de referencia: 17 L/min por bomba manual. */
    public static final int AGUA_PERSONAS_POR_BOMBA_MAX = 500;

    /** Personas por pozo abierto. Flujo de referencia: 12.5 L/min por pozo abierto. */
    public static final int AGUA_PERSONAS_POR_POZO_MAX = 400;

    // ------------------------------------------------------------------
    // Saneamiento
    // ------------------------------------------------------------------

    /** Personas por inodoro. Ratio mínimo en el mediano plazo (1 inodoro cada 20 personas). */
    public static final int SANEAMIENTO_PERSONAS_POR_INODORO = 20;

    /** Personas por inodoro. Aceptable solo en fase inicial de una crisis repentina. */
    public static final int SANEAMIENTO_PERSONAS_POR_INODORO_FASE_INICIAL = 50;

    /** Metros. Distancia máxima entre vivienda e inodoro compartido. */
    public static final int SANEAMIENTO_DISTANCIA_MAX_METROS = 50;

    /** Personas por inodoro accesible (discapacidad): 1 cada 250 personas como mínimo. */
    public static final int SANEAMIENTO_RATIO_INODOROS_ACCESIBLES = 250;

    /** Ratio 3:1. Proporción de inodoros para mujeres respecto a hombres (3 por cada 1). */
    public static final double SANEAMIENTO_RATIO_MUJER_HOMBRE = 3.0;

    // ------------------------------------------------------------------
    // Alojamiento: espacio vital y altura
    // ------------------------------------------------------------------

    /** m² por persona. Excluye cocina, baño y área de aseo. */
    public static final double ESPACIO_VITAL_M2_PERSONA_MIN = 3.5;

    /** m² por persona. Climas fríos o espacio con cocina/baño interior. */
    public static final double ESPACIO_VITAL_M2_PERSONA_CLIMA_FRIO_MIN = 4.5;

    /** m² por persona. Límite superior del rango anterior. */
    public static final double ESPACIO_VITAL_M2_PERSONA_CLIMA_FRIO_MAX = 5.5;

    /** Metros. Altura mínima piso-techo en el punto más alto. */
    public static final double ALTURA_TECHO_MIN_METROS = 2.0;

    /** Metros. Altura mínima piso-techo para mejorar la ventilación en climas cálidos. */
    public static final double ALTURA_TECHO_MIN_METROS_CLIMA_CALIDO = 2.6;

    // ------------------------------------------------------------------
    // Asentamiento
    // ------------------------------------------------------------------

    /** m² por persona. Asentamiento tipo campamento con todos los servicios dentro. */
    public static final int ASENTAMIENTO_M2_PERSONA_CAMPAMENTO = 45;

    /** m² por persona. Si los servicios comunales ya existen fuera del asentamiento. */
    public static final int ASENTAMIENTO_M2_PERSONA_SERVICIOS_EXTERNOS = 30;
}
