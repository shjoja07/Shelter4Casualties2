package com.shelter.ui;

import com.shelter.controller.ShelterController;
import com.shelter.ui.componentes.Formularios;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.tema.Tema;
import com.shelter.ui.vistas.Contexto;
import com.shelter.ui.vistas.Encabezado;
import com.shelter.ui.vistas.PanelDamnificados;
import com.shelter.ui.vistas.PanelHistorial;
import com.shelter.ui.vistas.PanelRecursos;
import com.shelter.ui.vistas.PanelTablero;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana principal (la "V" de MVC). Solo arma la estructura: encabezado, navegación lateral y
 * las cuatro vistas. Toda la lógica vive en los servicios y se usa a través de ShelterController.
 *
 * Sincronización: la ventana registra UN oyente en el controlador; ante cualquier cambio de datos
 * se refrescan todas las vistas y el encabezado desde el mismo estado, siempre en el EDT.
 */
public class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient ShelterController controller = new ShelterController();
    private final CardLayout cartas = new CardLayout();
    private final JPanel contenido = new JPanel(cartas);
    private final List<BotonNavegacion> botonesNavegacion = new ArrayList<>();

    /** Albergue en foco compartido por las cuatro secciones, y la navegación entre ellas. */
    private final transient Contexto contexto = new Contexto();

    private final Encabezado encabezado = new Encabezado();
    private final PanelTablero tablero = new PanelTablero(controller, contexto);
    private final PanelDamnificados damnificados = new PanelDamnificados(controller, contexto);
    private final PanelRecursos recursos = new PanelRecursos(controller, contexto);
    private final PanelHistorial historial = new PanelHistorial(controller);

    public VentanaPrincipal() {
        super("Shelter 4 Casualties · Centro de Comando de Albergues");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        // Tamaño cómodo, pero nunca mayor que el área útil de la pantalla (sin la barra de tareas)
        java.awt.Rectangle util = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1180, util.width), Math.min(700, util.height)));
        setSize(Math.min(1440, util.width), Math.min(900, util.height));
        setLocationRelativeTo(null);
        if (util.width < 1440 || util.height < 900) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        setIconImage(crearIconoVentana());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(encabezado, BorderLayout.NORTH);
        raiz.add(construirNavegacion(), BorderLayout.WEST);

        contenido.setOpaque(false);
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(tablero, "TABLERO");
        contenido.add(damnificados, "DAMNIFICADOS");
        contenido.add(recursos, "RECURSOS");
        contenido.add(historial, "HISTORIAL");
        raiz.add(contenido, BorderLayout.CENTER);
        setContentPane(raiz);

        // Navegación y acciones que resuelven alertas (desde el Tablero hacia otra sección)
        contexto.setNavegador(s -> seleccionar(s.ordinal()));
        contexto.setAcciones(
                tipo -> {
                    seleccionar(Contexto.Seccion.RECURSOS.ordinal());
                    recursos.prepararLote(tipo);
                },
                () -> {
                    seleccionar(Contexto.Seccion.DAMNIFICADOS.ordinal());
                    damnificados.prepararDerivacion();
                });
        instalarAtajos(raiz);

        controller.agregarOyente(() -> {
            if (SwingUtilities.isEventDispatchThread()) {
                refrescarTodo();
            } else {
                SwingUtilities.invokeLater(this::refrescarTodo);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrar();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                String aviso = controller.iniciar();
                if (aviso != null) {
                    Formularios.aviso(VentanaPrincipal.this, aviso);
                }
            }
        });
        seleccionar(0);
    }

    /** Ctrl+1…4 abre cada sección desde cualquier lugar de la ventana. */
    private void instalarAtajos(JPanel raiz) {
        for (Contexto.Seccion s : Contexto.Seccion.values()) {
            int n = s.ordinal() + 1;
            javax.swing.KeyStroke tecla = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0 + n,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK);
            raiz.getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(tecla, "seccion" + n);
            raiz.getActionMap().put("seccion" + n, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    seleccionar(s.ordinal());
                }
            });
        }
    }

    private void refrescarTodo() {
        encabezado.actualizar(controller.resumenRed());
        tablero.refrescar();
        damnificados.refrescar();
        recursos.refrescar();
        historial.refrescar();
    }

    /**
     * Guarda al cerrar. CORRECCIÓN: antes un error al guardar se ignoraba en silencio y se perdían
     * los cambios; ahora se informa y el usuario decide si cierra igual.
     */
    private void cerrar() {
        try {
            controller.guardar(ShelterController.RUTA_DATOS);
        } catch (IOException ex) {
            if (!Formularios.confirmar(this, "No se pudo guardar",
                    "No se pudieron guardar los datos (" + ex.getMessage() + ").\n¿Cerrar de todas formas y perder los cambios?")) {
                return;
            }
        }
        dispose();
        System.exit(0);
    }

    // =========================================================
    // NAVEGACIÓN LATERAL
    // =========================================================

    private JComponent construirNavegacion() {
        JPanel nav = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Tema.FONDO_LATERAL);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Tema.BORDE_SUAVE);
                g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            }
        };
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(18, 12, 18, 12));
        nav.setPreferredSize(new Dimension(220, 100));

        agregarBoton(nav, "Tablero", "Qué hacer y la red", Iconos.Figura.TABLERO, "TABLERO");
        agregarBoton(nav, "Damnificados", "Ingreso y traslados", Iconos.Figura.PERSONAS, "DAMNIFICADOS");
        agregarBoton(nav, "Recursos", "Inventario y reparto", Iconos.Figura.CAJA, "RECURSOS");
        agregarBoton(nav, "Historial", "Entregas y datos", Iconos.Figura.RELOJ, "HISTORIAL");
        nav.add(Box.createVerticalGlue());

        // La leyenda de colores se quitó: cada mosaico, combo y tabla ya nombra su recurso.
        JLabel atajos = new JLabel("<html>Ctrl+1 … Ctrl+4<br>cambia de sección</html>");
        atajos.setFont(Tema.TEXTO_PEQUENO);
        atajos.setForeground(Tema.TEXTO_TENUE);
        atajos.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        atajos.setAlignmentX(LEFT_ALIGNMENT);
        nav.add(atajos);
        return nav;
    }

    private void agregarBoton(JPanel nav, String titulo, String subtitulo, Iconos.Figura icono, String carta) {
        int indice = botonesNavegacion.size();
        BotonNavegacion b = new BotonNavegacion(titulo, subtitulo, icono, carta);
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                b.requestFocusInWindow();
                seleccionar(indice);
            }
        });
        // Teclado: Tab llega al botón, Enter o Espacio abre la sección
        b.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER || e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    seleccionar(indice);
                }
            }
        });
        b.setToolTipText(subtitulo + " (Ctrl+" + (indice + 1) + ")");
        botonesNavegacion.add(b);
        nav.add(b);
        nav.add(Box.createVerticalStrut(6));
    }

    private void seleccionar(int indice) {
        boolean focoEnNavegacion = false;
        for (int i = 0; i < botonesNavegacion.size(); i++) {
            botonesNavegacion.get(i).setActivo(i == indice);
            focoEnNavegacion |= botonesNavegacion.get(i).isFocusOwner();
        }
        if (focoEnNavegacion) {
            // El anillo de foco acompaña a la sección elegida (nunca dos botones resaltados)
            botonesNavegacion.get(indice).requestFocusInWindow();
        }
        cartas.show(contenido, botonesNavegacion.get(indice).carta);
        // Cada sección se alinea con el albergue en foco (el que se estaba mirando en otra)
        switch (indice) {
            case 0 -> tablero.alMostrar();
            case 1 -> damnificados.alMostrar();
            case 2 -> recursos.alMostrar();
            default -> { }
        }
    }

    /** Botón de la barra lateral con indicador de sección activa y efecto hover. */
    private static final class BotonNavegacion extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String titulo;
        private final String subtitulo;
        private final Iconos.Figura icono;
        private final String carta;
        private boolean activo;
        private boolean encima;

        BotonNavegacion(String titulo, String subtitulo, Iconos.Figura icono, String carta) {
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.icono = icono;
            this.carta = carta;
            setAlignmentX(LEFT_ALIGNMENT);
            setFocusable(true);
            setToolTipText(subtitulo);
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    repaint();
                }
            });
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
            setPreferredSize(new Dimension(196, 54));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    encima = false;
                    repaint();
                }
            });
        }

        void setActivo(boolean activo) {
            this.activo = activo;
            repaint();
        }

        /** Para lectores de pantalla: se anuncia como botón con su nombre. */
        @Override
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public javax.accessibility.AccessibleRole getAccessibleRole() {
                        return javax.accessibility.AccessibleRole.PUSH_BUTTON;
                    }
                };
                accessibleContext.setAccessibleName(titulo + ": " + subtitulo);
            }
            return accessibleContext;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (activo) {
                // Sección actual: tinte del acento con borde completo
                g2.setColor(Tema.alfa(Tema.ACENTO, 34));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.setColor(Tema.alfa(Tema.ACENTO, 120));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            } else if (encima) {
                g2.setColor(Tema.alfa(Color.WHITE, 10));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            }
            if (isFocusOwner()) {
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.setColor(Tema.ACENTO);
                g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);
            }
            Color colorIcono = activo ? Tema.ACENTO : Tema.TEXTO_SUAVE;
            Iconos.dibujar(g2, icono, 16, (h - 20) / 2.0, 20, colorIcono, Tema.mezclar(colorIcono, Color.WHITE, 0.4));
            g2.setFont(Tema.TEXTO_NEGRITA);
            g2.setColor(activo ? Color.WHITE : Tema.TEXTO);
            g2.drawString(titulo, 50, h / 2 - 2);
            g2.setFont(Tema.TEXTO_PEQUENO);
            g2.setColor(Tema.TEXTO_TENUE);
            g2.drawString(subtitulo, 50, h / 2 + 14);
            g2.dispose();
        }
    }

    private static java.awt.Image crearIconoVentana() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(64, 64, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Iconos.dibujar(g2, Iconos.Figura.ESCUDO, 2, 2, 60, Tema.ACENTO_OSCURO, Tema.ACENTO);
        g2.dispose();
        return img;
    }

    // =========================================================
    // ARRANQUE
    // =========================================================

    /** Punto de entrada de la interfaz gráfica. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tema.instalar();
            new VentanaPrincipal().setVisible(true);
        });
    }
}
