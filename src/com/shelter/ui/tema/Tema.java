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
 * Tema "Centro de Comando de Emergencias": fondo Dark Slate con alto contraste.
 *
 * Toda la interfaz toma sus colores y fuentes de aquí, de modo que cada tipo de recurso, nivel de
 * semáforo, prioridad y tipo de damnificado tiene SIEMPRE el mismo color en tarjetas, chips,
 * barras e iconos. Cambiar un color en esta clase lo cambia en toda la aplicación.
 */
public final class Tema {

    private Tema() {
    }

    // ------------------------------------------------------------------
    // Superficies y texto (Dark Slate)
    // ------------------------------------------------------------------
    public static final Color FONDO = new Color(0x0B1220);
    public static final Color FONDO_LATERAL = new Color(0x0E1627);
    public static final Color SUPERFICIE = new Color(0x152033);
    public static final Color SUPERFICIE_ALTA = new Color(0x1C2A41);
    public static final Color CAMPO = new Color(0x0F1A2B);
    public static final Color BORDE = new Color(0x2A3A55);
    public static final Color BORDE_SUAVE = new Color(0x1F2D45);
    public static final Color TEXTO = new Color(0xE6EDF7);
    public static final Color TEXTO_SUAVE = new Color(0x93A3BC);
    public static final Color TEXTO_TENUE = new Color(0x5E6F8A);
    public static final Color ACENTO = new Color(0x38BDF8);
    public static final Color ACENTO_OSCURO = new Color(0x0369A1);
    public static final Color SELECCION = new Color(0x1E3A5F);

    // ------------------------------------------------------------------
    // Semáforo
    // ------------------------------------------------------------------
    public static final Color VERDE = new Color(0x22C55E);
    public static final Color AMARILLO = new Color(0xFACC15);
    public static final Color ROJO = new Color(0xEF4444);

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

    public static PaletaRecurso paleta(TipoRecurso tipo) {
        return switch (tipo) {
            // Hidratación: azul profundo / cian
            case AGUA -> new PaletaRecurso(new Color(0x0288D1), new Color(0x4FC3F7), "Agua");
            // Víveres: verde esmeralda / naranja cálido
            case ALIMENTO -> new PaletaRecurso(new Color(0x10B981), new Color(0xFB8C00), "Alimentos");
            // Salud: rojo coral / blanco clínico
            case MEDICINA -> new PaletaRecurso(new Color(0xFF5A5F), new Color(0xF8FAFC), "Medicina");
            // Refugio y abrigo: ámbar / amarillo
            case ABRIGO -> new PaletaRecurso(new Color(0xFFA000), new Color(0xFFD54F), "Abrigo");
            // Higiene: lavanda / lila (limpieza)
            case HIGIENE -> new PaletaRecurso(new Color(0x8B5CF6), new Color(0xC4B5FD), "Higiene");
        };
    }

    // ------------------------------------------------------------------
    // Prioridad de damnificados
    // ------------------------------------------------------------------
    public static Color color(NivelPrioridad nivel) {
        return switch (nivel) {
            case CRITICA -> new Color(0xF43F5E);
            case ALTA -> new Color(0xFB923C);
            case MEDIA -> new Color(0xFACC15);
            case BAJA -> new Color(0x60A5FA);
        };
    }

    /** Color y nombre del tipo de damnificado (General, Menor, Vulnerable). */
    public static Color colorTipo(Damnificado d) {
        if (d instanceof DamnificadoVulnerable) {
            return new Color(0xC084FC);
        }
        if (d instanceof DamnificadoMenor) {
            return new Color(0x2DD4BF);
        }
        return new Color(0x94A3B8);
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

    public static final Font TEXTO_NORMAL = fuente(Font.PLAIN, 13);
    public static final Font TEXTO_NEGRITA = fuente(Font.BOLD, 13);
    public static final Font TEXTO_PEQUENO = fuente(Font.PLAIN, 11);
    public static final Font TITULO_TARJETA = fuente(Font.BOLD, 12);
    public static final Font TITULO = fuente(Font.BOLD, 20);
    public static final Font CIFRA = fuente(Font.BOLD, 22);

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
        UIManager.put("nimbusBase", new Color(0x1A2740));
        UIManager.put("nimbusBlueGrey", new Color(0x2A3A55));
        UIManager.put("nimbusLightBackground", CAMPO);
        UIManager.put("nimbusFocus", ACENTO);
        UIManager.put("nimbusSelectionBackground", ACENTO_OSCURO);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusDisabledText", TEXTO_TENUE);
        UIManager.put("nimbusInfoBlue", ACENTO);
        UIManager.put("nimbusAlertYellow", AMARILLO);
        UIManager.put("nimbusRed", ROJO);
        UIManager.put("nimbusGreen", VERDE);
        UIManager.put("nimbusOrange", new Color(0xFB923C));
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
        UIManager.put("Table.alternateRowColor", new Color(0x121C2E));

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
                    g.setColor(new Color(0x111A29));
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
        tabla.setRowHeight(34);
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
