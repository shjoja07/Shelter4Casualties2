package com.shelter.ui.componentes;

import com.shelter.service.AlbergueService;
import com.shelter.ui.tema.Tema;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Barra de progreso redondeada. Por defecto se colorea con los mismos umbrales del semáforo de
 * ocupación (verde < 70 %, amarillo < 90 %, rojo desde 90 %), o con un color fijo si se indica.
 */
public class BarraNivel extends JComponent {

    private static final long serialVersionUID = 1L;

    private double porcentaje;
    private Color colorFijo;
    private String texto;
    private boolean mostrarTexto = true;

    public BarraNivel() {
        setOpaque(false);
        setFont(Tema.fuente(java.awt.Font.BOLD, 11));
    }

    public void setPorcentaje(double porcentaje, String texto) {
        this.porcentaje = Double.isFinite(porcentaje) ? Math.max(0, porcentaje) : 0;
        this.texto = texto;
        repaint();
    }

    public void setColorFijo(Color color) {
        this.colorFijo = color;
        repaint();
    }

    public void setMostrarTexto(boolean mostrar) {
        this.mostrarTexto = mostrar;
    }

    /** Color que corresponde a un porcentaje de ocupación (umbrales del servicio). */
    public static Color colorOcupacion(double pct) {
        if (pct >= AlbergueService.UMBRAL_ROJO) {
            return Tema.ROJO;
        }
        if (pct >= AlbergueService.UMBRAL_AMARILLO) {
            return Tema.AMARILLO;
        }
        return Tema.VERDE;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 18);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int alto = Math.min(getHeight(), 14);
        int y = (getHeight() - alto) / 2;
        int ancho = getWidth();
        Color color = colorFijo != null ? colorFijo : colorOcupacion(porcentaje);

        g2.setColor(new Color(0x0A1220));
        g2.fill(new RoundRectangle2D.Double(0, y, ancho, alto, alto, alto));
        double relleno = Math.min(1, porcentaje / 100.0) * ancho;
        if (relleno > 0) {
            g2.setPaint(new GradientPaint(0, 0, Tema.mezclar(color, Color.BLACK, 0.25), (float) relleno, 0, color));
            g2.fill(new RoundRectangle2D.Double(0, y, Math.max(relleno, alto), alto, alto, alto));
            g2.setColor(new Color(255, 255, 255, 40));
            g2.fill(new RoundRectangle2D.Double(2, y + 1, Math.max(relleno - 4, 0), alto / 2.5, alto, alto));
        }
        if (mostrarTexto && texto != null) {
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ancho - fm.stringWidth(texto)) / 2;
            int ty = y + (alto - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(texto, tx + 1, ty + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(texto, tx, ty);
        }
        g2.dispose();
    }
}
