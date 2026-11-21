package com.shelter.ui.componentes;

import com.shelter.model.Albergue;
import com.shelter.model.Alerta;
import com.shelter.model.Damnificado;
import com.shelter.model.NivelPrioridad;
import com.shelter.model.NivelSemaforo;
import com.shelter.model.Recurso;
import com.shelter.model.TipoRecurso;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Renderizadores de celdas para tablas, listas y combos. Todos aplican la misma paleta del tema:
 * el color de un nivel o de un recurso es idéntico en cualquier lugar de la interfaz.
 */
public final class Renderizadores {

    private Renderizadores() {
    }

    private static Color fondoFila(JTable tabla, int fila, boolean seleccionada) {
        if (seleccionada) {
            return Tema.SELECCION;
        }
        return fila % 2 == 0 ? Tema.SUPERFICIE : new Color(0x121C2E);
    }

    // =========================================================
    // CHIP: etiqueta en forma de píldora con punto luminoso
    // =========================================================

    /** Etiqueta en forma de píldora; opcionalmente con un punto que "brilla". */
    public static class Chip extends JComponent {
        private static final long serialVersionUID = 1L;
        private String texto = "";
        private Color color = Tema.ACENTO;
        private boolean punto = true;
        private int alineacion = SwingConstants.LEFT;

        public Chip() {
            setFont(Tema.fuente(Font.BOLD, 11));
        }

        public Chip configurar(String texto, Color color, boolean punto) {
            this.texto = texto == null ? "" : texto;
            this.color = color;
            this.punto = punto;
            repaint();
            return this;
        }

        public void setAlineacion(int alineacion) {
            this.alineacion = alineacion;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            // Holgura extra: con escalado de pantalla el texto pintado puede ser algo más ancho que el medido
            return new Dimension((int) Math.ceil(fm.stringWidth(texto) * 1.08) + (punto ? 28 : 18) + 16, 24);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int ancho = fm.stringWidth(texto) + (punto ? 28 : 18);
            int alto = 22;
            int x = switch (alineacion) {
                case SwingConstants.CENTER -> (getWidth() - ancho) / 2;
                case SwingConstants.RIGHT -> getWidth() - ancho - 6;
                default -> 6;
            };
            int y = (getHeight() - alto) / 2;
            g2.setColor(Tema.alfa(color, 38));
            g2.fill(new RoundRectangle2D.Double(x, y, ancho, alto, alto, alto));
            g2.setColor(Tema.alfa(color, 110));
            g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, ancho - 1, alto - 1, alto, alto));
            int tx = x + 9;
            if (punto) {
                double cx = x + 12;
                double cy = y + alto / 2.0;
                g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), 7f, new float[]{0f, 1f},
                        new Color[]{Tema.alfa(color, 160), Tema.alfa(color, 0)}));
                g2.fill(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(cx - 3.5, cy - 3.5, 7, 7));
                tx = x + 20;
            }
            g2.setColor(Tema.mezclar(color, Color.WHITE, 0.45));
            g2.drawString(texto, tx, y + (alto - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Envuelve un componente en una celda con el fondo correcto de la fila. */
    private static JPanel celda(Component contenido, Color fondo) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(fondo);
        p.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        p.add(contenido, BorderLayout.CENTER);
        return p;
    }

    // =========================================================
    // TABLAS
    // =========================================================

    /** Encabezado oscuro con texto en mayúsculas pequeñas. */
    public static class Encabezado extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
            l.setText(v == null ? "" : v.toString().toUpperCase());
            l.setFont(Tema.fuente(Font.BOLD, 11));
            l.setForeground(Tema.TEXTO_SUAVE);
            l.setBackground(Tema.FONDO_LATERAL);
            l.setOpaque(true);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                    BorderFactory.createEmptyBorder(9, 10, 9, 10)));
            return l;
        }
    }

    /** Texto con relleno y filas alternadas. */
    public static class Texto extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final boolean monoespaciado;

        public Texto(boolean monoespaciado) {
            this.monoespaciado = monoespaciado;
        }

        public Texto() {
            this(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, false, r, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            l.setBackground(fondoFila(t, r, s));
            l.setForeground(monoespaciado ? Tema.ACENTO : Tema.TEXTO);
            l.setFont(monoespaciado ? Tema.mono(Font.BOLD, 12) : Tema.TEXTO_NORMAL);
            return l;
        }
    }

    /** Estado del semáforo como chip con punto luminoso (color + texto: accesible, RNF-01). */
    public static class SemaforoCelda implements TableCellRenderer {
        private final Chip chip = new Chip();

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof NivelSemaforo n) {
                chip.configurar(n.getEtiqueta(), Tema.color(n), true);
            } else {
                chip.configurar("", Tema.BORDE, false);
            }
            return celda(chip, fondoFila(t, r, s));
        }
    }

    /** Prioridad de un damnificado como chip de color. */
    public static class PrioridadCelda implements TableCellRenderer {
        private final Chip chip = new Chip();

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof NivelPrioridad n) {
                chip.configurar(n.getEtiqueta(), Tema.color(n), true);
            }
            return celda(chip, fondoFila(t, r, s));
        }
    }

    /** Tipo de recurso con su icono y color de paleta. */
    public static class TipoRecursoCelda extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, false, r, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            l.setBackground(fondoFila(t, r, s));
            if (v instanceof TipoRecurso tipo) {
                l.setIcon(Iconos.recurso(tipo, 18));
                l.setIconTextGap(8);
                l.setText(Tema.paleta(tipo).nombre());
                l.setForeground(Tema.mezclar(Tema.paleta(tipo).principal(), Color.WHITE, 0.35));
                l.setFont(Tema.TEXTO_NEGRITA);
            } else {
                l.setIcon(null);
            }
            return l;
        }
    }

    /** Porcentaje (Double o Integer) como barra de ocupación coloreada. */
    public static class OcupacionCelda implements TableCellRenderer {
        private final BarraNivel barra = new BarraNivel();

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            double pct = v instanceof Number n ? n.doubleValue() : 0;
            barra.setPorcentaje(pct, Math.round(pct) + "%");
            JPanel p = celda(barra, fondoFila(t, r, s));
            p.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
            return p;
        }
    }

    // =========================================================
    // LISTAS
    // =========================================================

    /**
     * Lista que siempre ocupa el ancho visible (sin barra horizontal) y recalcula la altura de sus
     * filas al cambiar de tamaño, para que los textos largos se ajusten en varias líneas.
     */
    public static <T> JList<T> lista(javax.swing.ListModel<T> modelo, ListCellRenderer<? super T> renderer) {
        JList<T> lista = new JList<>(modelo) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        lista.setCellRenderer(renderer);
        lista.setBackground(Tema.SUPERFICIE);
        lista.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                lista.setFixedCellHeight(10);   // invalida la caché de alturas de JList
                lista.setFixedCellHeight(-1);
            }
        });
        return lista;
    }

    /** Alerta: chip con el código y la gravedad, y debajo el mensaje completo ajustado al ancho. */
    public static class AlertaLista implements ListCellRenderer<Alerta> {
        private final JPanel panel = new JPanel(new BorderLayout(0, 4));
        private final Chip chip = new Chip();
        private final JLabel texto = new JLabel();

        public AlertaLista() {
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE),
                    BorderFactory.createEmptyBorder(7, 2, 8, 6)));
            texto.setFont(Tema.TEXTO_NORMAL);
            texto.setForeground(Tema.TEXTO);
            texto.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            JPanel fila = new JPanel(new BorderLayout());
            fila.setOpaque(false);
            fila.add(chip, BorderLayout.WEST);
            panel.add(fila, BorderLayout.NORTH);
            panel.add(texto, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Alerta> l, Alerta a, int i, boolean s, boolean f) {
            chip.configurar(a.gravedad().getEtiqueta() + " · " + a.codigo().replace('_', ' '), Tema.color(a.gravedad()), true);
            int ancho = Math.max(140, l.getWidth() - 30);
            texto.setText("<html><body style='width:" + ancho + "px'>" + escapar(a.mensaje()) + "</body></html>");
            panel.setBackground(s ? Tema.SELECCION : Tema.SUPERFICIE);
            return panel;
        }
    }

    /** Damnificado: código, nombre, tipo (texto de color) y chip de prioridad. */
    public static class DamnificadoLista implements ListCellRenderer<Damnificado> {
        private final JPanel panel = new JPanel(new BorderLayout(8, 0));
        private final JLabel izquierda = new JLabel();
        private final Chip chipPrioridad = new Chip();
        private final boolean numerado;

        public DamnificadoLista(boolean numerado) {
            this.numerado = numerado;
            panel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 4));
            izquierda.setForeground(Tema.TEXTO);
            panel.add(izquierda, BorderLayout.CENTER);
            panel.add(chipPrioridad, BorderLayout.EAST);
        }

        private static String hex(Color c) {
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Damnificado> l, Damnificado d, int i,
                                                      boolean s, boolean f) {
            if (d == null) {
                izquierda.setText(" ");
                chipPrioridad.configurar("", Tema.BORDE, false);
            } else {
                String orden = numerado ? "<span style='color:#5E6F8A'>" + (i + 1) + ".</span>&nbsp;" : "";
                izquierda.setText("<html>" + orden + "<span style='font-family:monospace;color:#38BDF8'><b>"
                        + escapar(d.getId()) + "</b></span>&nbsp; " + escapar(d.getNombreCompleto())
                        + "<br><span style='color:" + hex(Tema.colorTipo(d)) + "'>" + Tema.nombreTipo(d) + "</span>"
                        + " <span style='color:#93A3BC'>· " + d.getEdad() + " años</span></html>");
                chipPrioridad.configurar(d.getNivelPrioridad().getEtiqueta(), Tema.color(d.getNivelPrioridad()), true);
            }
            panel.setBackground(s ? Tema.SELECCION : (i % 2 == 0 ? Tema.SUPERFICIE : new Color(0x121C2E)));
            return panel;
        }
    }

    /** Lote de inventario: icono y color del tipo + nombre del lote. */
    public static class LoteCelda extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, false, r, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
            l.setBackground(fondoFila(t, r, s));
            if (v instanceof Recurso rec) {
                l.setIcon(Iconos.recurso(rec.getTipo(), 18));
                l.setIconTextGap(8);
                l.setText(rec.getNombre());
                l.setToolTipText(Tema.paleta(rec.getTipo()).nombre() + " · " + rec.getNombre());
                l.setForeground(Tema.mezclar(Tema.paleta(rec.getTipo()).principal(), Color.WHITE, 0.35));
                l.setFont(Tema.TEXTO_NEGRITA);
            }
            return l;
        }
    }

    /** Texto simple para combos (fondo oscuro del tema en lugar del gris de Nimbus). */
    public static class TextoCombo extends javax.swing.DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
            JLabel label = (JLabel) super.getListCellRendererComponent(l, v, i, s, f);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setBackground(s ? Tema.SELECCION : Tema.CAMPO);
            label.setForeground(Tema.TEXTO);
            return label;
        }
    }

    /** Albergue en un combo: punto de semáforo, ID, nombre y cupos. */
    public static class AlbergueCombo implements ListCellRenderer<Albergue> {
        private final JLabel label = new JLabel();

        public AlbergueCombo() {
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Albergue> l, Albergue a, int i, boolean s, boolean f) {
            if (a == null) {
                label.setText("— Sin albergues —");
                label.setIcon(null);
            } else {
                label.setIcon(new PuntoIcono(Tema.color(a.getEstadoSemaforo())));
                label.setText(a.getId() + "  ·  " + a.getNombre() + "   (" + a.getCuposDisponibles() + " cupos)");
            }
            label.setBackground(s ? Tema.SELECCION : Tema.CAMPO);
            label.setForeground(Tema.TEXTO);
            return label;
        }
    }

    /** Damnificado en un combo: código + nombre + prioridad. */
    public static class DamnificadoCombo implements ListCellRenderer<Damnificado> {
        private final JLabel label = new JLabel();

        public DamnificadoCombo() {
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Damnificado> l, Damnificado d, int i,
                                                      boolean s, boolean f) {
            if (d == null) {
                label.setText("— Nadie alojado —");
                label.setIcon(null);
            } else {
                label.setIcon(new PuntoIcono(Tema.color(d.getNivelPrioridad())));
                label.setText(d.getId() + "  ·  " + d.getNombreCompleto() + "  [" + d.getNivelPrioridad().getEtiqueta() + "]");
            }
            label.setBackground(s ? Tema.SELECCION : Tema.CAMPO);
            label.setForeground(Tema.TEXTO);
            return label;
        }
    }

    /** Recurso en un combo: icono del tipo, nombre y stock. */
    public static class RecursoCombo implements ListCellRenderer<Recurso> {
        private final JLabel label = new JLabel();

        public RecursoCombo() {
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setIconTextGap(8);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Recurso> l, Recurso r, int i, boolean s, boolean f) {
            if (r == null) {
                label.setText("— Sin recursos —");
                label.setIcon(null);
            } else {
                label.setIcon(Iconos.recurso(r.getTipo(), 16));
                String vence = r.getFechaVencimiento() == null ? "no vence" : "vence " + r.getFechaVencimiento();
                label.setText(String.format("%s  ·  %.2f %s  (%s)", r.getNombre(), r.getCantidadDisponible(), r.getUnidad(), vence));
            }
            label.setBackground(s ? Tema.SELECCION : Tema.CAMPO);
            label.setForeground(Tema.TEXTO);
            return label;
        }
    }

    /** Tipo de recurso en un combo, con icono y nombre coloreado. */
    public static class TipoRecursoCombo implements ListCellRenderer<TipoRecurso> {
        private final JLabel label = new JLabel();

        public TipoRecursoCombo() {
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setIconTextGap(8);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends TipoRecurso> l, TipoRecurso t, int i,
                                                      boolean s, boolean f) {
            if (t != null) {
                label.setIcon(Iconos.recurso(t, 16));
                label.setText(Tema.paleta(t).nombre() + (t.esCritico() ? "  · crítico" : ""));
            }
            label.setBackground(s ? Tema.SELECCION : Tema.CAMPO);
            label.setForeground(Tema.TEXTO);
            return label;
        }
    }

    /** Pequeño punto de color con halo. */
    public record PuntoIcono(Color color) implements javax.swing.Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.alfa(color, 70));
            g2.fill(new Ellipse2D.Double(x, y, 12, 12));
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x + 3, y + 3, 6, 6));
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    /** Evita que un nombre con "<" rompa el HTML de una etiqueta. */
    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
