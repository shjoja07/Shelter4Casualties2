package com.shelter.ui.componentes;

import com.shelter.model.NivelSemaforo;
import com.shelter.ui.tema.Tema;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Semáforo dibujado íntegramente con Graphics2D: carcasa metálica con sombra, viseras sobre cada
 * luz, luces con degradado radial y resplandor (glow) cuando están encendidas.
 *
 * Fluidez: el cambio de estado no es instantáneo. Un javax.swing.Timer (que corre en el hilo de
 * eventos de Swing, así que nunca bloquea ni compite con la interfaz) interpola la intensidad de
 * cada luz hacia su objetivo. En ROJO, además, la luz "late" suavemente para llamar la atención.
 * El timer se detiene solo cuando la animación termina y cuando el componente deja de mostrarse.
 */
public class SemaforoComponente extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int FPS = 60;
    /** Fracción de la distancia al objetivo que se recorre en cada cuadro (suavizado exponencial). */
    private static final double SUAVIZADO = 0.12;

    // Orden de dibujo de arriba hacia abajo
    private static final NivelSemaforo[] LUCES = {NivelSemaforo.ROJO, NivelSemaforo.AMARILLO, NivelSemaforo.VERDE};

    private final double[] intensidad = new double[3];   // 0 = apagada ... 1 = encendida
    private NivelSemaforo nivel = NivelSemaforo.VERDE;
    private double fasePulso;
    private final Timer animacion;

    public SemaforoComponente() {
        setOpaque(false);
        intensidad[indice(nivel)] = 1;
        animacion = new Timer(1000 / FPS, e -> avanzar());
        animacion.setCoalesce(true);
        setToolTipText("Estado semáforo: " + nivel.getEtiqueta());
    }

    /** Cambia el estado. Se puede llamar desde cualquier hilo: el cambio se aplica en el EDT. */
    public void setNivel(NivelSemaforo nuevo) {
        if (nuevo == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setNivel(nuevo));
            return;
        }
        if (nuevo != nivel) {
            nivel = nuevo;
            setToolTipText("Estado semáforo: " + nivel.getEtiqueta());
            getAccessibleContext().setAccessibleName("Semáforo: " + nivel.getEtiqueta());
            firePropertyChange("nivel", null, nuevo);
        }
        arrancar();
    }

    public NivelSemaforo getNivel() {
        return nivel;
    }

    private static int indice(NivelSemaforo n) {
        for (int i = 0; i < LUCES.length; i++) {
            if (LUCES[i] == n) {
                return i;
            }
        }
        return 0;
    }

    private void arrancar() {
        if (isShowing() && !animacion.isRunning()) {
            animacion.start();
        }
        repaint();
    }

    private void avanzar() {
        boolean enMovimiento = false;
        for (int i = 0; i < 3; i++) {
            double objetivo = (LUCES[i] == nivel) ? 1 : 0;
            double diferencia = objetivo - intensidad[i];
            if (Math.abs(diferencia) > 0.003) {
                intensidad[i] += diferencia * SUAVIZADO;
                enMovimiento = true;
            } else {
                intensidad[i] = objetivo;
            }
        }
        if (nivel == NivelSemaforo.ROJO) {
            fasePulso = (fasePulso + 2 * Math.PI / (FPS * 1.6)) % (2 * Math.PI); // latido de 1.6 s
            enMovimiento = true;
        } else {
            fasePulso = 0;
        }
        repaint();
        if (!enMovimiento) {
            animacion.stop();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        arrancar();
    }

    @Override
    public void removeNotify() {
        animacion.stop(); // evita que un timer huérfano siga vivo (fuga de recursos)
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        return isPreferredSizeSet() ? super.getPreferredSize() : new Dimension(150, 360);
    }

    @Override
    public Dimension getMinimumSize() {
        return isMinimumSizeSet() ? super.getMinimumSize() : new Dimension(50, 120);
    }

    /** Para lectores de pantalla el semáforo es una imagen con su estado como nombre. */
    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                private static final long serialVersionUID = 1L;

                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.ICON;
                }
            };
            accessibleContext.setAccessibleName("Semáforo: " + nivel.getEtiqueta());
        }
        return accessibleContext;
    }

    // =========================================================
    // DIBUJO
    // =========================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // La carcasa mantiene proporción 1 : 2.5 y se centra en el espacio disponible.
        // Márgenes proporcionales: el mismo componente sirve grande o como indicador compacto.
        double margen = Math.max(4, Math.min(18, getWidth() * 0.1));
        double soporte = Math.min(26, getHeight() * 0.1);
        double altoDisp = getHeight() - 2 * margen - soporte;
        double anchoDisp = getWidth() - 2 * margen;
        double ancho = Math.min(anchoDisp, altoDisp / 2.5);
        double alto = ancho * 2.5;
        double x = (getWidth() - ancho) / 2;
        double y = margen + Math.max(0, (altoDisp - alto) / 2);
        double radio = ancho * 0.28;

        dibujarSoporte(g2, x, y, ancho, alto);
        dibujarSombra(g2, x, y, ancho, alto, radio);
        dibujarCarcasa(g2, x, y, ancho, alto, radio);

        double diametro = ancho * 0.62;
        double espacio = alto / 3;
        double pulso = (nivel == NivelSemaforo.ROJO) ? 0.82 + 0.18 * (0.5 + 0.5 * Math.cos(fasePulso)) : 1;
        for (int i = 0; i < 3; i++) {
            double cx = x + ancho / 2;
            double cy = y + espacio * i + espacio / 2;
            double brillo = intensidad[i] * (LUCES[i] == NivelSemaforo.ROJO ? pulso : 1);
            dibujarLuz(g2, cx, cy, diametro, Tema.color(LUCES[i]), brillo);
            dibujarVisera(g2, cx, cy, diametro);
        }
        g2.dispose();
    }

    private void dibujarSoporte(Graphics2D g2, double x, double y, double ancho, double alto) {
        double cx = x + ancho / 2;
        double base = y + alto;
        double k = ancho / 100.0; // el soporte escala con la carcasa
        double poste = Math.max(4, 12 * k);
        double largo = Math.max(6, 22 * k);
        g2.setPaint(new GradientPaint((float) (cx - poste / 2), 0, new Color(0x2B3647), (float) (cx + poste / 2), 0, new Color(0x161D29)));
        g2.fill(new java.awt.geom.Rectangle2D.Double(cx - poste / 2, base - 4, poste, largo));
        g2.setPaint(new GradientPaint(0, (float) (base + largo - 6), new Color(0x2B3647), 0, (float) (base + largo + 3), new Color(0x10151E)));
        g2.fill(new RoundRectangle2D.Double(cx - ancho * 0.32, base + largo - 6, ancho * 0.64, Math.max(4, 9 * k), 8, 8));
    }

    private void dibujarSombra(Graphics2D g2, double x, double y, double ancho, double alto, double radio) {
        // Sombra difusa: varias capas translúcidas, cada una un poco más grande
        for (int i = 10; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 7));
            g2.fill(new RoundRectangle2D.Double(x - i + 4, y - i + 8, ancho + 2 * i, alto + 2 * i, radio + i * 2, radio + i * 2));
        }
    }

    private void dibujarCarcasa(Graphics2D g2, double x, double y, double ancho, double alto, double radio) {
        RoundRectangle2D carcasa = new RoundRectangle2D.Double(x, y, ancho, alto, radio, radio);
        // Cuerpo metálico: degradado horizontal que simula un cilindro
        g2.setPaint(new java.awt.LinearGradientPaint(
                new Point2D.Double(x, 0), new Point2D.Double(x + ancho, 0),
                new float[]{0f, 0.18f, 0.55f, 1f},
                new Color[]{new Color(0x1A2230), new Color(0x3A4658), new Color(0x232C3B), new Color(0x0F141C)}));
        g2.fill(carcasa);
        // Placa frontal más oscura
        double m = ancho * 0.07;
        RoundRectangle2D placa = new RoundRectangle2D.Double(x + m, y + m, ancho - 2 * m, alto - 2 * m, radio * 0.8, radio * 0.8);
        g2.setPaint(new GradientPaint(0, (float) y, new Color(0x121924), 0, (float) (y + alto), new Color(0x0A0E15)));
        g2.fill(placa);
        // Bisel: brillo arriba-izquierda y borde exterior
        g2.setStroke(new BasicStroke(1.4f));
        g2.setPaint(new GradientPaint((float) x, (float) y, new Color(255, 255, 255, 70),
                (float) (x + ancho), (float) (y + alto), new Color(255, 255, 255, 0)));
        g2.draw(carcasa);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.draw(placa);
        // Tornillos decorativos
        g2.setColor(new Color(0x4A5568));
        double t = ancho * 0.05;
        g2.fill(new Ellipse2D.Double(x + ancho / 2 - t / 2, y + m * 0.25, t, t));
        g2.fill(new Ellipse2D.Double(x + ancho / 2 - t / 2, y + alto - m * 0.25 - t, t, t));
    }

    private void dibujarLuz(Graphics2D g2, double cx, double cy, double d, Color color, double brillo) {
        double r = d / 2;

        // 1. Resplandor exterior (glow): se extiende más allá de la lente y se desvanece
        if (brillo > 0.01) {
            float rGlow = (float) (r * 2.3);
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), rGlow,
                    new float[]{0f, 0.35f, 0.6f, 1f},
                    new Color[]{Tema.alfa(color, (int) (170 * brillo)), Tema.alfa(color, (int) (90 * brillo)),
                            Tema.alfa(color, (int) (28 * brillo)), Tema.alfa(color, 0)}));
            g2.fill(new Ellipse2D.Double(cx - rGlow, cy - rGlow, rGlow * 2, rGlow * 2));
        }

        // 2. Aro metálico alrededor de la lente
        g2.setPaint(new GradientPaint((float) cx, (float) (cy - r - 4), new Color(0x2E3A4C),
                (float) cx, (float) (cy + r + 4), new Color(0x07090D)));
        g2.fill(new Ellipse2D.Double(cx - r - 4, cy - r - 4, d + 8, d + 8));

        // 3. Lente: mezcla entre la versión apagada (opaca, casi negra) y la encendida (brillante)
        Color apagadoCentro = Tema.mezclar(color, Color.BLACK, 0.72);
        Color apagadoBorde = Tema.mezclar(color, Color.BLACK, 0.9);
        Color encendidoCentro = Tema.mezclar(color, Color.WHITE, 0.55);
        Color encendidoBorde = Tema.mezclar(color, Color.BLACK, 0.25);
        Color centro = Tema.mezclar(apagadoCentro, encendidoCentro, brillo);
        Color medio = Tema.mezclar(Tema.mezclar(color, Color.BLACK, 0.8), color, brillo);
        Color borde = Tema.mezclar(apagadoBorde, encendidoBorde, brillo);
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Double(cx, cy), (float) r,
                new Point2D.Double(cx - r * 0.25, cy - r * 0.3),
                new float[]{0f, 0.55f, 1f}, new Color[]{centro, medio, borde},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, d, d));

        // 4. Textura de lente Fresnel: anillos concéntricos muy sutiles
        g2.setStroke(new BasicStroke(1f));
        for (int i = 1; i <= 3; i++) {
            double ri = r * i / 4.0;
            g2.setColor(new Color(0, 0, 0, (int) (22 + 18 * (1 - brillo))));
            g2.draw(new Ellipse2D.Double(cx - ri, cy - ri, ri * 2, ri * 2));
        }

        // 5. Reflejo especular (más visible si la luz está apagada: el vidrio refleja el ambiente)
        g2.setPaint(new GradientPaint((float) cx, (float) (cy - r), new Color(255, 255, 255, (int) (110 - 40 * brillo)),
                (float) cx, (float) cy, new Color(255, 255, 255, 0)));
        g2.fill(new Ellipse2D.Double(cx - r * 0.62, cy - r * 0.9, r * 1.24, r * 0.8));
    }

    /** Visera curva sobre cada luz (como en los semáforos reales). */
    private void dibujarVisera(Graphics2D g2, double cx, double cy, double d) {
        double r = d / 2 + 6;
        Path2D visera = new Path2D.Double();
        visera.moveTo(cx - r, cy - r * 0.15);
        visera.curveTo(cx - r, cy - r * 1.35, cx + r, cy - r * 1.35, cx + r, cy - r * 0.15);
        visera.lineTo(cx + r - 3, cy - r * 0.2);
        visera.curveTo(cx + r * 0.85, cy - r * 1.12, cx - r * 0.85, cy - r * 1.12, cx - r + 3, cy - r * 0.2);
        visera.closePath();
        g2.setPaint(new GradientPaint((float) cx, (float) (cy - r * 1.2), new Color(0x3A4658), (float) cx, (float) cy, new Color(0x0D1118)));
        g2.fill(visera);
    }
}
