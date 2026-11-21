package com.shelter.ui.componentes;

import com.shelter.model.TipoRecurso;
import com.shelter.ui.tema.Tema;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Mosaico de un tipo de recurso: icono, nombre, stock vigente y una línea de estado,
 * todo con la paleta propia del recurso. Si el recurso es crítico y falta, el borde se vuelve rojo.
 */
public class TarjetaRecurso extends JComponent {

    private static final long serialVersionUID = 1L;

    private final TipoRecurso tipo;
    private String cifra = "—";
    private String detalle = "";
    private boolean enAlerta;

    public TarjetaRecurso(TipoRecurso tipo) {
        this.tipo = tipo;
        setOpaque(false);
    }

    public void setDatos(String cifra, String detalle, boolean enAlerta) {
        this.cifra = cifra;
        this.detalle = detalle;
        this.enAlerta = enAlerta;
        setToolTipText(Tema.paleta(tipo).nombre() + ": " + cifra + " · " + detalle);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(170, 86);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(120, 80);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Tema.PaletaRecurso p = Tema.paleta(tipo);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        RoundRectangle2D forma = new RoundRectangle2D.Double(0, 0, w, h, 16, 16);

        g2.setPaint(new GradientPaint(0, 0, Tema.mezclar(Tema.SUPERFICIE_ALTA, p.principal(), 0.16), w, h, Tema.SUPERFICIE));
        g2.fill(forma);
        // Franja lateral con los dos colores de la paleta
        g2.setClip(forma);
        g2.setPaint(new GradientPaint(0, 0, p.secundario(), 0, h, p.principal()));
        g2.fillRect(0, 0, 5, h);
        g2.setClip(null);
        g2.setStroke(new BasicStroke(enAlerta ? 1.8f : 1f));
        g2.setColor(enAlerta ? Tema.ROJO : Tema.alfa(p.principal(), 90));
        g2.draw(forma);

        // Icono dentro de un círculo con halo
        int s = 38;
        int ix = 16;
        int iy = (h - s) / 2;
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(ix + s / 2.0, iy + s / 2.0), s * 0.75f,
                new float[]{0f, 1f}, new Color[]{Tema.alfa(p.principal(), 90), Tema.alfa(p.principal(), 0)}));
        g2.fill(new Ellipse2D.Double(ix - 8, iy - 8, s + 16, s + 16));
        g2.setColor(Tema.alfa(p.principal(), 50));
        g2.fill(new Ellipse2D.Double(ix, iy, s, s));
        Iconos.dibujar(g2, Iconos.figura(tipo), ix + 8, iy + 8, s - 16, p.principal(), p.secundario());

        // Textos
        int tx = ix + s + 12;
        int anchoTexto = w - tx - 8;
        g2.setFont(Tema.fuente(Font.BOLD, 11));
        g2.setColor(Tema.mezclar(p.principal(), Color.WHITE, 0.4));
        g2.drawString(recortar(g2, p.nombre().toUpperCase(), anchoTexto), tx, h / 2 - 14);
        if (tipo.esCritico()) {
            // Marca de recurso crítico: pequeño rombo en la esquina superior derecha
            g2.setColor(enAlerta ? Tema.ROJO : Tema.alfa(p.secundario(), 170));
            java.awt.geom.Path2D rombo = new java.awt.geom.Path2D.Double();
            rombo.moveTo(w - 14, 8);
            rombo.lineTo(w - 9, 13);
            rombo.lineTo(w - 14, 18);
            rombo.lineTo(w - 19, 13);
            rombo.closePath();
            g2.fill(rombo);
        }
        g2.setFont(Tema.fuente(Font.BOLD, 18));
        g2.setColor(Tema.TEXTO);
        g2.drawString(recortar(g2, cifra, anchoTexto), tx, h / 2 + 6);
        g2.setFont(Tema.TEXTO_PEQUENO);
        g2.setColor(enAlerta ? Tema.mezclar(Tema.ROJO, Color.WHITE, 0.3) : Tema.TEXTO_SUAVE);
        g2.drawString(recortar(g2, detalle, anchoTexto), tx, h / 2 + 24);
        g2.dispose();
    }

    private static String recortar(Graphics2D g2, String texto, int ancho) {
        FontMetrics fm = g2.getFontMetrics();
        if (texto == null || fm.stringWidth(texto) <= ancho) {
            return texto == null ? "" : texto;
        }
        String t = texto;
        while (t.length() > 1 && fm.stringWidth(t + "…") > ancho) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }
}
