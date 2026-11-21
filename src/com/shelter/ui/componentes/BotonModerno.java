package com.shelter.ui.componentes;

import com.shelter.ui.tema.Tema;

import javax.swing.Icon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Botón plano con esquinas redondeadas, degradado y estado "hover" animado por color.
 * Estilo PRIMARIO (relleno de color) o SECUNDARIO (contorno).
 */
public class BotonModerno extends JButton {

    private static final long serialVersionUID = 1L;

    public enum Estilo { PRIMARIO, SECUNDARIO, PELIGRO }

    private final Estilo estilo;
    private final Color color;
    private boolean encima;
    private boolean presionado;

    public BotonModerno(String texto, Estilo estilo) {
        this(texto, estilo, estilo == Estilo.PELIGRO ? Tema.ROJO : Tema.ACENTO_OSCURO);
    }

    public BotonModerno(String texto, Estilo estilo, Color color) {
        super(texto);
        this.estilo = estilo;
        this.color = color;
        setFont(Tema.TEXTO_NEGRITA);
        setForeground(estilo == Estilo.SECUNDARIO ? Tema.TEXTO : Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                encima = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = false;
                presionado = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                presionado = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                presionado = false;
                repaint();
            }
        });
    }

    public BotonModerno(String texto) {
        this(texto, Estilo.PRIMARIO);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Icon icono = getIcon();
        int anchoIcono = icono == null ? 0 : icono.getIconWidth() + 8;
        return new Dimension(fm.stringWidth(getText()) + anchoIcono + 32, 36);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int arco = 12;
        boolean activo = isEnabled();

        if (estilo == Estilo.SECUNDARIO) {
            Color fondo = encima && activo ? Tema.alfa(Tema.ACENTO, presionado ? 50 : 30) : Tema.alfa(Color.WHITE, 6);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arco, arco);
            g2.setColor(encima && activo ? Tema.ACENTO : Tema.BORDE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arco, arco);
        } else {
            Color base = activo ? color : Tema.BORDE;
            if (presionado) {
                base = Tema.mezclar(base, Color.BLACK, 0.2);
            } else if (encima && activo) {
                base = Tema.mezclar(base, Color.WHITE, 0.15);
            }
            g2.setPaint(new GradientPaint(0, 0, Tema.mezclar(base, Color.WHITE, 0.12), 0, h, base));
            g2.fillRoundRect(0, 0, w - 1, h - 1, arco, arco);
            if (encima && activo) {
                g2.setColor(Tema.alfa(color, 90));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arco, arco);
            }
        }

        // Icono + texto centrados
        FontMetrics fm = g2.getFontMetrics(getFont());
        Icon icono = getIcon();
        int anchoIcono = icono == null ? 0 : icono.getIconWidth() + 8;
        int anchoTotal = anchoIcono + fm.stringWidth(getText());
        int x = (w - anchoTotal) / 2;
        if (icono != null) {
            icono.paintIcon(this, g2, x, (h - icono.getIconHeight()) / 2);
        }
        g2.setFont(getFont());
        g2.setColor(activo ? getForeground() : Tema.TEXTO_TENUE);
        g2.drawString(getText(), x + anchoIcono, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
