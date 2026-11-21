package com.shelter.ui.componentes;

import com.shelter.model.Alerta;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;

/**
 * Una alerta que dice qué pasa y ofrece cómo resolverlo.
 *
 * Antes la alerta era un callejón sin salida ("Sin stock vigente del recurso crítico ALIMENTO")
 * y el encargado tenía que ir a otra sección y volver a elegir el albergue. Ahora cada fila trae
 * su acción ("Registrar alimento", "Derivar personas"), que abre la sección correcta con el
 * albergue ya elegido.
 */
public class FilaAlerta extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * @param alerta        la alerta a mostrar
     * @param textoAccion   texto del botón, o null si la alerta no tiene una acción directa
     * @param accion        lo que hace el botón
     */
    public FilaAlerta(Alerta alerta, String textoAccion, Runnable accion) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE),
                BorderFactory.createEmptyBorder(10, 0, 12, 0)));

        Renderizadores.Chip chip = new Renderizadores.Chip()
                .configurar(alerta.gravedad().getEtiqueta() + " · " + Formatos.etiquetaAlerta(alerta.codigo()),
                        Tema.color(alerta.gravedad()), true);
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setOpaque(false);
        arriba.add(chip, BorderLayout.WEST);
        add(arriba, BorderLayout.NORTH);

        add(textoAjustable(alerta.mensaje()), BorderLayout.CENTER);

        if (textoAccion != null && accion != null) {
            BotonModerno boton = new BotonModerno(textoAccion + "  →", BotonModerno.Estilo.SECUNDARIO);
            boton.setToolTipText(textoAccion + " en este albergue");
            boton.addActionListener(e -> accion.run());
            JPanel abajo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            abajo.setOpaque(false);
            abajo.add(boton);
            add(abajo, BorderLayout.SOUTH);
        }
        getAccessibleContext().setAccessibleName(alerta.gravedad().getEtiqueta() + ": " + alerta.mensaje());
    }

    /** Texto de lectura que se ajusta al ancho disponible, sin barra horizontal. */
    public static JTextArea textoAjustable(String texto) {
        JTextArea area = new JTextArea(texto);
        // Nimbus pinta el fondo del área de texto aunque sea transparente (queda una franja);
        // la interfaz básica sí respeta setOpaque(false).
        area.setUI(new javax.swing.plaf.basic.BasicTextAreaUI());
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(Tema.TEXTO_NORMAL);
        area.setForeground(Tema.TEXTO);
        return area;
    }

    /** Columna de filas que ocupa el ancho visible del scroll (para que el texto se ajuste). */
    public static class Pila extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        public Pila() {
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int orientacion, int direccion) {
            return r.height - 32;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
