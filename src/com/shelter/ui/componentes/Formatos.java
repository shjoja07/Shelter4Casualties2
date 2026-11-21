package com.shelter.ui.componentes;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Una sola regla para mostrar números y textos de estado en toda la interfaz.
 *
 * Antes convivían "40.00 litros", "40 litros", "20.0 L/persona" y "14.0 m²", y fracciones con
 * significados opuestos ("0 / 2" eran cupos libres; "4 / 107", personas alojadas). Ahora:
 *  - los números van sin ceros sobrantes y con como máximo dos decimales (40, 12.5, 0.75);
 *  - las fracciones se escriben con palabras ("0 libres de 2", "4 de 107 personas").
 */
public final class Formatos {

    private Formatos() {
    }

    private static final DecimalFormatSymbols SIMBOLOS = DecimalFormatSymbols.getInstance(Locale.US);

    /** 40 → "40"; 12.5 → "12.5"; 0.756 → "0.76". */
    public static String numero(double valor) {
        return new DecimalFormat("#,##0.##", SIMBOLOS).format(valor);
    }

    /** Cantidad con su unidad: "40 litros". */
    public static String cantidad(double valor, String unidad) {
        return numero(valor) + " " + unidad;
    }

    /** Porcentaje redondeado: "40 %". */
    public static String porcentaje(double valor) {
        return Math.round(valor) + " %";
    }

    /** Nombre legible del código de una alerta (sin mayúsculas sostenidas ni guiones bajos). */
    public static String etiquetaAlerta(String codigo) {
        return switch (codigo) {
            case "OCUPACION" -> "Ocupación";
            case "AGUA" -> "Agua";
            case "ESPACIO" -> "Espacio vital";
            case "CAPACIDAD" -> "Capacidad";
            case "RECURSO_FALTANTE" -> "Falta recurso";
            case "RECURSO_VENCIDO" -> "Lote vencido";
            default -> codigo;
        };
    }
}
