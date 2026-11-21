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
 * Panel con esquinas redondeadas, borde fino y un título opcional en tipo de frase.
 * Es el contenedor base de cada sección.
 */
public class Tarjeta extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int RADIO = 18;

    private final JPanel cuerpo = new JPanel(new BorderLayout());

    public Tarjeta(String titulo) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        cuerpo.setOpaque(false);
        if (titulo != null) {
            // Título en tipo de frase, sin mayúsculas sostenidas ni punto de color: se lee como un
            // encabezado, no como un rótulo decorativo sobre la sección.
            JLabel lbl = new JLabel(titulo);
            lbl.setFont(Tema.SUBTITULO);
            lbl.setForeground(Tema.TEXTO);
            add(lbl, BorderLayout.NORTH);
        }
        add(cuerpo, BorderLayout.CENTER);
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, RADIO, RADIO);
        // Sombra con desplazamiento hacia abajo y difuminado suave (capas que se agrandan y aclaran)
        for (int i = 4; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 14));
            g2.fill(new RoundRectangle2D.Double(1 - i * 0.5, 2 + i, getWidth() - 2 + i, getHeight() - 2, RADIO + i, RADIO + i));
        }
        // Superficie plana (sin degradado ni "línea de vidrio")
        g2.setColor(Tema.SUPERFICIE);
        g2.fill(forma);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(Tema.BORDE_SUAVE);
        g2.draw(forma);
        g2.dispose();
    }
}
