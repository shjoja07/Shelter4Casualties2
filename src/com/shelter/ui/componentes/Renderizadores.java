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
        return fila % 2 == 0 ? Tema.SUPERFICIE : Tema.FILA_ALTERNA;
    }

    // =========================================================
    // CHIP: etiqueta en forma de píldora con punto luminoso
    // =========================================================

    /**
     * Indicador de estado. Con fondo es una pastilla (para lo que exige atención: rojo, ámbar,
     * prioridad urgente); sin fondo es texto simple con un punto opcional (para lo que está bien).
     * Así la pastilla de color vuelve a significar "mira esto", en lugar de decorar cada celda.
     */
    public static class Chip extends JComponent {
        private static final long serialVersionUID = 1L;
        private String texto = "";
        private Color color = Tema.ACENTO;
        private boolean punto = true;
        private boolean fondo = true;
        private int alineacion = SwingConstants.LEFT;

        public Chip() {
            setFont(Tema.ETIQUETA);
        }

        /** Pastilla de color: solo para estados que requieren atención. */
        public Chip configurar(String texto, Color color, boolean punto) {
            return configurar(texto, color, punto, true);
        }

        public Chip configurar(String texto, Color color, boolean punto, boolean fondo) {
            this.texto = texto == null ? "" : texto;
            this.color = color;
            this.punto = punto;
            this.fondo = fondo;
            revalidate(); // el ancho depende del texto: el contenedor debe recalcular su distribución
            repaint();
            return this;
        }

        /** Texto simple, sin pastilla: el estado está bien o el dato no es un estado. */
        public Chip texto(String texto, Color colorPunto) {
            return configurar(texto, colorPunto, colorPunto != null, false);
        }

        public void setAlineacion(int alineacion) {
            this.alineacion = alineacion;
        }

        /** El texto del chip está dibujado: se expone como etiqueta para los lectores de pantalla. */
        @Override
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public javax.accessibility.AccessibleRole getAccessibleRole() {
                        return javax.accessibility.AccessibleRole.LABEL;
                    }

                    @Override
                    public String getAccessibleName() {
                        return texto;
                    }
                };
            }
            return accessibleContext;
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
            if (fondo) {
                g2.setColor(Tema.alfa(color, 40));
                g2.fill(new RoundRectangle2D.Double(x, y, ancho, alto, alto, alto));
                g2.setColor(Tema.alfa(color, 120));
                g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, ancho - 1, alto - 1, alto, alto));
            } else {
                x -= 6; // sin pastilla el texto se alinea con el resto de la columna
            }
            int tx = x + (fondo ? 9 : 6);
            if (punto && color != null) {
                double cx = x + 12;
                double cy = y + alto / 2.0;
                g2.setColor(color);
                g2.fill(new Ellipse2D.Double(cx - 3.5, cy - 3.5, 7, 7));
                tx = x + 20;
            }
            g2.setFont(fondo ? getFont() : Tema.TEXTO_NORMAL);
            fm = g2.getFontMetrics();
            g2.setColor(fondo ? Tema.mezclar(color, Color.WHITE, 0.45) : Tema.TEXTO_SUAVE);
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
            l.setFont(Tema.ETIQUETA);
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
                // Verde = todo en orden: texto con su punto, sin pastilla. Ámbar y rojo sí la llevan.
                if (n == NivelSemaforo.VERDE) {
                    chip.texto(n.getEtiqueta(), Tema.VERDE);
                } else {
                    chip.configurar(n.getEtiqueta(), Tema.color(n), true);
                }
            } else {
                chip.configurar("", Tema.BORDE, false);
            }
            return celda(chip, fondoFila(t, r, s));
        }
    }

    /** Prioridad: pastilla solo si es urgente (crítica o alta); media y baja van como texto. */
    public static class PrioridadCelda implements TableCellRenderer {
        private final Chip chip = new Chip();

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v instanceof NivelPrioridad n) {
                if (Tema.esUrgente(n)) {
                    chip.configurar(n.getEtiqueta(), Tema.color(n), true);
                } else {
                    chip.texto(n.getEtiqueta(), null);
                }
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
    public static <T> JList<T> lista(javax.swing.ListModel<T> modelo, ListCellRenderer<? super T> renderer,
                                     java.util.function.Supplier<String> textoVacio) {
        JList<T> lista = new JList<>(modelo) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getModel().getSize() == 0) {
                    pintarVacio(this, g, textoVacio.get());
                }
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

    /** Tabla que, sin filas, explica por qué está vacía y qué hacer. */
    public static JTable tabla(javax.swing.table.TableModel modelo, java.util.function.Supplier<String> textoVacio) {
        return new JTable(modelo) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    pintarVacio(this, g, textoVacio.get());
                }
            }
        };
    }

    /** Mensaje centrado y ajustado al ancho para un contenedor vacío. */
    private static void pintarVacio(JComponent c, Graphics g, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Tema.TEXTO_NORMAL);
        g2.setColor(Tema.TEXTO_SUAVE);
        FontMetrics fm = g2.getFontMetrics();
        int ancho = Math.max(120, c.getWidth() - 48);
        java.util.List<String> lineas = new java.util.ArrayList<>();
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (fm.stringWidth(prueba) > ancho && linea.length() > 0) {
                lineas.add(linea.toString());
                linea = new StringBuilder(palabra);
            } else {
                linea = new StringBuilder(prueba);
            }
        }
        lineas.add(linea.toString());
        int y = 28 + fm.getAscent();
        for (String l : lineas) {
            g2.drawString(l, (c.getWidth() - fm.stringWidth(l)) / 2, y);
            y += fm.getHeight() + 2;
        }
        g2.dispose();
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

        @Override
        public Component getListCellRendererComponent(JList<? extends Damnificado> l, Damnificado d, int i,
                                                      boolean s, boolean f) {
            if (d == null) {
                izquierda.setText(" ");
                chipPrioridad.configurar("", Tema.BORDE, false);
            } else {
                String orden = numerado ? "<span style='color:" + Tema.hex(Tema.TEXTO_TENUE) + "'>" + (i + 1) + ".</span>&nbsp;" : "";
                izquierda.setText("<html>" + orden + "<span style='font-family:monospace;color:" + Tema.hex(Tema.TEXTO) + "'><b>"
                        + escapar(d.getId()) + "</b></span>&nbsp; " + escapar(d.getNombreCompleto())
                        + "<br><span style='color:" + Tema.hex(Tema.colorTipo(d)) + "'>" + Tema.nombreTipo(d) + "</span>"
                        + " <span style='color:" + Tema.hex(Tema.TEXTO_SUAVE) + "'>· " + d.getEdad() + " años</span></html>");
                NivelPrioridad np = d.getNivelPrioridad();
                if (Tema.esUrgente(np)) {
                    chipPrioridad.configurar(np.getEtiqueta(), Tema.color(np), true);
                } else {
                    chipPrioridad.texto(np.getEtiqueta(), null); // media y baja: texto, sin pastilla
                }
            }
            panel.setBackground(s ? Tema.SELECCION : (i % 2 == 0 ? Tema.SUPERFICIE : Tema.FILA_ALTERNA));
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
            label.setFont(Tema.TEXTO_NORMAL);
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
            label.setFont(Tema.TEXTO_NORMAL);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Albergue> l, Albergue a, int i, boolean s, boolean f) {
            if (a == null) {
                label.setText("— Sin albergues —");
                label.setIcon(null);
            } else {
                label.setIcon(new PuntoIcono(Tema.color(a.getEstadoSemaforo())));
                label.setText(a.getId() + "  ·  " + a.getNombre() + "  ·  " + a.getCuposDisponibles()
                        + (a.getCuposDisponibles() == 1 ? " cupo libre" : " cupos libres"));
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
            label.setFont(Tema.TEXTO_NORMAL);
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
                label.setText(d.getId() + "  ·  " + d.getNombreCompleto() + "  ·  prioridad "
                        + d.getNivelPrioridad().getEtiqueta().toLowerCase());
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
            label.setFont(Tema.TEXTO_NORMAL);
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
                String vence = r.getFechaVencimiento() == null ? "no vence"
                        : "vence " + r.getFechaVencimiento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                label.setText(r.getNombre() + "  ·  " + Formatos.cantidad(r.getCantidadDisponible(), r.getUnidad())
                        + "  ·  " + vence);
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
            label.setFont(Tema.TEXTO_NORMAL);
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
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x + 2, y + 2, 8, 8));
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
