package com.shelter.ui.componentes;

import com.shelter.model.TipoRecurso;
import com.shelter.ui.tema.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Graphics;

/**
 * Columna de un recurso dentro de la fila de inventario: nombre, cantidad y detalle.
 *
 * Antes cada recurso era una tarjeta con borde de color, icono en círculo y un rombo de "crítico";
 * cinco tarjetas idénticas competían por la atención. Ahora es texto alineado en una sola fila,
 * y el único color aparece cuando el recurso falta (rojo), que es lo que hay que ver.
 */
public class TarjetaRecurso extends JPanel {

    private static final long serialVersionUID = 1L;

    private final TipoRecurso tipo;
    private final JLabel lblNombre = new JLabel();
    private final JLabel lblCifra = new JLabel("—");
    private final JLabel lblDetalle = new JLabel(" ");
    private boolean separador;

    public TarjetaRecurso(TipoRecurso tipo) {
        this.tipo = tipo;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(2, 16, 2, 12));
        lblNombre.setText(Tema.paleta(tipo).nombre());
        lblNombre.setFont(Tema.TEXTO_NORMAL);
        lblNombre.setForeground(Tema.TEXTO_SUAVE);
        lblCifra.setFont(Tema.TITULO);
        lblCifra.setForeground(Tema.TEXTO);
        lblDetalle.setFont(Tema.TEXTO_PEQUENO);
        lblDetalle.setForeground(Tema.TEXTO_SUAVE);
        for (JComponent c : new JComponent[]{lblNombre, lblCifra, lblDetalle}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        add(lblNombre);
        add(Box.createVerticalStrut(4));
        add(lblCifra);
        add(Box.createVerticalStrut(2));
        add(lblDetalle);
    }

    /** Dibuja una línea divisoria a la izquierda (para todas las columnas menos la primera). */
    public void setSeparador(boolean separador) {
        this.separador = separador;
    }

    public void setDatos(String cifra, String detalle, boolean enAlerta) {
        lblCifra.setText(cifra);
        lblDetalle.setText(detalle == null || detalle.isEmpty() ? " " : detalle);
        // Solo el estado lleva color: un recurso que falta se lee en rojo
        lblCifra.setForeground(enAlerta ? Tema.ROJO : Tema.TEXTO);
        lblDetalle.setForeground(enAlerta ? Tema.mezclar(Tema.ROJO, java.awt.Color.WHITE, 0.2) : Tema.TEXTO_SUAVE);
        String descripcion = Tema.paleta(tipo).nombre() + ": " + cifra + ", " + detalle + (enAlerta ? ", en alerta" : "");
        setToolTipText(descripcion);
        getAccessibleContext().setAccessibleName(descripcion);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (separador) {
            g.setColor(Tema.BORDE_SUAVE);
            g.drawLine(0, 4, 0, getHeight() - 4);
        }
    }
}
