package com.shelter.ui.tema;

import com.shelter.model.Damnificado;
import com.shelter.model.DamnificadoMenor;
import com.shelter.model.DamnificadoVulnerable;
import com.shelter.model.NivelPrioridad;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.TipoRecurso;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/**
 * Tema "Grafito sobrio".
 *
 * Una sola regla de color: en la pantalla solo tiene color lo que indica un ESTADO (rojo, ámbar,
 * verde del semáforo, y la prioridad cuando es urgente). Superficies, texto, recursos, tipos de
 * damnificado y acciones son neutros. Así, cuando algo se ve rojo, es porque requiere atención.
 *
 * Reemplaza al tema anterior "azul marino con neón" (acento cian, resplandores), que era el look por
 * defecto de cualquier "centro de comando". Toda la interfaz toma de aquí sus colores y fuentes.
 */
public final class Tema {

    private Tema() {
    }

    // ------------------------------------------------------------------
    // Superficies y texto: grafito neutro, sin tinte azul
    // ------------------------------------------------------------------
    public static final Color FONDO = new Color(0x18181A);
    public static final Color FONDO_LATERAL = new Color(0x141416);
    public static final Color SUPERFICIE = new Color(0x222225);
    public static final Color SUPERFICIE_ALTA = new Color(0x26262A);
    /** Fila alterna de tablas y listas. */
    public static final Color FILA_ALTERNA = new Color(0x1E1E21);
    public static final Color CAMPO = new Color(0x1B1B1E);
    public static final Color BORDE = new Color(0x3A3A40);
    public static final Color BORDE_SUAVE = new Color(0x34343A);
    public static final Color TEXTO = new Color(0xF2F1EE);
    /** Texto secundario: 8.6:1 sobre SUPERFICIE (un proyector se come el contraste fino). */
    public static final Color TEXTO_SUAVE = new Color(0xB9B7B1);
    /** Texto terciario: 5.0:1 sobre SUPERFICIE (cumple AA). */
    public static final Color TEXTO_TENUE = new Color(0x908E88);
    /** Foco, selección y elemento activo: claro, no un color (el color queda para los estados). */
    public static final Color ACENTO = new Color(0xE9E7E1);
    /** Fondo de la acción principal (botón claro sobre grafito). */
    public static final Color ACCION = new Color(0xE9E7E1);
    /** Texto e iconos sobre la acción principal. */
    public static final Color TINTA_ACCION = new Color(0x18181A);
    public static final Color ACENTO_OSCURO = ACCION;
    public static final Color SELECCION = new Color(0x3A3A41);

    // ------------------------------------------------------------------
    // Semáforo: los únicos colores saturados de la interfaz
    // ------------------------------------------------------------------
    public static final Color VERDE = new Color(0x3FA34D);
    public static final Color AMARILLO = new Color(0xE3A008);
    public static final Color ROJO = new Color(0xE5484D);
    /** Rojo para fondos de botón con texto blanco (5.5:1). */
    public static final Color ROJO_BOTON = new Color(0xC2353A);

    public static Color color(NivelSemaforo nivel) {
        return switch (nivel) {
            case VERDE -> VERDE;
            case AMARILLO -> AMARILLO;
            case ROJO -> ROJO;
        };
    }

    // ------------------------------------------------------------------
    // Recursos: color principal y secundario de cada TipoRecurso
    // ------------------------------------------------------------------

    /** Paleta de un tipo de recurso: color principal, secundario y nombre visible. */
    public record PaletaRecurso(Color principal, Color secundario, String nombre) {
    }

    /**
     * Los recursos ya no tienen color propio: un recurso no es un estado. Se distinguen por su
     * nombre y su icono monocromo; el color aparece solo si falta (rojo).
     */
    public static PaletaRecurso paleta(TipoRecurso tipo) {
        String nombre = switch (tipo) {
            case AGUA -> "Agua";
            case ALIMENTO -> "Alimentos";
            case MEDICINA -> "Medicina";
            case ABRIGO -> "Abrigo";
            case HIGIENE -> "Higiene";
        };
        return new PaletaRecurso(TEXTO_SUAVE, TEXTO, nombre);
    }

    // ------------------------------------------------------------------
    // Prioridad de damnificados: color solo cuando es urgente
    // ------------------------------------------------------------------
    public static Color color(NivelPrioridad nivel) {
        return switch (nivel) {
            case CRITICA -> ROJO;
            case ALTA -> AMARILLO;
            case MEDIA -> TEXTO_SUAVE;
            case BAJA -> TEXTO_TENUE;
        };
    }

    /** ¿Esta prioridad merece pastilla de color? Solo CRÍTICA y ALTA; MEDIA y BAJA van como texto. */
    public static boolean esUrgente(NivelPrioridad nivel) {
        return nivel == NivelPrioridad.CRITICA || nivel == NivelPrioridad.ALTA;
    }

    /** El tipo de damnificado (General, Menor, Vulnerable) no es un estado: texto neutro. */
    public static Color colorTipo(Damnificado d) {
        return TEXTO_SUAVE;
    }

    /** Color en formato #RRGGBB para etiquetas HTML de Swing. */
    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static String nombreTipo(Damnificado d) {
        if (d instanceof DamnificadoVulnerable) {
            return "Vulnerable";
        }
        if (d instanceof DamnificadoMenor) {
            return "Menor";
        }
        return "General";
    }

    // ------------------------------------------------------------------
    // Utilidades de color
    // ------------------------------------------------------------------
    public static Color alfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alfa)));
    }

    /** Mezcla dos colores: t = 0 devuelve a, t = 1 devuelve b. */
    public static Color mezclar(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * k),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * k),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * k),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * k));
    }

    // ------------------------------------------------------------------
    // Tipografía
    // ------------------------------------------------------------------
    private static final String FAMILIA = elegirFamilia("Segoe UI", "Inter", "Helvetica Neue", "Arial", Font.SANS_SERIF);
    private static final String FAMILIA_MONO = elegirFamilia("Consolas", "JetBrains Mono", "Menlo", Font.MONOSPACED);

    private static String elegirFamilia(String... candidatas) {
        List<String> disponibles = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String c : candidatas) {
            if (disponibles.contains(c)) {
                return c;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA, estilo, Math.round(tamano));
    }

    public static Font mono(int estilo, float tamano) {
        return new Font(FAMILIA_MONO, estilo, Math.round(tamano));
    }

    // Escala pensada para proyector: nada por debajo de 12 px; el texto de lectura a 14 px.
    // Pasos cortos (12 · 14 · 16 · 20): en una herramienta de trabajo la jerarquía la dan el
    // peso y la posición, no tamaños exagerados.
    public static final Font TEXTO_NORMAL = fuente(Font.PLAIN, 14);
    public static final Font TEXTO_NEGRITA = fuente(Font.BOLD, 14);
    public static final Font TEXTO_PEQUENO = fuente(Font.PLAIN, 12);
    public static final Font ETIQUETA = fuente(Font.BOLD, 12);
    public static final Font TITULO_TARJETA = fuente(Font.BOLD, 14);
    public static final Font SUBTITULO = fuente(Font.BOLD, 16);
    public static final Font TITULO = fuente(Font.BOLD, 20);

    // ------------------------------------------------------------------
    // Instalación del Look & Feel
    // ------------------------------------------------------------------

    /**
     * Instala Nimbus (incluido en el JDK) con la paleta oscura. Nimbus deriva todos sus colores de
     * unas pocas claves base, por eso basta con redefinirlas para oscurecer diálogos, combos,
     * campos y barras de desplazamiento sin librerías externas.
     */
    public static void instalar() {
        UIManager.put("control", SUPERFICIE);
        UIManager.put("info", SUPERFICIE_ALTA);
        UIManager.put("nimbusBase", new Color(0x2A2A2E));
        UIManager.put("nimbusBlueGrey", new Color(0x3A3A40));
        UIManager.put("nimbusLightBackground", CAMPO);
        UIManager.put("nimbusFocus", new Color(0x9A9892));
        UIManager.put("nimbusSelectionBackground", SELECCION);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusDisabledText", TEXTO_TENUE);
        UIManager.put("nimbusInfoBlue", TEXTO_SUAVE);
        UIManager.put("nimbusAlertYellow", AMARILLO);
        UIManager.put("nimbusRed", ROJO);
        UIManager.put("nimbusGreen", VERDE);
        UIManager.put("nimbusOrange", AMARILLO);
        UIManager.put("text", TEXTO);
        UIManager.put("textForeground", TEXTO);
        UIManager.put("textBackground", CAMPO);
        UIManager.put("background", SUPERFICIE);
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Si Nimbus no existe se usa el Look & Feel por defecto; los componentes propios
            // siguen pintándose con el tema porque no dependen del Look & Feel.
        }
        FontUIResource fuenteBase = new FontUIResource(TEXTO_NORMAL);
        for (String clave : new String[]{"Label.font", "Button.font", "TextField.font", "ComboBox.font",
                "CheckBox.font", "Spinner.font", "List.font", "Table.font", "TableHeader.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TextArea.font", "ToolTip.font",
                "FormattedTextField.font", "TabbedPane.font"}) {
            UIManager.put(clave, fuenteBase);
        }
        UIManager.put("OptionPane.background", new ColorUIResource(SUPERFICIE));
        UIManager.put("Panel.background", new ColorUIResource(SUPERFICIE));
        UIManager.put("ToolTip.background", new ColorUIResource(SUPERFICIE_ALTA));
        UIManager.put("ToolTip.foreground", new ColorUIResource(TEXTO));
        UIManager.put("Table.alternateRowColor", FILA_ALTERNA);

        // Combos oscuros: Nimbus pinta su fondo con "painters" grises; se reemplazan por uno propio.
        javax.swing.Painter<javax.swing.JComponent> fondoCombo = (g, c, w, h) -> pintarCampo(g, w, h, c.hasFocus());
        javax.swing.Painter<javax.swing.JComponent> vacio = (g, c, w, h) -> { };
        for (String estado : new String[]{"Enabled", "Focused", "MouseOver", "Focused+MouseOver", "Pressed",
                "Focused+Pressed", "Enabled+Selected", "Disabled", "Disabled+Pressed"}) {
            UIManager.put("ComboBox[" + estado + "].backgroundPainter", fondoCombo);
            UIManager.put("ComboBox:\"ComboBox.arrowButton\"[" + estado + "].backgroundPainter", vacio);
        }
        UIManager.put("ComboBox:\"ComboBox.renderer\"[Enabled].textForeground", TEXTO);
        UIManager.put("ComboBox:\"ComboBox.listRenderer\".background", CAMPO);
        // Campos de texto deshabilitados: oscuros, no azul grisáceo
        UIManager.put("TextField[Disabled].backgroundPainter",
                (javax.swing.Painter<javax.swing.JComponent>) (g, c, w, h) -> {
                    g.setColor(FONDO_LATERAL);
                    g.fillRect(0, 0, w, h);
                });
    }

    private static void pintarCampo(java.awt.Graphics2D g, int w, int h, boolean foco) {
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(CAMPO);
        g.fillRoundRect(1, 1, w - 2, h - 2, 8, 8);
        g.setColor(foco ? ACENTO : BORDE);
        g.drawRoundRect(1, 1, w - 3, h - 3, 8, 8);
    }

    // ------------------------------------------------------------------
    // Estilo de componentes estándar
    // ------------------------------------------------------------------
    public static Border bordeVacio(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    /** Aplica el estilo oscuro a una tabla y a su encabezado. */
    public static void estilizar(JTable tabla) {
        tabla.setBackground(SUPERFICIE);
        tabla.setForeground(TEXTO);
        tabla.setGridColor(BORDE_SUAVE);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setRowHeight(38);
        tabla.setIntercellSpacing(new java.awt.Dimension(0, 1));
        tabla.setSelectionBackground(SELECCION);
        tabla.setSelectionForeground(Color.WHITE);
        tabla.setFillsViewportHeight(true);
        tabla.setFont(TEXTO_NORMAL);
        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setReorderingAllowed(false);
        encabezado.setDefaultRenderer(new com.shelter.ui.componentes.Renderizadores.Encabezado());
    }

    /** Scroll sin bordes del Look & Feel, con fondo del tema. */
    public static JScrollPane scroll(JComponent contenido) {
        JScrollPane sp = new JScrollPane(contenido);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(SUPERFICIE);
        sp.setBackground(SUPERFICIE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }
}
