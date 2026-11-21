package com.shelter.ui.vistas;

import com.shelter.model.NivelSemaforo;
import com.shelter.service.AlbergueService.ResumenRed;
import com.shelter.ui.componentes.BarraNivel;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Barra superior del centro de comando: identidad, indicadores de toda la red y reloj.
 */
public class Encabezado extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_RELOJ =
            DateTimeFormatter.ofPattern("EEE dd MMM · HH:mm:ss", Locale.forLanguageTag("es-PE"));

    private final JLabel lblAlbergues = cifra();
    private final JLabel lblPersonas = cifra();
    private final BarraNivel barraRed = new BarraNivel();
    private final Renderizadores.Chip chipVerdes = new Renderizadores.Chip();
    private final Renderizadores.Chip chipAmarillos = new Renderizadores.Chip();
    private final Renderizadores.Chip chipRojos = new Renderizadores.Chip();
    private final Renderizadores.Chip chipEstadoRed = new Renderizadores.Chip();
    private final JLabel lblReloj = new JLabel();
    private NivelSemaforo estadoRed = NivelSemaforo.VERDE;

    public Encabezado() {
        super(new BorderLayout(24, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        // Identidad
        JLabel titulo = new JLabel("SHELTER 4 CASUALTIES");
        titulo.setFont(Tema.fuente(Font.BOLD, 19));
        titulo.setForeground(Tema.TEXTO);
        titulo.setIcon(Iconos.de(Iconos.Figura.ESCUDO, 34, Tema.ACENTO));
        titulo.setIconTextGap(12);
        JLabel subtitulo = new JLabel("Centro de comando de albergues · Normas Sphere");
        subtitulo.setFont(Tema.TEXTO_PEQUENO);
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        subtitulo.setBorder(BorderFactory.createEmptyBorder(0, 46, 0, 0));
        JPanel identidad = new JPanel();
        identidad.setOpaque(false);
        identidad.setLayout(new BoxLayout(identidad, BoxLayout.Y_AXIS));
        identidad.add(titulo);
        identidad.add(subtitulo);
        add(identidad, BorderLayout.WEST);

        // Indicadores
        barraRed.setPreferredSize(new Dimension(150, 16));
        JPanel indicadores = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        indicadores.setOpaque(false);
        indicadores.add(indicador("ALBERGUES", lblAlbergues));
        indicadores.add(indicador("PERSONAS ALOJADAS", lblPersonas));
        indicadores.add(indicador("OCUPACIÓN DE LA RED", barraRed));
        JPanel estados = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        estados.setOpaque(false);
        estados.add(chipVerdes);
        estados.add(chipAmarillos);
        estados.add(chipRojos);
        indicadores.add(indicador("ALBERGUES POR ESTADO", estados));
        add(indicadores, BorderLayout.CENTER);

        // Estado global + reloj
        lblReloj.setFont(Tema.mono(Font.BOLD, 13));
        lblReloj.setForeground(Tema.TEXTO_SUAVE);
        lblReloj.setHorizontalAlignment(JLabel.RIGHT);
        chipEstadoRed.setFont(Tema.fuente(Font.BOLD, 12));
        chipEstadoRed.setAlineacion(javax.swing.SwingConstants.RIGHT);
        JPanel derecha = new JPanel(new BorderLayout(0, 4));
        derecha.setOpaque(false);
        derecha.add(chipEstadoRed, BorderLayout.NORTH);
        derecha.add(lblReloj, BorderLayout.SOUTH);
        derecha.setPreferredSize(new Dimension(230, 54));
        add(derecha, BorderLayout.EAST);

        Timer reloj = new Timer(1000, e -> lblReloj.setText(LocalDateTime.now().format(FORMATO_RELOJ).toUpperCase()));
        reloj.setInitialDelay(0);
        reloj.start();
    }

    private static JLabel cifra() {
        JLabel l = new JLabel("0");
        l.setFont(Tema.CIFRA);
        l.setForeground(Tema.TEXTO);
        return l;
    }

    private static JPanel indicador(String titulo, java.awt.Component valor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(Tema.fuente(Font.BOLD, 10));
        t.setForeground(Tema.TEXTO_TENUE);
        p.add(t, BorderLayout.NORTH);
        JPanel envoltorio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        envoltorio.setOpaque(false);
        envoltorio.add(valor);
        p.add(envoltorio, BorderLayout.CENTER);
        return p;
    }

    public void actualizar(ResumenRed r) {
        lblAlbergues.setText(String.valueOf(r.albergues()));
        lblPersonas.setText(r.ocupacion() + " / " + r.capacidad());
        barraRed.setPorcentaje(r.porcentajeOcupacion(), Math.round(r.porcentajeOcupacion()) + "%");
        chipVerdes.configurar(String.valueOf(r.verdes()), Tema.VERDE, true);
        chipAmarillos.configurar(String.valueOf(r.amarillos()), Tema.AMARILLO, true);
        chipRojos.configurar(String.valueOf(r.rojos()), Tema.ROJO, true);
        estadoRed = r.peorEstado();
        chipEstadoRed.configurar("RED EN " + estadoRed.getEtiqueta(), Tema.color(estadoRed), true);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(0, 0, new Color(0x101A2C), getWidth(), 0, new Color(0x0B1220)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        // Línea inferior teñida con el estado global de la red
        g2.setPaint(new GradientPaint(0, 0, Tema.alfa(Tema.color(estadoRed), 200), getWidth(), 0, Tema.alfa(Tema.color(estadoRed), 0)));
        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
        g2.dispose();
    }
}
