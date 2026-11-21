package com.shelter.ui.vistas;

import com.shelter.model.NivelSemaforo;
import com.shelter.service.AlbergueService.ResumenRed;
import com.shelter.ui.componentes.BarraNivel;
import com.shelter.ui.componentes.Formatos;
import com.shelter.ui.componentes.Iconos;
import com.shelter.ui.componentes.Renderizadores;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Barra superior: identidad y el estado de toda la red en UNA línea legible.
 *
 * Antes era una fila de cifras gigantes con rótulos diminutos encima (la plantilla de "métricas
 * de héroe") y un reloj con segundos que se movía sin decir nada. Ahora cada dato se lee como
 * frase al mismo tamaño ("4 de 107 personas"), y el reloj solo marca horas y minutos.
 */
public class Encabezado extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATO_RELOJ =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM · HH:mm", Locale.forLanguageTag("es-PE"));

    private final JLabel lblResumen = new JLabel();
    private final BarraNivel barraRed = new BarraNivel();
    private final Renderizadores.Chip chipVerdes = new Renderizadores.Chip();
    private final Renderizadores.Chip chipAmarillos = new Renderizadores.Chip();
    private final Renderizadores.Chip chipRojos = new Renderizadores.Chip();
    private final JLabel lblReloj = new JLabel();
    private NivelSemaforo estadoRed = NivelSemaforo.VERDE;

    public Encabezado() {
        super(new BorderLayout(24, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        // Identidad
        JLabel titulo = new JLabel("Shelter 4 Casualties");
        titulo.setFont(Tema.TITULO);
        titulo.setForeground(Tema.TEXTO);
        titulo.setIcon(Iconos.de(Iconos.Figura.ESCUDO, 30, Tema.ACENTO));
        titulo.setIconTextGap(12);
        add(titulo, BorderLayout.WEST);

        // Estado de la red, leído como una frase
        lblResumen.setFont(Tema.TEXTO_NORMAL);
        lblResumen.setForeground(Tema.TEXTO);
        barraRed.setPreferredSize(new Dimension(120, 14));
        barraRed.setMostrarTexto(false);
        barraRed.getAccessibleContext().setAccessibleName("Ocupación de la red");
        JPanel red = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        red.setOpaque(false);
        red.add(lblResumen);
        red.add(barraRed);
        red.add(chipRojos);
        red.add(chipAmarillos);
        red.add(chipVerdes);
        add(red, BorderLayout.CENTER);

        // Fecha y hora (sin segundos: un reloj que cambia cada segundo es movimiento sin información)
        lblReloj.setFont(Tema.TEXTO_NORMAL);
        lblReloj.setForeground(Tema.TEXTO_SUAVE);
        add(lblReloj, BorderLayout.EAST);
        Timer reloj = new Timer(15_000, e -> actualizarReloj());
        reloj.setInitialDelay(0);
        reloj.start();
    }

    private void actualizarReloj() {
        String texto = LocalDateTime.now().format(FORMATO_RELOJ);
        lblReloj.setText(Character.toUpperCase(texto.charAt(0)) + texto.substring(1));
    }

    public void actualizar(ResumenRed r) {
        lblResumen.setText("<html>" + r.albergues() + (r.albergues() == 1 ? " albergue" : " albergues")
                + " &nbsp;·&nbsp; <b>" + r.ocupacion() + " de " + r.capacidad() + "</b> personas alojadas"
                + " &nbsp;·&nbsp; ocupación de la red <b>" + Formatos.porcentaje(r.porcentajeOcupacion()) + "</b></html>");
        barraRed.setPorcentaje(r.porcentajeOcupacion(), null);
        // RNF-01: cada número lleva el nombre de su estado; el color no es la única señal
        // Pastilla solo cuando hay albergues que requieren atención; el resto es texto
        if (r.rojos() > 0) {
            chipRojos.configurar(r.rojos() + " en rojo", Tema.ROJO, true);
        } else {
            chipRojos.texto("0 en rojo", Tema.ROJO);
        }
        if (r.amarillos() > 0) {
            chipAmarillos.configurar(r.amarillos() + " en amarillo", Tema.AMARILLO, true);
        } else {
            chipAmarillos.texto("0 en amarillo", Tema.AMARILLO);
        }
        chipVerdes.texto(r.verdes() + " en verde", Tema.VERDE);
        estadoRed = r.peorEstado();
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Tema.FONDO_LATERAL);
        g2.fillRect(0, 0, getWidth(), getHeight());
        // Línea inferior del color del peor estado de la red: el aviso global, sin texto extra
        g2.setColor(Tema.color(estadoRed));
        g2.fillRect(0, getHeight() - 3, getWidth(), 3);
        g2.dispose();
    }
}
