package com.shelter.ui.componentes;

import com.shelter.model.TipoRecurso;
import com.shelter.ui.tema.Tema;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Iconos vectoriales dibujados con Graphics2D (sin imágenes externas): se ven nítidos a cualquier
 * tamaño y toman el color del tema.
 */
public final class Iconos {

    private Iconos() {
    }

    /** Figuras disponibles. */
    public enum Figura { GOTA, VIVERES, CRUZ, CASA, BURBUJAS, TABLERO, PERSONAS, CAJA, RELOJ, ESCUDO, CAMION, SALIDA, LUPA, MAS, GUARDAR, ABRIR }

    public static Figura figura(TipoRecurso tipo) {
        return switch (tipo) {
            case AGUA -> Figura.GOTA;
            case ALIMENTO -> Figura.VIVERES;
            case MEDICINA -> Figura.CRUZ;
            case ABRIGO -> Figura.CASA;
            case HIGIENE -> Figura.BURBUJAS;
        };
    }

    /** Icono del tipo de recurso con su color de paleta. */
    public static Icon recurso(TipoRecurso tipo, int tamano) {
        return new Vectorial(figura(tipo), tamano, Tema.paleta(tipo).principal(), Tema.paleta(tipo).secundario());
    }

    public static Icon de(Figura figura, int tamano, Color color) {
        return new Vectorial(figura, tamano, color, Tema.mezclar(color, Color.WHITE, 0.5));
    }

    /** Dibuja la figura dentro del cuadrado (x, y, s, s). */
    public static void dibujar(Graphics2D g2, Figura f, double x, double y, double s, Color c1, Color c2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        float trazo = (float) Math.max(1.4, s / 11);
        g2.setStroke(new BasicStroke(trazo, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (f) {
            case GOTA -> {
                Path2D p = new Path2D.Double();
                p.moveTo(x + s * 0.5, y + s * 0.06);
                p.curveTo(x + s * 0.5, y + s * 0.06, x + s * 0.86, y + s * 0.5, x + s * 0.86, y + s * 0.64);
                p.curveTo(x + s * 0.86, y + s * 0.84, x + s * 0.7, y + s * 0.96, x + s * 0.5, y + s * 0.96);
                p.curveTo(x + s * 0.3, y + s * 0.96, x + s * 0.14, y + s * 0.84, x + s * 0.14, y + s * 0.64);
                p.curveTo(x + s * 0.14, y + s * 0.5, x + s * 0.5, y + s * 0.06, x + s * 0.5, y + s * 0.06);
                p.closePath();
                g2.setPaint(new java.awt.GradientPaint((float) x, (float) y, c2, (float) x, (float) (y + s), c1));
                g2.fill(p);
                g2.setColor(Tema.alfa(Color.WHITE, 150));
                g2.draw(new java.awt.geom.Arc2D.Double(x + s * 0.28, y + s * 0.5, s * 0.3, s * 0.3, 180, 80, java.awt.geom.Arc2D.OPEN));
            }
            case VIVERES -> {
                // Cuenco con alimento (verde) y vapor (naranja)
                Path2D cuenco = new Path2D.Double();
                cuenco.moveTo(x + s * 0.08, y + s * 0.5);
                cuenco.lineTo(x + s * 0.92, y + s * 0.5);
                cuenco.curveTo(x + s * 0.92, y + s * 0.78, x + s * 0.72, y + s * 0.92, x + s * 0.5, y + s * 0.92);
                cuenco.curveTo(x + s * 0.28, y + s * 0.92, x + s * 0.08, y + s * 0.78, x + s * 0.08, y + s * 0.5);
                cuenco.closePath();
                g2.setColor(c1);
                g2.fill(cuenco);
                g2.setColor(c2);
                for (int i = 0; i < 3; i++) {
                    double bx = x + s * (0.3 + i * 0.2);
                    Path2D vapor = new Path2D.Double();
                    vapor.moveTo(bx, y + s * 0.42);
                    vapor.curveTo(bx - s * 0.08, y + s * 0.32, bx + s * 0.08, y + s * 0.22, bx, y + s * 0.1);
                    g2.draw(vapor);
                }
            }
            case CRUZ -> {
                g2.setColor(c2);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.06, y + s * 0.06, s * 0.88, s * 0.88, s * 0.3, s * 0.3));
                g2.setColor(c1);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.39, y + s * 0.2, s * 0.22, s * 0.6, s * 0.08, s * 0.08));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.2, y + s * 0.39, s * 0.6, s * 0.22, s * 0.08, s * 0.08));
            }
            case CASA -> {
                Path2D techo = new Path2D.Double();
                techo.moveTo(x + s * 0.04, y + s * 0.5);
                techo.lineTo(x + s * 0.5, y + s * 0.1);
                techo.lineTo(x + s * 0.96, y + s * 0.5);
                techo.closePath();
                g2.setColor(c1);
                g2.fill(techo);
                g2.setColor(c2);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.18, y + s * 0.48, s * 0.64, s * 0.44, s * 0.08, s * 0.08));
                g2.setColor(c1);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.42, y + s * 0.62, s * 0.16, s * 0.3, s * 0.05, s * 0.05));
            }
            case BURBUJAS -> {
                g2.setColor(c1);
                g2.fill(new Ellipse2D.Double(x + s * 0.06, y + s * 0.38, s * 0.54, s * 0.54));
                g2.setColor(c2);
                g2.fill(new Ellipse2D.Double(x + s * 0.54, y + s * 0.16, s * 0.36, s * 0.36));
                g2.fill(new Ellipse2D.Double(x + s * 0.62, y + s * 0.62, s * 0.24, s * 0.24));
                g2.setColor(Tema.alfa(Color.WHITE, 170));
                g2.fill(new Ellipse2D.Double(x + s * 0.18, y + s * 0.48, s * 0.12, s * 0.12));
            }
            case TABLERO -> {
                g2.setColor(c1);
                double m = s * 0.42;
                g2.fill(new RoundRectangle2D.Double(x + s * 0.06, y + s * 0.06, m, m * 1.2, 4, 4));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.52, y + s * 0.06, m, m * 0.7, 4, 4));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.06, y + s * 0.62, m, m * 0.75, 4, 4));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.52, y + s * 0.42, m, m * 1.24, 4, 4));
            }
            case PERSONAS -> {
                g2.setColor(c1);
                g2.fill(new Ellipse2D.Double(x + s * 0.2, y + s * 0.1, s * 0.3, s * 0.3));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.06, y + s * 0.48, s * 0.58, s * 0.42, s * 0.3, s * 0.3));
                g2.setColor(c2);
                g2.fill(new Ellipse2D.Double(x + s * 0.58, y + s * 0.2, s * 0.24, s * 0.24));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.54, y + s * 0.52, s * 0.4, s * 0.34, s * 0.24, s * 0.24));
            }
            case CAJA -> {
                g2.setColor(c1);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.08, y + s * 0.3, s * 0.84, s * 0.62, 5, 5));
                g2.setColor(c2);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.04, y + s * 0.14, s * 0.92, s * 0.22, 5, 5));
                g2.setColor(Tema.FONDO);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.36, y + s * 0.46, s * 0.28, s * 0.08, 3, 3));
            }
            case RELOJ -> {
                g2.setColor(c1);
                g2.draw(new Ellipse2D.Double(x + s * 0.1, y + s * 0.1, s * 0.8, s * 0.8));
                g2.setColor(c2);
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.5, y + s * 0.5, x + s * 0.5, y + s * 0.26));
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.5, y + s * 0.5, x + s * 0.68, y + s * 0.6));
            }
            case ESCUDO -> {
                Path2D p = new Path2D.Double();
                p.moveTo(x + s * 0.5, y + s * 0.04);
                p.lineTo(x + s * 0.9, y + s * 0.2);
                p.curveTo(x + s * 0.9, y + s * 0.6, x + s * 0.72, y + s * 0.84, x + s * 0.5, y + s * 0.96);
                p.curveTo(x + s * 0.28, y + s * 0.84, x + s * 0.1, y + s * 0.6, x + s * 0.1, y + s * 0.2);
                p.closePath();
                g2.setPaint(new java.awt.GradientPaint((float) x, (float) y, c2, (float) x, (float) (y + s), c1));
                g2.fill(p);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.43, y + s * 0.26, s * 0.14, s * 0.48, 3, 3));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.26, y + s * 0.43, s * 0.48, s * 0.14, 3, 3));
            }
            case CAMION -> {
                g2.setColor(c1);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.04, y + s * 0.26, s * 0.56, s * 0.44, 4, 4));
                g2.setColor(c2);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.6, y + s * 0.4, s * 0.34, s * 0.3, 4, 4));
                g2.setColor(c1);
                g2.fill(new Ellipse2D.Double(x + s * 0.14, y + s * 0.66, s * 0.2, s * 0.2));
                g2.fill(new Ellipse2D.Double(x + s * 0.66, y + s * 0.66, s * 0.2, s * 0.2));
            }
            case SALIDA -> {
                g2.setColor(c1);
                g2.draw(new RoundRectangle2D.Double(x + s * 0.1, y + s * 0.1, s * 0.46, s * 0.8, 4, 4));
                g2.setColor(c2);
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.4, y + s * 0.5, x + s * 0.92, y + s * 0.5));
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.74, y + s * 0.32, x + s * 0.92, y + s * 0.5));
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.74, y + s * 0.68, x + s * 0.92, y + s * 0.5));
            }
            case LUPA -> {
                g2.setColor(c1);
                g2.draw(new Ellipse2D.Double(x + s * 0.1, y + s * 0.1, s * 0.54, s * 0.54));
                g2.setColor(c2);
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.58, y + s * 0.58, x + s * 0.9, y + s * 0.9));
            }
            case MAS -> {
                g2.setColor(c1);
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.5, y + s * 0.15, x + s * 0.5, y + s * 0.85));
                g2.draw(new java.awt.geom.Line2D.Double(x + s * 0.15, y + s * 0.5, x + s * 0.85, y + s * 0.5));
            }
            case GUARDAR -> {
                g2.setColor(c1);
                g2.draw(new RoundRectangle2D.Double(x + s * 0.12, y + s * 0.12, s * 0.76, s * 0.76, 4, 4));
                g2.setColor(c2);
                g2.fill(new RoundRectangle2D.Double(x + s * 0.3, y + s * 0.12, s * 0.4, s * 0.24, 2, 2));
                g2.fill(new RoundRectangle2D.Double(x + s * 0.26, y + s * 0.56, s * 0.48, s * 0.24, 2, 2));
            }
            case ABRIR -> {
                g2.setColor(c1);
                Path2D carpeta = new Path2D.Double();
                carpeta.moveTo(x + s * 0.08, y + s * 0.84);
                carpeta.lineTo(x + s * 0.08, y + s * 0.2);
                carpeta.lineTo(x + s * 0.38, y + s * 0.2);
                carpeta.lineTo(x + s * 0.48, y + s * 0.3);
                carpeta.lineTo(x + s * 0.92, y + s * 0.3);
                carpeta.lineTo(x + s * 0.92, y + s * 0.84);
                carpeta.closePath();
                g2.fill(carpeta);
            }
        }
    }

    private record Vectorial(Figura figura, int tamano, Color c1, Color c2) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            dibujar(g2, figura, x, y, tamano, c1, c2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return tamano;
        }

        @Override
        public int getIconHeight() {
            return tamano;
        }
    }
}
