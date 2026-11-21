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
        this(texto, estilo, estilo == Estilo.PELIGRO ? Tema.ROJO_BOTON : Tema.ACENTO_OSCURO);
    }

    private static boolean esClaro(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) > 150;
    }

    public BotonModerno(String texto, Estilo estilo, Color color) {
        super(texto);
        this.estilo = estilo;
        this.color = color;
        setFont(Tema.TEXTO_NEGRITA);
        // Texto oscuro sobre fondos claros (la acción principal) y blanco sobre oscuros (peligro)
        setForeground(estilo == Estilo.SECUNDARIO ? Tema.TEXTO : (esClaro(color) ? Tema.TINTA_ACCION : Color.WHITE));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // El foco de teclado se dibuja a mano (ver paintComponent), así que hay que repintar al cambiar
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
            Color fondo = encima && activo ? Tema.alfa(Color.WHITE, presionado ? 22 : 12) : Tema.alfa(Color.WHITE, 4);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arco, arco);
            g2.setColor(encima && activo ? Tema.TEXTO_SUAVE : Tema.BORDE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, arco, arco);
        } else {
            // Relleno plano (sin degradado): la jerarquía la da el contraste, no el brillo
            Color base = activo ? color : Tema.BORDE;
            if (presionado) {
                base = Tema.mezclar(base, Color.BLACK, 0.18);
            } else if (encima && activo) {
                base = esClaro(base) ? Tema.mezclar(base, Color.WHITE, 0.5) : Tema.mezclar(base, Color.WHITE, 0.12);
            }
            g2.setColor(base);
            g2.fillRoundRect(0, 0, w - 1, h - 1, arco, arco);
        }

        // Anillo de foco visible para quien navega con teclado (separado del botón por un hueco)
        if (isFocusOwner() && activo) {
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.setColor(estilo == Estilo.SECUNDARIO ? Tema.TEXTO : (esClaro(color) ? Tema.TINTA_ACCION : Color.WHITE));
            g2.drawRoundRect(3, 3, w - 7, h - 7, arco - 4, arco - 4);
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
