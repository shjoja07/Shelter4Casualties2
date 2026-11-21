package com.shelter.ui.componentes;

import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Panel con esquinas redondeadas, borde fino y un título opcional con una marca de color.
 * Es la "tarjeta" base de todo el centro de comando.
 */
public class Tarjeta extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int RADIO = 18;

    private Color acento;
    private final JPanel cuerpo = new JPanel(new BorderLayout());

    public Tarjeta(String titulo, Color acento) {
        super(new BorderLayout(0, 10));
        this.acento = acento;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        cuerpo.setOpaque(false);
        if (titulo != null) {
            JLabel lbl = new JLabel(titulo.toUpperCase());
            lbl.setFont(Tema.TITULO_TARJETA);
            lbl.setForeground(Tema.TEXTO_SUAVE);
            lbl.setIcon(new MarcaIcono());
            lbl.setIconTextGap(8);
            add(lbl, BorderLayout.NORTH);
        }
        add(cuerpo, BorderLayout.CENTER);
    }

    public Tarjeta(String titulo) {
        this(titulo, Tema.ACENTO);
    }

    /** Coloca el contenido principal de la tarjeta. */
    public Tarjeta contenido(JComponent c) {
        cuerpo.removeAll();
        cuerpo.add(c, BorderLayout.CENTER);
        return this;
    }

    /** Agrega un componente a la derecha del título (por ejemplo, botones). */
    public void accionesTitulo(JComponent c) {
        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        java.awt.Component titulo = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.NORTH);
        if (titulo != null) {
            remove(titulo);
            norte.add(titulo, BorderLayout.WEST);
        }
        norte.add(c, BorderLayout.EAST);
        add(norte, BorderLayout.NORTH);
    }

    public void setAcento(Color acento) {
        this.acento = acento;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, RADIO, RADIO);
        // Sombra suave alrededor
        for (int i = 3; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 18 * i));
            g2.draw(new RoundRectangle2D.Double(0.5 - i, 0.5 - i + 1, getWidth() - 1.0 + 2 * i, getHeight() - 1.0 + 2 * i, RADIO + i, RADIO + i));
        }
        g2.setPaint(new java.awt.GradientPaint(0, 0, Tema.SUPERFICIE_ALTA, 0, getHeight(), Tema.SUPERFICIE));
        g2.fill(forma);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Tema.BORDE_SUAVE);
        g2.draw(forma);
        // Línea de luz superior (efecto vidrio)
        g2.setColor(new Color(255, 255, 255, 14));
        g2.drawLine(RADIO / 2, 1, getWidth() - RADIO / 2, 1);
        g2.dispose();
    }

    /** Pequeña barra vertical de color junto al título. */
    private final class MarcaIcono implements javax.swing.Icon {
        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(acento);
            g2.fillRoundRect(x, y + 1, 4, getIconHeight() - 2, 4, 4);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 4;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }
}
